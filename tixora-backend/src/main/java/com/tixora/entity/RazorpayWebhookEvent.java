package com.tixora.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "razorpay_webhook_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_razorpay_webhook_event_id",
                columnNames = "event_id"
        ),
        indexes = @Index(
                name = "idx_razorpay_webhook_received_at",
                columnList = "received_at"
        )
)
public class RazorpayWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100, updatable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    protected RazorpayWebhookEvent() {
    }

    public RazorpayWebhookEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.receivedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
}
