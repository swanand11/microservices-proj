package com.portfolens.service;

import com.portfolens.entity.OutboxEvent;
import com.portfolens.repository.OutboxEventRepository;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository events;
    private final RabbitTemplate rabbit;
    private final String exchange;

    public OutboxPublisher(OutboxEventRepository events, RabbitTemplate rabbit,
            @Value("${app.messaging.exchange}") String exchange) {
        this.events = events;
        this.rabbit = rabbit;
        this.exchange = exchange;
        this.rabbit.setMandatory(true);
    }

    @Scheduled(fixedDelayString = "${app.messaging.outbox-poll-ms:1000}")
    public void publishPending() {
        for (OutboxEvent event : events.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                CorrelationData confirmation = new CorrelationData(event.getId().toString());
                rabbit.convertAndSend(exchange, event.getRoutingKey(), event.getPayload(), message -> {
                    message.getMessageProperties().setMessageId(event.getId().toString());
                    message.getMessageProperties().setContentType("application/json");
                    message.getMessageProperties().setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("eventType", event.getEventType());
                    return message;
                }, confirmation);
                var result = confirmation.getFuture().get(5, TimeUnit.SECONDS);
                if (!result.isAck()) throw new IllegalStateException("RabbitMQ negatively acknowledged event");
                if (confirmation.getReturned() != null) throw new IllegalStateException("RabbitMQ returned an unroutable event");
                event.markPublished();
                events.save(event);
            } catch (Exception failure) {
                event.markFailed(failure.getMessage());
                events.save(event);
                log.warn("Could not publish outbox event {} (attempt {})", event.getId(), event.getAttempts(), failure);
            }
        }
    }
}
