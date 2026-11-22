package com.portfolens.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolens.dto.PortfolioTransactionRecorded;
import com.portfolens.entity.OutboxEvent;
import com.portfolens.repository.OutboxEventRepository;
import com.portfolens.repository.PortfolioTransactionRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PortfolioEventBackfill implements ApplicationRunner {
    private final PortfolioTransactionRepository transactions;
    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;
    private final String routingKey;

    public PortfolioEventBackfill(PortfolioTransactionRepository transactions, OutboxEventRepository outbox,
            ObjectMapper objectMapper,
            @Value("${app.messaging.transaction-routing-key}") String routingKey) {
        this.transactions = transactions;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.routingKey = routingKey;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (var transaction : transactions.findAll()) {
            UUID eventId = transaction.getId();
            if (outbox.existsById(eventId)) continue;
            try {
                String payload = objectMapper.writeValueAsString(PortfolioTransactionRecorded.from(transaction, eventId));
                outbox.save(new OutboxEvent(eventId, "PortfolioTransactionRecorded", routingKey, payload));
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Could not backfill a portfolio transaction event", ex);
            }
        }
    }
}
