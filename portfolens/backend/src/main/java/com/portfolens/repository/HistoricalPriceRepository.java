package com.portfolens.repository;
import com.portfolens.entity.HistoricalPrice;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HistoricalPriceRepository extends JpaRepository<HistoricalPrice, UUID> {
    List<HistoricalPrice> findAllByStockSymbolAndTradingDateBetweenOrderByTradingDateAsc(String symbol, LocalDate start, LocalDate end);
    List<HistoricalPrice> findTop2ByStockSymbolOrderByTradingDateDesc(String symbol);
    Optional<HistoricalPrice> findFirstByStockSymbolOrderByTradingDateDesc(String symbol);
    Optional<HistoricalPrice> findByStockIdAndTradingDate(UUID stockId, LocalDate date);
    long countByStockSymbol(String symbol);
}
