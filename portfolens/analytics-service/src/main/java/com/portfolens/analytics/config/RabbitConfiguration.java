package com.portfolens.analytics.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {
    @Bean DirectExchange portfolioEventsExchange(@Value("${app.rabbitmq.exchange}") String name) { return new DirectExchange(name, true, false); }
    @Bean Queue analyticsTransactionQueue(@Value("${app.rabbitmq.queue}") String name) { return new Queue(name, true); }
    @Bean Binding transactionBinding(Queue analyticsTransactionQueue, DirectExchange portfolioEventsExchange,
            @Value("${app.rabbitmq.routing-key}") String routingKey) {
        return BindingBuilder.bind(analyticsTransactionQueue).to(portfolioEventsExchange).with(routingKey);
    }
}
