package com.poise.sales.domain.event;

import java.time.Instant;

public record VisitHoldRequested(
        String visitId,
        String vehicleId,
        String correlationId,
        Instant occurredAt
) implements VisitDomainEvent {}
