package com.portfolens.analytics.repository;

import com.portfolens.analytics.entity.AnalyticsTransaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsTransactionRepository extends JpaRepository<AnalyticsTransaction, UUID> {
    List<AnalyticsTransaction> findAllByPortfolioIdOrderByTransactionDateAscRecordedAtAsc(UUID portfolioId);
}
