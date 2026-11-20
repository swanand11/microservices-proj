package com.portfolens.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "portfolio_transactions", indexes = @Index(name = "idx_transactions_portfolio_date", columnList = "portfolio_id, transaction_date"))
public class PortfolioTransaction {
    public enum Type { BUY, SELL }
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "portfolio_id", nullable = false) private Portfolio portfolio;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "stock_id", nullable = false) private Stock stock;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 4) private Type transactionType;
    @Column(nullable = false, precision = 24, scale = 8) private BigDecimal quantity;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal price;
    @Column(nullable = false) private Instant transactionDate;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected PortfolioTransaction() {}
    public PortfolioTransaction(Portfolio portfolio, Stock stock, Type type, BigDecimal quantity, BigDecimal price, Instant transactionDate) { id = UUID.randomUUID(); this.portfolio = portfolio; this.stock = stock; this.transactionType = type; this.quantity = quantity; this.price = price; this.transactionDate = transactionDate; }
    @PrePersist void create() { createdAt = Instant.now(); }
    public UUID getId() { return id; } public Portfolio getPortfolio() { return portfolio; } public Stock getStock() { return stock; } public Type getTransactionType() { return transactionType; } public BigDecimal getQuantity() { return quantity; } public BigDecimal getPrice() { return price; } public Instant getTransactionDate() { return transactionDate; } public Instant getCreatedAt() { return createdAt; }
}
