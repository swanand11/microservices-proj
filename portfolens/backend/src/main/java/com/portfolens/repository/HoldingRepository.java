package com.portfolens.repository;
import com.portfolens.entity.Holding;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HoldingRepository extends JpaRepository<Holding, UUID> {
    List<Holding> findAllByPortfolioIdOrderByStockSymbol(UUID portfolioId);
    Optional<Holding> findByIdAndPortfolioId(UUID id, UUID portfolioId);
    Optional<Holding> findByPortfolioIdAndStockId(UUID portfolioId, UUID stockId);
    boolean existsByPortfolioIdAndStockId(UUID portfolioId, UUID stockId);
    void deleteAllByPortfolioId(UUID portfolioId);
}
