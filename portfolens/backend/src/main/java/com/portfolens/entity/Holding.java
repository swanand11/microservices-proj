package com.portfolens.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "holdings", uniqueConstraints = @UniqueConstraint(name = "uk_holding_portfolio_stock", columnNames = {"portfolio_id", "stock_id"}))
public class Holding {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "portfolio_id", nullable = false) private Portfolio portfolio;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "stock_id", nullable = false) private Stock stock;
    @Column(nullable = false, precision = 24, scale = 8) private BigDecimal quantity;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal averageBuyPrice;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected Holding() {}
    public Holding(Portfolio portfolio, Stock stock, BigDecimal quantity, BigDecimal averageBuyPrice) { id = UUID.randomUUID(); this.portfolio = portfolio; this.stock = stock; this.quantity = quantity; this.averageBuyPrice = averageBuyPrice; }
    @PrePersist void create() { var now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void updateTime() { updatedAt = Instant.now(); }
    public UUID getId() { return id; } public Portfolio getPortfolio() { return portfolio; } public Stock getStock() { return stock; } public BigDecimal getQuantity() { return quantity; } public BigDecimal getAverageBuyPrice() { return averageBuyPrice; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
    public void update(BigDecimal quantity, BigDecimal averageBuyPrice) { this.quantity = quantity; this.averageBuyPrice = averageBuyPrice; }
}
