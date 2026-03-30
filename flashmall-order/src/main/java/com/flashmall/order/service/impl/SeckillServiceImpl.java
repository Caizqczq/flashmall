package com.flashmall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashmall.common.exception.BizException;
import com.flashmall.common.result.ResultCodeEnum;
import com.flashmall.order.config.RabbitMQConfig;
import com.flashmall.order.dto.SeckillMessageDTO;
import com.flashmall.order.entity.SeckillOrder;
import com.flashmall.order.mapper.SeckillOrderMapper;
import com.flashmall.order.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final SeckillOrderMapper seckillOrderMapper;

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String BOUGHT_KEY_PREFIX = "seckill:bought:";
    private static final String GOODS_INFO_KEY_PREFIX = "seckill:goods:info:";

    private DefaultRedisScript<Long> seckillScript;

    @PostConstruct
    public void init() {
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/seckill_stock.lua")));
        seckillScript.setResultType(Long.class);
    }

    @Override
    public void initSeckillStock(Long goodsId, Integer stockCount, String goodsName, BigDecimal seckillPrice) {
        // 预热秒杀库存到Redis
        redisTemplate.opsForValue().set(STOCK_KEY_PREFIX + goodsId, String.valueOf(stockCount));
        // 清理已购买用户集合（如果重新初始化）
        redisTemplate.delete(BOUGHT_KEY_PREFIX + goodsId);
        // 缓存商品信息供消费者使用
        redisTemplate.opsForHash().put(GOODS_INFO_KEY_PREFIX + goodsId, "goodsName", goodsName);
        redisTemplate.opsForHash().put(GOODS_INFO_KEY_PREFIX + goodsId, "seckillPrice", seckillPrice.toPlainString());
        log.info("秒杀库存初始化完成: goodsId={}, stockCount={}", goodsId, stockCount);
    }

    @Override
    public String doSeckill(Long userId, Long goodsId) {
        // 1. 执行Lua脚本：幂等校验 + 原子扣库存
        Long result = redisTemplate.execute(
                seckillScript,
                List.of(STOCK_KEY_PREFIX + goodsId, BOUGHT_KEY_PREFIX + goodsId),
                String.valueOf(userId)
        );

        if (result == null) {
            throw new BizException(ResultCodeEnum.SYSTEM_ERROR);
        }

        switch (result.intValue()) {
            case 1 -> throw new BizException(ResultCodeEnum.STOCK_NOT_ENOUGH);
            case 2 -> throw new BizException(ResultCodeEnum.SECKILL_REPEAT);
        }

        // 2. 获取商品信息（从Redis缓存中获取，初始化时已写入）
        Map<Object, Object> goodsInfo = redisTemplate.opsForHash().entries(GOODS_INFO_KEY_PREFIX + goodsId);
        String goodsName = (String) goodsInfo.getOrDefault("goodsName", "秒杀商品");
        BigDecimal seckillPrice = new BigDecimal((String) goodsInfo.getOrDefault("seckillPrice", "0"));

        // 3. 发送MQ消息，异步创建订单
        SeckillMessageDTO message = new SeckillMessageDTO(userId, goodsId, goodsName, seckillPrice);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_EXCHANGE,
                RabbitMQConfig.SECKILL_ROUTING_KEY,
                message
        );

        log.info("秒杀消息已发送: userId={}, goodsId={}", userId, goodsId);
        return "秒杀请求已提交，请稍后查询订单结果";
    }

    @Override
    public List<SeckillOrder> listByUserId(Long userId) {
        return seckillOrderMapper.selectList(
                new LambdaQueryWrapper<SeckillOrder>()
                        .eq(SeckillOrder::getUserId, userId)
                        .orderByDesc(SeckillOrder::getCreateTime)
        );
    }

    @Override
    public SeckillOrder getByOrderId(Long orderId) {
        return seckillOrderMapper.selectOne(
                new LambdaQueryWrapper<SeckillOrder>()
                        .eq(SeckillOrder::getOrderId, orderId)
        );
    }
}
