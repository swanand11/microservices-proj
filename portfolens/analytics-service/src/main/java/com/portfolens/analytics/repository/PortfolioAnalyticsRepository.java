package com.portfolens.analytics.repository;

import com.portfolens.analytics.entity.PortfolioAnalyticsSnapshot;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioAnalyticsRepository extends JpaRepository<PortfolioAnalyticsSnapshot, UUID> {}
