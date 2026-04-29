package com.poise.sales.domain.event;

import java.time.Instant;

public record VisitTentative(
        String visitId,
        String holdId,
        Instant confirmationDeadline,
        Instant occurredAt
) implements VisitDomainEvent {}
