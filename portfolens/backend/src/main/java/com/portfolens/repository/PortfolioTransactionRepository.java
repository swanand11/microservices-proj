package com.portfolens.repository;
import com.portfolens.entity.PortfolioTransaction;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PortfolioTransactionRepository extends JpaRepository<PortfolioTransaction, UUID> {
    List<PortfolioTransaction> findAllByPortfolioIdOrderByTransactionDateDescCreatedAtDesc(UUID portfolioId);
    java.util.Optional<PortfolioTransaction> findByPortfolioIdAndIdempotencyKey(UUID portfolioId, String idempotencyKey);
    void deleteAllByPortfolioId(UUID portfolioId);
}
