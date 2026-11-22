package com.portfolens.analytics.entity;

import com.portfolens.analytics.messaging.PortfolioTransactionRecorded;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_transaction_events", indexes = @Index(name = "idx_analytics_events_portfolio_date", columnList = "portfolio_id, transaction_date"))
public class AnalyticsTransaction {
    @Id private UUID transactionId;
    @Column(nullable = false, unique = true) private UUID eventId;
    @Column(nullable = false) private UUID portfolioId;
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false) private UUID stockId;
    @Column(nullable = false, length = 20) private String symbol;
    @Column(nullable = false, length = 4) private String transactionType;
    @Column(nullable = false, precision = 24, scale = 8) private BigDecimal quantity;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal price;
    @Column(nullable = false) private Instant transactionDate;
    @Column(nullable = false) private Instant recordedAt;
    protected AnalyticsTransaction() {}

    public AnalyticsTransaction(PortfolioTransactionRecorded event) {
        transactionId = event.transactionId(); eventId = event.eventId(); portfolioId = event.portfolioId();
        ownerId = event.ownerId(); stockId = event.stockId(); symbol = event.symbol();
        transactionType = event.transactionType(); quantity = event.quantity(); price = event.price();
        transactionDate = event.transactionDate(); recordedAt = event.recordedAt();
    }
    public UUID getTransactionId() { return transactionId; }
    public UUID getEventId() { return eventId; }
    public UUID getPortfolioId() { return portfolioId; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getStockId() { return stockId; }
    public String getSymbol() { return symbol; }
    public String getTransactionType() { return transactionType; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public Instant getTransactionDate() { return transactionDate; }
    public Instant getRecordedAt() { return recordedAt; }
}
