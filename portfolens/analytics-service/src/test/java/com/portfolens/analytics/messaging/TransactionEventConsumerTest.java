package com.portfolens.analytics.messaging;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolens.analytics.repository.AnalyticsTransactionRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {
    @Mock AnalyticsTransactionRepository transactions;

    @Test
    void duplicateTransactionEventIsAcknowledgedWithoutWritingAgain() throws Exception {
        UUID transactionId = UUID.randomUUID();
        when(transactions.existsById(transactionId)).thenReturn(true);
        var event = new PortfolioTransactionRecorded(UUID.randomUUID(), transactionId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "RELIANCE", "BUY", java.math.BigDecimal.ONE, java.math.BigDecimal.TEN,
                java.time.Instant.parse("2025-01-01T00:00:00Z"), java.time.Instant.parse("2025-01-01T00:00:01Z"));
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        new TransactionEventConsumer(mapper, transactions).consume(mapper.writeValueAsString(event));
        verify(transactions).existsById(transactionId);
        verify(transactions, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
