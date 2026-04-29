package com.poise.sales.domain.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.poise.sales.domain.event.VisitCanceled;
import com.poise.sales.domain.event.VisitCompleted;
import com.poise.sales.domain.event.VisitConfirmed;
import com.poise.sales.domain.event.VisitDomainEvent;
import com.poise.sales.domain.event.VisitExpired;
import com.poise.sales.domain.event.VisitHoldRequested;
import com.poise.sales.domain.event.VisitScheduled;
import com.poise.sales.domain.event.VisitTentative;
import com.poise.sales.domain.exception.InvalidVisitTransitionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

class VisitTest {

    @Test
    void schedule_createsDraftVisitWithEvent() {
        Visit visit = Visit.schedule("cust-1", "veh-1", "dealer-1",
                Instant.now().plus(1, ChronoUnit.DAYS), "corr-1", "idem-1");

        assertEquals(VisitState.DRAFT, visit.state());
        assertNotNull(visit.visitId());
        assertEquals("cust-1", visit.customerId());
        assertEquals("veh-1", visit.vehicleId());
        assertEquals("dealer-1", visit.dealerId());
        assertNotNull(visit.createdAt());
        assertNotNull(visit.updatedAt());

        List<VisitDomainEvent> events = visit.drainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(VisitScheduled.class, events.getFirst());

        VisitScheduled event = (VisitScheduled) events.getFirst();
        assertEquals(visit.visitId().value(), event.visitId());
        assertEquals("cust-1", event.customerId());
        assertEquals("corr-1", event.correlationId());
    }

    @Test
    void drainEvents_clearsEventsAfterDrain() {
        Visit visit = Visit.schedule("cust-1", "veh-1", "dealer-1",
                Instant.now(), "corr-1", "idem-1");

        visit.drainEvents();

        assertTrue(visit.drainEvents().isEmpty());
    }

    @Test
    void markHoldRequested_transitionsDraftToHoldPending() {
        Visit visit = draftVisit();
        visit.drainEvents();

        visit.markHoldRequested();

        assertEquals(VisitState.HOLD_PENDING, visit.state());
        List<VisitDomainEvent> events = visit.drainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(VisitHoldRequested.class, events.getFirst());
    }

    @Test
    void markHoldRequested_failsIfNotDraft() {
        Visit visit = holdPendingVisit();

        assertThrows(InvalidVisitTransitionException.class, visit::markHoldRequested);
    }

    @Test
    void markTentative_transitionsHoldPendingToTentative() {
        Visit visit = holdPendingVisit();
        VisitDeadline deadline = VisitPolicy.defaultDeadlineFrom(Instant.now());
        visit.drainEvents();

        visit.markTentative("hold-abc", deadline);

        assertEquals(VisitState.TENTATIVE, visit.state());
        assertEquals("hold-abc", visit.holdId());
        assertEquals(deadline, visit.confirmationDeadline());

        List<VisitDomainEvent> events = visit.drainEvents();
        assertEquals(1, events.size());
        VisitTentative event = (VisitTentative) events.getFirst();
        assertEquals("hold-abc", event.holdId());
    }

    @Test
    void markTentative_failsIfNotHoldPending() {
        Visit visit = draftVisit();
        VisitDeadline deadline = VisitPolicy.defaultDeadlineFrom(Instant.now());

        assertThrows(InvalidVisitTransitionException.class,
                () -> visit.markTentative("hold-abc", deadline));
    }

    @Test
    void confirm_transitionsTentativeToConfirmed() {
        Visit visit = tentativeVisit();
        visit.drainEvents();

        visit.confirm("dealer-1");

        assertEquals(VisitState.CONFIRMED, visit.state());
        assertEquals("dealer-1", visit.dealerId());

        List<VisitDomainEvent> events = visit.drainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(VisitConfirmed.class, events.getFirst());
    }

    @Test
    void confirm_failsIfNotTentative() {
        Visit visit = draftVisit();

        assertThrows(InvalidVisitTransitionException.class, () -> visit.confirm("dealer-1"));
    }

    @Test
    void complete_transitionsConfirmedToCompleted() {
        Visit visit = confirmedVisit();
        visit.drainEvents();

        visit.complete();

        assertEquals(VisitState.COMPLETED, visit.state());
        List<VisitDomainEvent> events = visit.drainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(VisitCompleted.class, events.getFirst());
    }

    @Test
    void complete_failsIfNotConfirmed() {
        Visit visit = tentativeVisit();

        assertThrows(InvalidVisitTransitionException.class, visit::complete);
    }

    @ParameterizedTest
    @EnumSource(value = VisitState.class, names = {"DRAFT", "HOLD_PENDING", "TENTATIVE", "CONFIRMED"})
    void cancel_allowedFromCancellableStates(VisitState startState) {
        Visit visit = visitInState(startState);
        visit.drainEvents();

        visit.cancel("actor-1", "No longer needed");

        assertEquals(VisitState.CANCELED, visit.state());
        assertEquals("No longer needed", visit.cancelReason());
        List<VisitDomainEvent> events = visit.drainEvents();
        assertEquals(1, events.size());
        VisitCanceled event = (VisitCanceled) events.getFirst();
        assertEquals("actor-1", event.actorId());
    }

    @ParameterizedTest
    @EnumSource(value = VisitState.class, names = {"COMPLETED", "CANCELED", "EXPIRED"})
    void cancel_failsFromTerminalStates(VisitState terminalState) {
        Visit visit = visitInState(terminalState);

        assertThrows(InvalidVisitTransitionException.class,
                () -> visit.cancel("actor-1", "reason"));
    }

    @Test
    void expire_transitionsTentativeToExpired() {
        Visit visit = tentativeVisit();
        visit.drainEvents();

        visit.expire();

        assertEquals(VisitState.EXPIRED, visit.state());
        List<VisitDomainEvent> events = visit.drainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(VisitExpired.class, events.getFirst());
    }

    @ParameterizedTest
    @EnumSource(value = VisitState.class, names = {"DRAFT", "HOLD_PENDING", "CONFIRMED", "COMPLETED", "CANCELED", "EXPIRED"})
    void expire_failsIfNotTentative(VisitState state) {
        Visit visit = visitInState(state);

        assertThrows(InvalidVisitTransitionException.class, visit::expire);
    }

    @Test
    void invalidTransitionException_carriesStateAndOperationName() {
        Visit visit = draftVisit();

        InvalidVisitTransitionException ex = assertThrows(
                InvalidVisitTransitionException.class, () -> visit.confirm("dealer-1"));

        assertEquals(VisitState.DRAFT, ex.from());
        assertEquals("confirm", ex.transitionName());
    }

    // --- Helpers ---

    private static Visit draftVisit() {
        return Visit.schedule("cust-1", "veh-1", "dealer-1",
                Instant.now().plus(1, ChronoUnit.DAYS), "corr-1", "idem-1");
    }

    private static Visit holdPendingVisit() {
        Visit visit = draftVisit();
        visit.markHoldRequested();
        return visit;
    }

    private static Visit tentativeVisit() {
        Visit visit = holdPendingVisit();
        visit.markTentative("hold-abc", VisitPolicy.defaultDeadlineFrom(Instant.now()));
        return visit;
    }

    private static Visit confirmedVisit() {
        Visit visit = tentativeVisit();
        visit.confirm("dealer-1");
        return visit;
    }

    private static Visit visitInState(VisitState target) {
        return switch (target) {
            case DRAFT -> draftVisit();
            case HOLD_PENDING -> holdPendingVisit();
            case TENTATIVE -> tentativeVisit();
            case CONFIRMED -> confirmedVisit();
            case COMPLETED -> {
                Visit v = confirmedVisit();
                v.complete();
                yield v;
            }
            case CANCELED -> {
                Visit v = draftVisit();
                v.cancel("actor-1", "test");
                yield v;
            }
            case EXPIRED -> {
                Visit v = tentativeVisit();
                v.expire();
                yield v;
            }
        };
    }

    private static <T> void assertInstanceOf(Class<T> expected, Object actual) {
        assertTrue(expected.isInstance(actual),
                "Expected instance of %s but was %s".formatted(expected.getSimpleName(),
                        actual.getClass().getSimpleName()));
    }
}
