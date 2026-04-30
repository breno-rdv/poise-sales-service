package com.poise.sales.infrastructure.persistence.entity;

import com.poise.sales.domain.visit.VisitState;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "visits")
public class VisitEntity extends PanacheEntityBase {

    @Id
    @Column(name = "visit_id")
    public String visitId;

    @Column(name = "customer_id", nullable = false)
    public String customerId;

    @Column(name = "vehicle_id", nullable = false)
    public String vehicleId;

    @Column(name = "dealer_id", nullable = false)
    public String dealerId;

    @Column(name = "proposed_time", nullable = false)
    public Instant proposedTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    public VisitState state;

    @Column(name = "hold_id")
    public String holdId;

    @Column(name = "confirmation_deadline")
    public Instant confirmationDeadline;

    @Column(name = "cancel_reason")
    public String cancelReason;

    @Column(name = "correlation_id")
    public String correlationId;

    @Column(name = "idempotency_key", nullable = false)
    public String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
