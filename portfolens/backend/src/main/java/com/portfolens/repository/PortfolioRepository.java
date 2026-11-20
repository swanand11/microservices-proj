package com.portfolens.repository;
import com.portfolens.entity.Portfolio;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {
    List<Portfolio> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Portfolio> findByIdAndUserId(UUID id, UUID userId);
}
