package com.poise.sales.infrastructure.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.poise.sales.application.port.VisitRepository;
import com.poise.sales.domain.visit.Visit;
import com.poise.sales.domain.visit.VisitDeadline;
import com.poise.sales.domain.visit.VisitId;
import com.poise.sales.domain.visit.VisitPolicy;
import com.poise.sales.domain.visit.VisitState;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@QuarkusTest
class JpaVisitRepositoryTest {

    @Inject
    VisitRepository visitRepository;

    @Test
    @RunOnVertxContext
    void saveAndFindById_roundTripsVisitInDraftState(UniAsserter asserter) {
        Visit visit = newVisit();

        asserter.assertThat(
                () -> visitRepository.save(visit)
                        .chain(() -> visitRepository.findById(visit.visitId())),
                result -> {
                    assertTrue(result.isPresent());
                    Visit loaded = result.get();
                    assertEquals(visit.visitId(), loaded.visitId());
                    assertEquals(visit.customerId(), loaded.customerId());
                    assertEquals(visit.vehicleId(), loaded.vehicleId());
                    assertEquals(visit.dealerId(), loaded.dealerId());
                    assertEquals(VisitState.DRAFT, loaded.state());
                    assertEquals(visit.idempotencyKey(), loaded.idempotencyKey());
                    assertEquals(visit.correlationId(), loaded.correlationId());
                });
    }

    @Test
    @RunOnVertxContext
    void save_updatesExistingVisitOnSecondCall(UniAsserter asserter) {
        Visit visit = newVisit();
        visit.markHoldRequested();
        VisitDeadline deadline = VisitPolicy.defaultDeadlineFrom(Instant.now());
        visit.markTentative("hold-xyz", deadline);

        asserter.assertThat(
                () -> visitRepository.save(visit)
                        .chain(() -> {
                            visit.confirm("dealer-updated");
                            return visitRepository.save(visit);
                        })
                        .chain(() -> visitRepository.findById(visit.visitId())),
                result -> {
                    assertTrue(result.isPresent());
                    Visit loaded = result.get();
                    assertEquals(VisitState.CONFIRMED, loaded.state());
                    assertEquals("dealer-updated", loaded.dealerId());
                });
    }

    @Test
    @RunOnVertxContext
    void save_persistsConfirmationDeadlineForTentativeVisit(UniAsserter asserter) {
        Visit visit = newVisit();
        visit.markHoldRequested();
        VisitDeadline deadline = VisitPolicy.defaultDeadlineFrom(Instant.now());
        visit.markTentative("hold-abc", deadline);

        asserter.assertThat(
                () -> visitRepository.save(visit)
                        .chain(() -> visitRepository.findById(visit.visitId())),
                result -> {
                    assertTrue(result.isPresent());
                    Visit loaded = result.get();
                    assertEquals(VisitState.TENTATIVE, loaded.state());
                    assertEquals("hold-abc", loaded.holdId());
                    assertNotNull(loaded.confirmationDeadline());
                    assertEquals(
                            deadline.value().truncatedTo(ChronoUnit.MILLIS),
                            loaded.confirmationDeadline().value().truncatedTo(ChronoUnit.MILLIS));
                });
    }

    @Test
    @RunOnVertxContext
    void findById_returnsEmptyWhenNotFound(UniAsserter asserter) {
        asserter.assertThat(
                () -> visitRepository.findById(VisitId.of(UUID.randomUUID().toString())),
                result -> assertTrue(result.isEmpty()));
    }

    @Test
    @RunOnVertxContext
    void save_persistsCancelReasonForCanceledVisit(UniAsserter asserter) {
        Visit visit = newVisit();
        visit.cancel("cust-1", "Changed my mind");

        asserter.assertThat(
                () -> visitRepository.save(visit)
                        .chain(() -> visitRepository.findById(visit.visitId())),
                result -> {
                    assertTrue(result.isPresent());
                    Visit loaded = result.get();
                    assertEquals(VisitState.CANCELED, loaded.state());
                    assertEquals("Changed my mind", loaded.cancelReason());
                });
    }

    private static Visit newVisit() {
        return Visit.schedule(
                "cust-" + UUID.randomUUID(),
                "veh-" + UUID.randomUUID(),
                "dealer-1",
                Instant.now().plus(1, ChronoUnit.DAYS),
                "corr-" + UUID.randomUUID(),
                "idem-" + UUID.randomUUID());
    }
}
