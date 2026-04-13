package com.flashmall.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private RabbitMQConfig() {
    }

    public static final String EVENT_EXCHANGE = "flashmall.event.exchange";
    public static final String DELAY_EXCHANGE = "flashmall.delay.exchange";

    public static final String ORDER_STOCK_RESULT_QUEUE = "order.stock.result.queue";
    public static final String ORDER_CLOSE_DELAY_QUEUE = "order.close.delay.queue";
    public static final String ORDER_CLOSE_HANDLER_QUEUE = "order.close.handler.queue";

    public static final String RK_ORDER_CREATED = "order.created";
    public static final String RK_ORDER_CANCELED = "order.canceled";
    public static final String RK_ORDER_PAID = "order.paid";
    public static final String RK_STOCK_DEDUCTED = "stock.deducted";
    public static final String RK_STOCK_DEDUCT_FAILED = "stock.deduct.failed";

    public static final String RK_ORDER_CLOSE_DELAY = "order.close.delay";
    public static final String RK_ORDER_CLOSE_DELAY_FIRE = "order.close.delay.fire";

    @Bean
    public DirectExchange eventExchange() {
        return new DirectExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderStockResultQueue() {
        return QueueBuilder.durable(ORDER_STOCK_RESULT_QUEUE).build();
    }

    @Bean
    public Queue orderCloseDelayQueue() {
        return QueueBuilder.durable(ORDER_CLOSE_DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", EVENT_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RK_ORDER_CLOSE_DELAY_FIRE)
                .build();
    }

    @Bean
    public Queue orderCloseHandlerQueue() {
        return QueueBuilder.durable(ORDER_CLOSE_HANDLER_QUEUE).build();
    }

    @Bean
    public Binding stockDeductedBinding(Queue orderStockResultQueue, DirectExchange eventExchange) {
        return BindingBuilder.bind(orderStockResultQueue).to(eventExchange).with(RK_STOCK_DEDUCTED);
    }

    @Bean
    public Binding stockDeductFailedBinding(Queue orderStockResultQueue, DirectExchange eventExchange) {
        return BindingBuilder.bind(orderStockResultQueue).to(eventExchange).with(RK_STOCK_DEDUCT_FAILED);
    }

    @Bean
    public Binding orderCloseDelayBinding(Queue orderCloseDelayQueue, DirectExchange delayExchange) {
        return BindingBuilder.bind(orderCloseDelayQueue).to(delayExchange).with(RK_ORDER_CLOSE_DELAY);
    }

    @Bean
    public Binding orderCloseHandlerBinding(Queue orderCloseHandlerQueue, DirectExchange eventExchange) {
        return BindingBuilder.bind(orderCloseHandlerQueue).to(eventExchange).with(RK_ORDER_CLOSE_DELAY_FIRE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    public static MessagePostProcessor persistentWithDelay(long delayMs) {
        return message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setExpiration(String.valueOf(delayMs));
            return message;
        };
    }
}
