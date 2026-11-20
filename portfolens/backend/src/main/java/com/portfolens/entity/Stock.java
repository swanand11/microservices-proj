package com.portfolens.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stocks", indexes = {@Index(name = "idx_stocks_symbol", columnList = "symbol"), @Index(name = "idx_stocks_company", columnList = "company_name"), @Index(name = "idx_stocks_sector", columnList = "sector")})
public class Stock {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 20) private String symbol;
    @Column(nullable = false, length = 180) private String companyName;
    @Column(nullable = false, length = 20) private String exchange;
    @Column(length = 20) private String bseScripCode;
    @Column(length = 40) private String yahooTicker;
    @Column(length = 100) private String sector;
    @Column(length = 100) private String industry;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected Stock() {}
    public Stock(String symbol, String companyName, String bseScripCode, String sector, String industry) { this.id = UUID.randomUUID(); this.symbol = symbol; this.companyName = companyName; this.exchange = "BSE"; this.bseScripCode = bseScripCode; this.sector = sector; this.industry = industry; }
    @PrePersist void create() { createdAt = Instant.now(); }
    public UUID getId() { return id; } public String getSymbol() { return symbol; } public String getCompanyName() { return companyName; } public String getExchange() { return exchange; } public String getBseScripCode() { return bseScripCode; } public String getYahooTicker() { return yahooTicker; } public String getSector() { return sector; } public String getIndustry() { return industry; } public boolean isActive() { return active; } public Instant getCreatedAt() { return createdAt; }
}
