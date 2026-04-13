package com.flashmall.stock.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashmall.common.event.DomainEvent;
import com.flashmall.common.event.EventType;
import com.flashmall.stock.config.RabbitMQConfig;
import com.flashmall.stock.entity.StockOutbox;
import com.flashmall.stock.mapper.StockOutboxMapper;
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
public class StockOutboxDispatcher {

    private final StockOutboxMapper stockOutboxMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${stock.outbox.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${stock.outbox.dispatch-interval-ms:3000}")
    public void dispatch() {
        List<StockOutbox> pendingEvents = stockOutboxMapper.selectList(new LambdaQueryWrapper<StockOutbox>()
                .eq(StockOutbox::getStatus, 0)
                .le(StockOutbox::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(StockOutbox::getId)
                .last("limit " + batchSize));

        for (StockOutbox outbox : pendingEvents) {
            try {
                DomainEvent event = objectMapper.readValue(outbox.getPayload(), DomainEvent.class);
                publish(event);
                markSent(outbox.getId());
            } catch (Exception e) {
                log.error("投递库存Outbox事件失败: eventId={}, eventType={}", outbox.getEventId(), outbox.getEventType(), e);
                markFailed(outbox, e.getMessage());
            }
        }
    }

    private void publish(DomainEvent event) {
        switch (event.getEventType()) {
            case EventType.STOCK_DEDUCTED -> rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EVENT_EXCHANGE,
                    RabbitMQConfig.RK_STOCK_DEDUCTED,
                    event
            );
            case EventType.STOCK_DEDUCT_FAILED -> rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EVENT_EXCHANGE,
                    RabbitMQConfig.RK_STOCK_DEDUCT_FAILED,
                    event
            );
            default -> throw new IllegalArgumentException("不支持的库存事件类型: " + event.getEventType());
        }
    }

    private void markSent(Long id) {
        stockOutboxMapper.update(null, new LambdaUpdateWrapper<StockOutbox>()
                .eq(StockOutbox::getId, id)
                .eq(StockOutbox::getStatus, 0)
                .set(StockOutbox::getStatus, 1)
                .set(StockOutbox::getLastError, null));
    }

    private void markFailed(StockOutbox outbox, String errorMessage) {
        int nextRetryCount = outbox.getRetryCount() + 1;
        int delaySeconds = Math.min(60, 1 << Math.min(nextRetryCount, 6));

        String safeError = errorMessage == null ? "unknown" : errorMessage;
        if (safeError.length() > 500) {
            safeError = safeError.substring(0, 500);
        }

        stockOutboxMapper.update(null, new LambdaUpdateWrapper<StockOutbox>()
                .eq(StockOutbox::getId, outbox.getId())
                .eq(StockOutbox::getStatus, 0)
                .set(StockOutbox::getRetryCount, nextRetryCount)
                .set(StockOutbox::getLastError, safeError)
                .set(StockOutbox::getNextRetryTime, LocalDateTime.now().plusSeconds(delaySeconds)));
    }
}
