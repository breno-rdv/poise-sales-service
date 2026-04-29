package com.poise.sales.domain.event;

import java.time.Instant;

public record VisitScheduled(
        String visitId,
        String customerId,
        String vehicleId,
        String dealerId,
        Instant proposedTime,
        String correlationId,
        Instant occurredAt
) implements VisitDomainEvent {}
