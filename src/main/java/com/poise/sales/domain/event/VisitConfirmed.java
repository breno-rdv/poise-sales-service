package com.poise.sales.domain.event;

import java.time.Instant;

public record VisitConfirmed(
        String visitId,
        String dealerId,
        Instant occurredAt
) implements VisitDomainEvent {}
