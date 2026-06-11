package com.toby.report.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Pedidos (order-service)
    public static final String ORDERS_EXCHANGE = "orders.exchange";
    public static final String QUEUE_ORDERS = "report.orders";
    public static final String ROUTING_ORDER_CREATED = "order.created";

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE);
    }

    @Bean
    public Queue reportOrdersQueue() {
        return new Queue(QUEUE_ORDERS, true);
    }

    @Bean
    public Binding ordersBinding(Queue reportOrdersQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(reportOrdersQueue).to(ordersExchange).with(ROUTING_ORDER_CREATED);
    }

    // Default TypePrecedence=INFERRED: deserializa por el tipo del parametro del listener.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
