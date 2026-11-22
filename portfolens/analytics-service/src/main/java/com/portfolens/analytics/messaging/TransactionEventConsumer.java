package com.portfolens.analytics.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolens.analytics.entity.AnalyticsTransaction;
import com.portfolens.analytics.repository.AnalyticsTransactionRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionEventConsumer {
    private final ObjectMapper objectMapper;
    private final AnalyticsTransactionRepository transactions;

    public TransactionEventConsumer(ObjectMapper objectMapper, AnalyticsTransactionRepository transactions) {
        this.objectMapper = objectMapper; this.transactions = transactions;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    @Transactional
    public void consume(String payload) throws JsonProcessingException {
        PortfolioTransactionRecorded event = objectMapper.readValue(payload, PortfolioTransactionRecorded.class);
        if (event.eventId() == null || event.transactionId() == null || event.portfolioId() == null || event.ownerId() == null
                || event.transactionType() == null || event.quantity() == null || event.price() == null) {
            throw new IllegalArgumentException("Transaction event is missing required fields");
        }
        if (transactions.existsById(event.transactionId())) return;
        transactions.save(new AnalyticsTransaction(event));
    }
}
