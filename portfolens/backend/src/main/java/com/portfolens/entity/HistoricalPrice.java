package com.portfolens.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name="historical_prices", uniqueConstraints=@UniqueConstraint(name="uk_historical_price_stock_date", columnNames={"stock_id", "trading_date"}), indexes={@Index(name="idx_historical_price_symbol", columnList="stock_id"), @Index(name="idx_historical_price_date", columnList="trading_date")})
public class HistoricalPrice {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="stock_id", nullable=false) private Stock stock;
    @Column(name="trading_date", nullable=false) private LocalDate tradingDate;
    @Column(nullable=false, precision=19, scale=6) private BigDecimal open;
    @Column(nullable=false, precision=19, scale=6) private BigDecimal high;
    @Column(nullable=false, precision=19, scale=6) private BigDecimal low;
    @Column(nullable=false, precision=19, scale=6) private BigDecimal close;
    @Column(name="adjusted_close", nullable=false, precision=19, scale=6) private BigDecimal adjustedClose;
    @Column(nullable=false) private long volume;
    @Column(nullable=false, length=40) private String source;
    @Column(nullable=false) private Instant fetchedAt;
    protected HistoricalPrice() {}
    public HistoricalPrice(Stock stock, LocalDate date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal adjustedClose, long volume, String source, Instant fetchedAt) { this.id=UUID.randomUUID(); this.stock=stock; this.tradingDate=date; update(open,high,low,close,adjustedClose,volume,source,fetchedAt); }
    public void update(BigDecimal open,BigDecimal high,BigDecimal low,BigDecimal close,BigDecimal adjustedClose,long volume,String source,Instant fetchedAt) { this.open=open; this.high=high; this.low=low; this.close=close; this.adjustedClose=adjustedClose; this.volume=volume; this.source=source; this.fetchedAt=fetchedAt; }
    public UUID getId(){return id;} public Stock getStock(){return stock;} public LocalDate getTradingDate(){return tradingDate;} public BigDecimal getOpen(){return open;} public BigDecimal getHigh(){return high;} public BigDecimal getLow(){return low;} public BigDecimal getClose(){return close;} public BigDecimal getAdjustedClose(){return adjustedClose;} public long getVolume(){return volume;} public String getSource(){return source;} public Instant getFetchedAt(){return fetchedAt;}
}
