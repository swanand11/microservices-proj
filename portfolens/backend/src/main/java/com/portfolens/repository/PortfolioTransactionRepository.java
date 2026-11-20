package com.portfolens.repository;
import com.portfolens.entity.PortfolioTransaction;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PortfolioTransactionRepository extends JpaRepository<PortfolioTransaction, UUID> {
    List<PortfolioTransaction> findAllByPortfolioIdOrderByTransactionDateDescCreatedAtDesc(UUID portfolioId);
    void deleteAllByPortfolioId(UUID portfolioId);
}
