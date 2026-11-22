package com.portfolens.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = @Index(name = "idx_outbox_pending", columnList = "published_at, created_at"))
public class OutboxEvent {
    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String eventType;

    @Column(nullable = false, length = 180)
    private String routingKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(length = 500)
    private String lastError;

    protected OutboxEvent() {}

    public OutboxEvent(UUID id, String eventType, String routingKey, String payload) {
        this.id = id;
        this.eventType = eventType;
        this.routingKey = routingKey;
        this.payload = payload;
    }

    @PrePersist
    void create() { createdAt = Instant.now(); }

    public void markPublished() { publishedAt = Instant.now(); lastError = null; }
    public void markFailed(String error) {
        attempts++;
        lastError = error == null ? "Unknown publish failure" : error.substring(0, Math.min(error.length(), 500));
    }

    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public String getRoutingKey() { return routingKey; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
}
