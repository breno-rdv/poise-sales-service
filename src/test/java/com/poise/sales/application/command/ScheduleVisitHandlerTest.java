package com.poise.sales.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import com.poise.sales.application.port.CommandPublisher;
import com.poise.sales.application.port.VisitRepository;
import com.poise.sales.domain.command.ScheduleVisit;
import com.poise.sales.domain.visit.VisitState;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@QuarkusTest
class ScheduleVisitHandlerTest {

    @Inject
    ScheduleVisitHandler handler;

    @Inject
    VisitRepository visitRepository;

    @InjectMock
    CommandPublisher commandPublisher;

    @BeforeEach
    void setUp() {
        Mockito.when(commandPublisher.publishHoldVehicleCommand(any()))
                .thenReturn(Uni.createFrom().voidItem());
    }

    @Test
    @RunOnVertxContext
    void handle_persistsVisitAsHoldPending(UniAsserter asserter) {
        ScheduleVisit command = newCommand();

        asserter.assertThat(
                () -> handler.handle(command),
                visit -> {
                    assertNotNull(visit.visitId());
                    assertEquals(VisitState.HOLD_PENDING, visit.state());
                });
    }

    @Test
    @RunOnVertxContext
    void handle_visitIsFoundInRepositoryAfterHandling(UniAsserter asserter) {
        ScheduleVisit command = newCommand();

        asserter.assertThat(
                () -> handler.handle(command)
                        .chain(visit -> visitRepository.findById(visit.visitId())),
                result -> {
                    assertTrue(result.isPresent());
                    assertEquals(VisitState.HOLD_PENDING, result.get().state());
                });
    }

    @Test
    @RunOnVertxContext
    void handle_publishesHoldVehicleCommand(UniAsserter asserter) {
        ScheduleVisit command = newCommand();

        asserter.assertThat(
                () -> handler.handle(command),
                visit -> Mockito.verify(commandPublisher).publishHoldVehicleCommand(any()));
    }

    @Test
    @RunOnVertxContext
    void handle_preservesCommandFieldsOnPersistedVisit(UniAsserter asserter) {
        ScheduleVisit command = newCommand();

        asserter.assertThat(
                () -> handler.handle(command)
                        .chain(visit -> visitRepository.findById(visit.visitId())),
                result -> {
                    assertTrue(result.isPresent());
                    var visit = result.get();
                    assertEquals(command.customerId(), visit.customerId());
                    assertEquals(command.vehicleId(), visit.vehicleId());
                    assertEquals(command.dealerId(), visit.dealerId());
                    assertEquals(command.idempotencyKey(), visit.idempotencyKey());
                    assertEquals(command.correlationId(), visit.correlationId());
                });
    }

    private static ScheduleVisit newCommand() {
        return new ScheduleVisit(
                "cust-" + UUID.randomUUID(),
                "veh-" + UUID.randomUUID(),
                "dealer-1",
                Instant.now().plus(1, ChronoUnit.DAYS),
                "idem-" + UUID.randomUUID(),
                "corr-" + UUID.randomUUID());
    }
}
