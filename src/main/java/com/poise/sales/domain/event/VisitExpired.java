package com.poise.sales.domain.event;

import java.time.Instant;

public record VisitExpired(
        String visitId,
        Instant occurredAt
) implements VisitDomainEvent {}
