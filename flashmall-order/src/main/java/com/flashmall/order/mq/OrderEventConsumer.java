package com.flashmall.order.mq;

import com.flashmall.common.event.DomainEvent;
import com.flashmall.order.config.RabbitMQConfig;
import com.flashmall.order.service.OrderEventService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderEventService orderEventService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_STOCK_RESULT_QUEUE)
    public void consumeStockResult(@Payload DomainEvent event,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            orderEventService.processStockResult(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理库存结果事件失败: eventId={}, type={}, orderId={}",
                    event.getEventId(), event.getEventType(), event.getOrderId(), e);
            channel.basicReject(deliveryTag, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CLOSE_HANDLER_QUEUE)
    public void consumeCloseDelay(@Payload DomainEvent event,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            orderEventService.processOrderCloseDelay(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理延迟关单事件失败: eventId={}, orderId={}", event.getEventId(), event.getOrderId(), e);
            channel.basicReject(deliveryTag, false);
        }
    }
}
