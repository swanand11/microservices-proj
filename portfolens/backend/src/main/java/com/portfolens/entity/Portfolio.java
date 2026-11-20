package com.portfolens.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "portfolios", indexes = @Index(name = "idx_portfolios_user", columnList = "user_id"))
public class Portfolio {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 1000) private String description;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected Portfolio() {}
    public Portfolio(User user, String name, String description) { this.id = UUID.randomUUID(); this.user = user; this.name = name; this.description = description; }
    @PrePersist void create() { var now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void update() { updatedAt = Instant.now(); }
    public UUID getId() { return id; } public User getUser() { return user; } public String getName() { return name; } public String getDescription() { return description; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
    public void update(String name, String description) { this.name = name; this.description = description; }
}
