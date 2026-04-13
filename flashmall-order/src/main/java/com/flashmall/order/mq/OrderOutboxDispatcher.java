package com.flashmall.order.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashmall.common.event.DomainEvent;
import com.flashmall.common.event.EventType;
import com.flashmall.order.config.RabbitMQConfig;
import com.flashmall.order.entity.OrderOutbox;
import com.flashmall.order.mapper.OrderOutboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxDispatcher {

    private final OrderOutboxMapper orderOutboxMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${order.outbox.batch-size:100}")
    private int batchSize;

    @Value("${order.close.delay-ms:900000}")
    private long closeDelayMs;

    @Scheduled(fixedDelayString = "${order.outbox.dispatch-interval-ms:3000}")
    public void dispatch() {
        List<OrderOutbox> pendingEvents = orderOutboxMapper.selectList(new LambdaQueryWrapper<OrderOutbox>()
                .eq(OrderOutbox::getStatus, 0)
                .le(OrderOutbox::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(OrderOutbox::getId)
                .last("limit " + batchSize));

        for (OrderOutbox outbox : pendingEvents) {
            try {
                DomainEvent event = objectMapper.readValue(outbox.getPayload(), DomainEvent.class);
                publish(event);
                markSent(outbox.getId());
            } catch (Exception e) {
                log.error("投递Outbox事件失败: eventId={}, eventType={}", outbox.getEventId(), outbox.getEventType(), e);
                markFailed(outbox, e.getMessage());
            }
        }
    }

    private void publish(DomainEvent event) {
        switch (event.getEventType()) {
            case EventType.ORDER_CREATED -> rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EVENT_EXCHANGE,
                    RabbitMQConfig.RK_ORDER_CREATED,
                    event
            );
            case EventType.ORDER_CANCELED -> rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EVENT_EXCHANGE,
                    RabbitMQConfig.RK_ORDER_CANCELED,
                    event
            );
            case EventType.ORDER_PAID -> rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EVENT_EXCHANGE,
                    RabbitMQConfig.RK_ORDER_PAID,
                    event
            );
            case EventType.ORDER_CLOSE_DELAY -> rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DELAY_EXCHANGE,
                    RabbitMQConfig.RK_ORDER_CLOSE_DELAY,
                    event,
                    RabbitMQConfig.persistentWithDelay(closeDelayMs)
            );
            default -> throw new IllegalArgumentException("不支持的订单事件类型: " + event.getEventType());
        }
    }

    private void markSent(Long id) {
        orderOutboxMapper.update(null, new LambdaUpdateWrapper<OrderOutbox>()
                .eq(OrderOutbox::getId, id)
                .eq(OrderOutbox::getStatus, 0)
                .set(OrderOutbox::getStatus, 1)
                .set(OrderOutbox::getLastError, null));
    }

    private void markFailed(OrderOutbox outbox, String errorMessage) {
        int nextRetryCount = outbox.getRetryCount() + 1;
        int delaySeconds = Math.min(60, 1 << Math.min(nextRetryCount, 6));

        String safeError = errorMessage == null ? "unknown" : errorMessage;
        if (safeError.length() > 500) {
            safeError = safeError.substring(0, 500);
        }

        orderOutboxMapper.update(null, new LambdaUpdateWrapper<OrderOutbox>()
                .eq(OrderOutbox::getId, outbox.getId())
                .eq(OrderOutbox::getStatus, 0)
                .set(OrderOutbox::getRetryCount, nextRetryCount)
                .set(OrderOutbox::getLastError, safeError)
                .set(OrderOutbox::getNextRetryTime, LocalDateTime.now().plusSeconds(delaySeconds)));
    }
}
