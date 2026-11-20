package com.portfolens.repository;
import com.portfolens.entity.Stock;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface StockRepository extends JpaRepository<Stock, UUID> {
    Optional<Stock> findByIdAndActiveTrue(UUID id);
    @Query("select s from Stock s where s.active = true and (:sector is null or lower(s.sector) = lower(:sector)) and (:q is null or lower(s.symbol) like lower(concat('%', :q, '%')) or lower(s.companyName) like lower(concat('%', :q, '%'))) order by s.symbol")
    List<Stock> search(@Param("q") String query, @Param("sector") String sector);
    boolean existsBySymbol(String symbol);
    Optional<Stock> findBySymbolIgnoreCase(String symbol);
}
