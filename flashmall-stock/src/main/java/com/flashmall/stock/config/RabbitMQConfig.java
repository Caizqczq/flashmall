package com.flashmall.stock.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
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
    public static final String STOCK_ORDER_EVENT_QUEUE = "stock.order.event.queue";

    public static final String RK_ORDER_CREATED = "order.created";
    public static final String RK_ORDER_CANCELED = "order.canceled";
    public static final String RK_STOCK_DEDUCTED = "stock.deducted";
    public static final String RK_STOCK_DEDUCT_FAILED = "stock.deduct.failed";

    @Bean
    public DirectExchange eventExchange() {
        return new DirectExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue stockOrderEventQueue() {
        return QueueBuilder.durable(STOCK_ORDER_EVENT_QUEUE).build();
    }

    @Bean
    public Binding orderCreatedBinding(Queue stockOrderEventQueue, DirectExchange eventExchange) {
        return BindingBuilder.bind(stockOrderEventQueue).to(eventExchange).with(RK_ORDER_CREATED);
    }

    @Bean
    public Binding orderCanceledBinding(Queue stockOrderEventQueue, DirectExchange eventExchange) {
        return BindingBuilder.bind(stockOrderEventQueue).to(eventExchange).with(RK_ORDER_CANCELED);
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
}
