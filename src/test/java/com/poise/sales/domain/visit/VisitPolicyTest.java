package com.poise.sales.domain.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

class VisitPolicyTest {

    @ParameterizedTest
    @EnumSource(value = VisitState.class, names = {"DRAFT", "HOLD_PENDING", "TENTATIVE", "CONFIRMED"})
    void canCancel_returnsTrueForCancellableStates(VisitState state) {
        assertTrue(VisitPolicy.canCancel(state));
    }

    @ParameterizedTest
    @EnumSource(value = VisitState.class, names = {"COMPLETED", "CANCELED", "EXPIRED"})
    void canCancel_returnsFalseForTerminalStates(VisitState state) {
        assertFalse(VisitPolicy.canCancel(state));
    }

    @Test
    void canExpire_returnsTrueOnlyForTentative() {
        assertTrue(VisitPolicy.canExpire(VisitState.TENTATIVE));
    }

    @ParameterizedTest
    @EnumSource(value = VisitState.class, names = {"DRAFT", "HOLD_PENDING", "CONFIRMED", "COMPLETED", "CANCELED", "EXPIRED"})
    void canExpire_returnsFalseForNonTentativeStates(VisitState state) {
        assertFalse(VisitPolicy.canExpire(state));
    }

    @Test
    void defaultDeadlineFrom_returns48hInFuture() {
        Instant now = Instant.now();
        VisitDeadline deadline = VisitPolicy.defaultDeadlineFrom(now);

        assertEquals(now.plus(VisitPolicy.DEFAULT_CONFIRMATION_DEADLINE), deadline.value());
    }

    @Test
    void visitDeadline_hasExpired_returnsTrueWhenPastDeadline() {
        VisitDeadline deadline = new VisitDeadline(Instant.now().minusSeconds(1));
        assertTrue(deadline.hasExpired(Instant.now()));
    }

    @Test
    void visitDeadline_hasExpired_returnsFalseWhenBeforeDeadline() {
        VisitDeadline deadline = new VisitDeadline(Instant.now().plusSeconds(3600));
        assertFalse(deadline.hasExpired(Instant.now()));
    }
}
