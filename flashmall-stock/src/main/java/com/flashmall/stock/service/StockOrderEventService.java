package com.flashmall.stock.service;

import com.flashmall.common.event.DomainEvent;
import com.flashmall.common.event.EventType;
import com.flashmall.stock.entity.StockConsumedEvent;
import com.flashmall.stock.mapper.StockConsumedEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockOrderEventService {

    private static final String CONSUMER_GROUP_ORDER_CREATED = "stock-order-created-consumer";
    private static final String CONSUMER_GROUP_ORDER_CANCELED = "stock-order-canceled-consumer";

    private final StockConsumedEventMapper stockConsumedEventMapper;
    private final StockService stockService;
    private final StockOutboxService stockOutboxService;

    @Transactional(rollbackFor = Exception.class)
    public void processOrderEvent(DomainEvent event) {
        if (EventType.ORDER_CREATED.equals(event.getEventType())) {
            processOrderCreated(event);
            return;
        }

        if (EventType.ORDER_CANCELED.equals(event.getEventType())) {
            processOrderCanceled(event);
            return;
        }

        log.warn("库存服务收到未知订单事件类型: eventType={}", event.getEventType());
    }

    private void processOrderCreated(DomainEvent event) {
        if (!tryMarkConsumed(CONSUMER_GROUP_ORDER_CREATED, event.getEventId())) {
            log.info("重复消费ORDER_CREATED，跳过: eventId={}", event.getEventId());
            return;
        }

        Integer quantity = event.getQuantity() == null ? 1 : event.getQuantity();
        boolean deducted = stockService.deductStock(event.getGoodsId(), quantity);

        DomainEvent resultEvent = new DomainEvent();
        resultEvent.setBizNo(event.getBizNo());
        resultEvent.setOrderNo(event.getOrderNo());
        resultEvent.setOrderId(event.getOrderId());
        resultEvent.setUserId(event.getUserId());
        resultEvent.setGoodsId(event.getGoodsId());
        resultEvent.setQuantity(quantity);
        resultEvent.setOccurredAt(LocalDateTime.now());

        if (deducted) {
            resultEvent.setEventType(EventType.STOCK_DEDUCTED);
        } else {
            resultEvent.setEventType(EventType.STOCK_DEDUCT_FAILED);
            resultEvent.setReason("库存不足");
        }

        stockOutboxService.saveEvent(resultEvent);
    }

    private void processOrderCanceled(DomainEvent event) {
        if (!tryMarkConsumed(CONSUMER_GROUP_ORDER_CANCELED, event.getEventId())) {
            log.info("重复消费ORDER_CANCELED，跳过: eventId={}", event.getEventId());
            return;
        }

        Integer quantity = event.getQuantity() == null ? 1 : event.getQuantity();
        stockService.addStock(event.getGoodsId(), quantity);
    }

    private boolean tryMarkConsumed(String consumerGroup, String eventId) {
        StockConsumedEvent consumedEvent = new StockConsumedEvent();
        consumedEvent.setConsumerGroup(consumerGroup);
        consumedEvent.setEventId(eventId);
        try {
            stockConsumedEventMapper.insert(consumedEvent);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
