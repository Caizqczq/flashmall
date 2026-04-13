package com.flashmall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashmall.common.exception.BizException;
import com.flashmall.common.result.ResultCodeEnum;
import com.flashmall.order.entity.Order;
import com.flashmall.order.entity.SeckillOrder;
import com.flashmall.order.mapper.SeckillOrderMapper;
import com.flashmall.order.service.SeckillOrderTxService;
import com.flashmall.order.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
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
    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillOrderTxService seckillOrderTxService;

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
        redisTemplate.opsForValue().set(STOCK_KEY_PREFIX + goodsId, String.valueOf(stockCount));
        redisTemplate.delete(BOUGHT_KEY_PREFIX + goodsId);
        redisTemplate.opsForHash().put(GOODS_INFO_KEY_PREFIX + goodsId, "goodsName", goodsName);
        redisTemplate.opsForHash().put(GOODS_INFO_KEY_PREFIX + goodsId, "seckillPrice", seckillPrice.toPlainString());
        log.info("秒杀库存初始化完成: goodsId={}, stockCount={}", goodsId, stockCount);
    }

    @Override
    public String doSeckill(Long userId, Long goodsId) {
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
            default -> {
            }
        }

        Map<Object, Object> goodsInfo = redisTemplate.opsForHash().entries(GOODS_INFO_KEY_PREFIX + goodsId);
        if (goodsInfo == null || goodsInfo.isEmpty()) {
            rollbackRedis(userId, goodsId);
            throw new BizException("秒杀商品未初始化，请先预热库存");
        }

        String goodsName = (String) goodsInfo.getOrDefault("goodsName", "秒杀商品");
        BigDecimal seckillPrice = new BigDecimal((String) goodsInfo.getOrDefault("seckillPrice", "0"));

        try {
            Order order = seckillOrderTxService.createPendingOrder(userId, goodsId, goodsName, seckillPrice);
            log.info("秒杀待确认订单创建成功: orderId={}, userId={}, goodsId={}",
                    order.getId(), userId, goodsId);
            return "秒杀请求已提交，订单号: " + order.getOrderNo() + "，请稍后查看状态";
        } catch (DuplicateKeyException e) {
            throw new BizException(ResultCodeEnum.SECKILL_REPEAT);
        } catch (Exception e) {
            rollbackRedis(userId, goodsId);
            log.error("秒杀创建订单失败，已回滚Redis预扣: userId={}, goodsId={}", userId, goodsId, e);
            throw new BizException(ResultCodeEnum.ORDER_CREATE_FAIL);
        }
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

    private void rollbackRedis(Long userId, Long goodsId) {
        redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + goodsId);
        redisTemplate.opsForSet().remove(BOUGHT_KEY_PREFIX + goodsId, String.valueOf(userId));
    }
}
