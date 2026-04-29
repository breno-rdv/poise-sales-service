package com.poise.sales.domain.visit;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public final class VisitPolicy {

    public static final Duration DEFAULT_CONFIRMATION_DEADLINE = Duration.ofHours(48);

    private static final Set<VisitState> CANCELLABLE_STATES = Set.of(
            VisitState.DRAFT,
            VisitState.HOLD_PENDING,
            VisitState.TENTATIVE,
            VisitState.CONFIRMED
    );

    private VisitPolicy() {}

    public static boolean canCancel(VisitState state) {
        return CANCELLABLE_STATES.contains(state);
    }

    public static boolean canExpire(VisitState state) {
        return state == VisitState.TENTATIVE;
    }

    public static VisitDeadline defaultDeadlineFrom(Instant now) {
        return new VisitDeadline(now.plus(DEFAULT_CONFIRMATION_DEADLINE));
    }
}
