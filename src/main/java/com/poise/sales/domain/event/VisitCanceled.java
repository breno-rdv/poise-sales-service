package com.poise.sales.domain.event;

import java.time.Instant;

public record VisitCanceled(
        String visitId,
        String actorId,
        String reason,
        Instant occurredAt
) implements VisitDomainEvent {}
