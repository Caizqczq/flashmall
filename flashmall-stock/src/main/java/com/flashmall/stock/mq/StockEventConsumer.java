package com.flashmall.stock.mq;

import com.flashmall.common.event.DomainEvent;
import com.flashmall.stock.config.RabbitMQConfig;
import com.flashmall.stock.service.StockOrderEventService;
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
public class StockEventConsumer {

    private final StockOrderEventService stockOrderEventService;

    @RabbitListener(queues = RabbitMQConfig.STOCK_ORDER_EVENT_QUEUE)
    public void consumeOrderEvent(@Payload DomainEvent event,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            stockOrderEventService.processOrderEvent(event);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("库存服务处理订单事件失败: eventId={}, type={}, orderId={}",
                    event.getEventId(), event.getEventType(), event.getOrderId(), e);
            channel.basicReject(deliveryTag, false);
        }
    }
}
