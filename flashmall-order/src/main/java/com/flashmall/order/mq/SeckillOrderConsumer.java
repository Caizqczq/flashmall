package com.flashmall.order.mq;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashmall.order.config.RabbitMQConfig;
import com.flashmall.order.dto.SeckillMessageDTO;
import com.flashmall.order.entity.Order;
import com.flashmall.order.entity.SeckillOrder;
import com.flashmall.order.mapper.OrderMapper;
import com.flashmall.order.mapper.SeckillOrderMapper;
import com.flashmall.order.mapper.StockMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final OrderMapper orderMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final StockMapper stockMapper;
    private final StringRedisTemplate redisTemplate;

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(2, 1);
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String BOUGHT_KEY_PREFIX = "seckill:bought:";

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    public void handleSeckillOrder(@Payload SeckillMessageDTO message,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            createSeckillOrder(message);
            channel.basicAck(deliveryTag, false);
            log.info("秒杀订单处理成功: userId={}, goodsId={}", message.getUserId(), message.getGoodsId());
        } catch (Exception e) {
            log.error("秒杀订单处理失败: userId={}, goodsId={}, error={}",
                    message.getUserId(), message.getGoodsId(), e.getMessage());
            rollbackRedis(message.getUserId(), message.getGoodsId());
            channel.basicReject(deliveryTag, false);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void createSeckillOrder(SeckillMessageDTO message) {
        Long userId = message.getUserId();
        Long goodsId = message.getGoodsId();

        // 1. DB层幂等校验（唯一索引兜底）
        SeckillOrder existing = seckillOrderMapper.selectOne(
                new LambdaQueryWrapper<SeckillOrder>()
                        .eq(SeckillOrder::getUserId, userId)
                        .eq(SeckillOrder::getGoodsId, goodsId)
        );
        if (existing != null) {
            log.warn("重复秒杀（DB层拦截）: userId={}, goodsId={}", userId, goodsId);
            return;
        }

        // 2. 本地扣减库存（同一个事务，乐观锁防超卖）
        int affected = stockMapper.deductStock(goodsId, 1);
        if (affected == 0) {
            throw new RuntimeException("数据库扣减库存失败（库存不足）: goodsId=" + goodsId);
        }

        // 3. 创建主订单
        Order order = new Order();
        order.setOrderNo(SNOWFLAKE.nextIdStr());
        order.setUserId(userId);
        order.setGoodsId(goodsId);
        order.setGoodsName(message.getGoodsName());
        order.setGoodsPrice(message.getSeckillPrice());
        order.setQuantity(1);
        order.setTotalAmount(message.getSeckillPrice());
        order.setStatus(0);
        orderMapper.insert(order);

        // 4. 创建秒杀订单记录
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setUserId(userId);
        seckillOrder.setGoodsId(goodsId);
        seckillOrder.setOrderId(order.getId());
        seckillOrderMapper.insert(seckillOrder);
    }

    private void rollbackRedis(Long userId, Long goodsId) {
        try {
            redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + goodsId);
            redisTemplate.opsForSet().remove(BOUGHT_KEY_PREFIX + goodsId, String.valueOf(userId));
            log.info("Redis库存回滚完成: userId={}, goodsId={}", userId, goodsId);
        } catch (Exception ex) {
            log.error("Redis库存回滚失败: userId={}, goodsId={}", userId, goodsId, ex);
        }
    }
}
