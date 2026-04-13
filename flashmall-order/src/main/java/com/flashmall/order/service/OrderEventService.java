package com.flashmall.order.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.flashmall.common.constant.OrderStatus;
import com.flashmall.common.event.DomainEvent;
import com.flashmall.common.event.EventType;
import com.flashmall.order.entity.Order;
import com.flashmall.order.entity.OrderConsumedEvent;
import com.flashmall.order.mapper.OrderConsumedEventMapper;
import com.flashmall.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventService {

    private static final String CONSUMER_GROUP_STOCK_RESULT = "order-stock-result-consumer";
    private static final String CONSUMER_GROUP_CLOSE_HANDLER = "order-close-handler-consumer";

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String BOUGHT_KEY_PREFIX = "seckill:bought:";

    private final OrderConsumedEventMapper orderConsumedEventMapper;
    private final OrderMapper orderMapper;
    private final OrderOutboxService orderOutboxService;
    private final StringRedisTemplate redisTemplate;

    @Transactional(rollbackFor = Exception.class)
    public void processStockResult(DomainEvent event) {
        if (!tryMarkConsumed(CONSUMER_GROUP_STOCK_RESULT, event.getEventId())) {
            log.info("重复消费库存结果事件，跳过: eventId={}", event.getEventId());
            return;
        }

        if (EventType.STOCK_DEDUCTED.equals(event.getEventType())) {
            handleStockDeducted(event);
            return;
        }
        if (EventType.STOCK_DEDUCT_FAILED.equals(event.getEventType())) {
            handleStockDeductFailed(event);
            return;
        }
        log.warn("未知库存结果事件类型: eventType={}", event.getEventType());
    }

    @Transactional(rollbackFor = Exception.class)
    public void processOrderCloseDelay(DomainEvent event) {
        if (!tryMarkConsumed(CONSUMER_GROUP_CLOSE_HANDLER, event.getEventId())) {
            log.info("重复消费延迟关单事件，跳过: eventId={}", event.getEventId());
            return;
        }

        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, event.getOrderId())
                .eq(Order::getStatus, OrderStatus.WAIT_PAY)
                .set(Order::getStatus, OrderStatus.CANCELED_TIMEOUT));

        if (updated == 0) {
            log.info("订单非待支付状态，忽略延迟关单: orderId={}", event.getOrderId());
            return;
        }

        DomainEvent cancelEvent = new DomainEvent();
        cancelEvent.setEventType(EventType.ORDER_CANCELED);
        cancelEvent.setBizNo(event.getBizNo());
        cancelEvent.setOrderNo(event.getOrderNo());
        cancelEvent.setOrderId(event.getOrderId());
        cancelEvent.setUserId(event.getUserId());
        cancelEvent.setGoodsId(event.getGoodsId());
        cancelEvent.setQuantity(event.getQuantity());
        cancelEvent.setReason("timeout");
        cancelEvent.setOccurredAt(LocalDateTime.now());
        orderOutboxService.saveEvent(cancelEvent);
    }

    private void handleStockDeducted(DomainEvent event) {
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, event.getOrderId())
                .eq(Order::getStatus, OrderStatus.PENDING_STOCK)
                .set(Order::getStatus, OrderStatus.WAIT_PAY));

        if (updated == 0) {
            log.info("订单状态非库存确认中，忽略扣减成功事件: orderId={}", event.getOrderId());
            return;
        }

        DomainEvent closeDelayEvent = new DomainEvent();
        closeDelayEvent.setEventType(EventType.ORDER_CLOSE_DELAY);
        closeDelayEvent.setBizNo(event.getBizNo());
        closeDelayEvent.setOrderNo(event.getOrderNo());
        closeDelayEvent.setOrderId(event.getOrderId());
        closeDelayEvent.setUserId(event.getUserId());
        closeDelayEvent.setGoodsId(event.getGoodsId());
        closeDelayEvent.setQuantity(event.getQuantity());
        closeDelayEvent.setOccurredAt(LocalDateTime.now());
        orderOutboxService.saveEvent(closeDelayEvent);
    }

    private void handleStockDeductFailed(DomainEvent event) {
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, event.getOrderId())
                .eq(Order::getStatus, OrderStatus.PENDING_STOCK)
                .set(Order::getStatus, OrderStatus.CANCELED_NO_STOCK));

        if (updated == 0) {
            log.info("订单状态非库存确认中，忽略扣减失败事件: orderId={}", event.getOrderId());
            return;
        }

        rollbackRedis(event.getUserId(), event.getGoodsId());
    }

    private boolean tryMarkConsumed(String consumerGroup, String eventId) {
        OrderConsumedEvent consumedEvent = new OrderConsumedEvent();
        consumedEvent.setConsumerGroup(consumerGroup);
        consumedEvent.setEventId(eventId);
        try {
            orderConsumedEventMapper.insert(consumedEvent);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    private void rollbackRedis(Long userId, Long goodsId) {
        if (goodsId == null || userId == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + goodsId);
            redisTemplate.opsForSet().remove(BOUGHT_KEY_PREFIX + goodsId, String.valueOf(userId));
        } catch (Exception e) {
            // Redis补偿失败不应回滚订单状态，避免形成“库存确认中”悬挂订单
            log.error("Redis补偿失败: userId={}, goodsId={}", userId, goodsId, e);
        }
    }
}
