package com.portfolens.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
    @Bean
    DirectExchange portfolioEventsExchange(@Value("${app.messaging.exchange}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }
}
