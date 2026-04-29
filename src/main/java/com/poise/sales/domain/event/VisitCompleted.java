package com.poise.sales.domain.event;

import java.time.Instant;

public record VisitCompleted(
        String visitId,
        Instant occurredAt
) implements VisitDomainEvent {}
