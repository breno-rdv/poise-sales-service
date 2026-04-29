package com.poise.sales.domain.visit;

import com.poise.sales.domain.event.VisitCanceled;
import com.poise.sales.domain.event.VisitCompleted;
import com.poise.sales.domain.event.VisitConfirmed;
import com.poise.sales.domain.event.VisitDomainEvent;
import com.poise.sales.domain.event.VisitExpired;
import com.poise.sales.domain.event.VisitHoldRequested;
import com.poise.sales.domain.event.VisitScheduled;
import com.poise.sales.domain.event.VisitTentative;
import com.poise.sales.domain.exception.InvalidVisitTransitionException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Visit {

    private final VisitId visitId;
    private final String customerId;
    private final String vehicleId;
    private String dealerId;
    private final Instant proposedTime;
    private VisitState state;
    private String holdId;
    private VisitDeadline confirmationDeadline;
    private String cancelReason;
    private final String correlationId;
    private final String idempotencyKey;
    private final Instant createdAt;
    private Instant updatedAt;

    private final List<VisitDomainEvent> domainEvents = new ArrayList<>();

    private Visit(
            VisitId visitId,
            String customerId,
            String vehicleId,
            String dealerId,
            Instant proposedTime,
            VisitState state,
            String holdId,
            VisitDeadline confirmationDeadline,
            String cancelReason,
            String correlationId,
            String idempotencyKey,
            Instant createdAt,
            Instant updatedAt) {
        this.visitId = visitId;
        this.customerId = customerId;
        this.vehicleId = vehicleId;
        this.dealerId = dealerId;
        this.proposedTime = proposedTime;
        this.state = state;
        this.holdId = holdId;
        this.confirmationDeadline = confirmationDeadline;
        this.cancelReason = cancelReason;
        this.correlationId = correlationId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Visit schedule(
            String customerId,
            String vehicleId,
            String dealerId,
            Instant proposedTime,
            String correlationId,
            String idempotencyKey) {
        Instant now = Instant.now();
        VisitId visitId = VisitId.generate();
        Visit visit = new Visit(
                visitId, customerId, vehicleId, dealerId, proposedTime,
                VisitState.DRAFT, null, null, null,
                correlationId, idempotencyKey, now, now);
        visit.domainEvents.add(new VisitScheduled(
                visitId.value(), customerId, vehicleId, dealerId, proposedTime, correlationId, now));
        return visit;
    }

    /** Reconstitutes a Visit from persistence without raising domain events. */
    public static Visit reconstitute(
            VisitId visitId,
            String customerId,
            String vehicleId,
            String dealerId,
            Instant proposedTime,
            VisitState state,
            String holdId,
            VisitDeadline confirmationDeadline,
            String cancelReason,
            String correlationId,
            String idempotencyKey,
            Instant createdAt,
            Instant updatedAt) {
        return new Visit(visitId, customerId, vehicleId, dealerId, proposedTime,
                state, holdId, confirmationDeadline, cancelReason,
                correlationId, idempotencyKey, createdAt, updatedAt);
    }

    public void markHoldRequested() {
        requireState(VisitState.DRAFT, "markHoldRequested");
        Instant now = Instant.now();
        this.state = VisitState.HOLD_PENDING;
        this.updatedAt = now;
        domainEvents.add(new VisitHoldRequested(visitId.value(), vehicleId, correlationId, now));
    }

    public void markTentative(String holdId, VisitDeadline deadline) {
        requireState(VisitState.HOLD_PENDING, "markTentative");
        Instant now = Instant.now();
        this.state = VisitState.TENTATIVE;
        this.holdId = holdId;
        this.confirmationDeadline = deadline;
        this.updatedAt = now;
        domainEvents.add(new VisitTentative(visitId.value(), holdId, deadline.value(), now));
    }

    public void confirm(String dealerId) {
        requireState(VisitState.TENTATIVE, "confirm");
        Instant now = Instant.now();
        this.state = VisitState.CONFIRMED;
        this.dealerId = dealerId;
        this.updatedAt = now;
        domainEvents.add(new VisitConfirmed(visitId.value(), dealerId, now));
    }

    public void cancel(String actorId, String reason) {
        if (!VisitPolicy.canCancel(this.state)) {
            throw new InvalidVisitTransitionException(this.state, "cancel");
        }
        Instant now = Instant.now();
        this.state = VisitState.CANCELED;
        this.cancelReason = reason;
        this.updatedAt = now;
        domainEvents.add(new VisitCanceled(visitId.value(), actorId, reason, now));
    }

    public void expire() {
        if (!VisitPolicy.canExpire(this.state)) {
            throw new InvalidVisitTransitionException(this.state, "expire");
        }
        Instant now = Instant.now();
        this.state = VisitState.EXPIRED;
        this.updatedAt = now;
        domainEvents.add(new VisitExpired(visitId.value(), now));
    }

    public void complete() {
        requireState(VisitState.CONFIRMED, "complete");
        Instant now = Instant.now();
        this.state = VisitState.COMPLETED;
        this.updatedAt = now;
        domainEvents.add(new VisitCompleted(visitId.value(), now));
    }

    /** Returns all pending domain events and clears the internal list. */
    public List<VisitDomainEvent> drainEvents() {
        List<VisitDomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void requireState(VisitState required, String operation) {
        if (this.state != required) {
            throw new InvalidVisitTransitionException(this.state, operation);
        }
    }

    public VisitId visitId() { return visitId; }
    public String customerId() { return customerId; }
    public String vehicleId() { return vehicleId; }
    public String dealerId() { return dealerId; }
    public Instant proposedTime() { return proposedTime; }
    public VisitState state() { return state; }
    public String holdId() { return holdId; }
    public VisitDeadline confirmationDeadline() { return confirmationDeadline; }
    public String cancelReason() { return cancelReason; }
    public String correlationId() { return correlationId; }
    public String idempotencyKey() { return idempotencyKey; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
