package com.portfolens.repository;
import com.portfolens.entity.Portfolio;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {
    List<Portfolio> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Portfolio> findByIdAndUserId(UUID id, UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Portfolio p where p.id = :id and p.user.id = :userId")
    Optional<Portfolio> lockByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
