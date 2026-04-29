package com.poise.sales.domain.event;

import java.time.Instant;

public sealed interface VisitDomainEvent permits
        VisitScheduled,
        VisitHoldRequested,
        VisitTentative,
        VisitConfirmed,
        VisitCanceled,
        VisitExpired,
        VisitCompleted {

    String visitId();
    Instant occurredAt();
}
