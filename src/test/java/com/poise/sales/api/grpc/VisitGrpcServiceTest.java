package com.poise.sales.api.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Test;

@QuarkusTest
class VisitGrpcServiceTest {

    @GrpcClient
    VisitCommandService visitCommandService;

    @GrpcClient
    VisitQueryService visitQueryService;

    @Test
    void scheduleVisit_returnsHoldPendingVisit() {
        ScheduleVisitResponse response = visitCommandService.scheduleVisit(
                ScheduleVisitRequest.newBuilder()
                        .setCustomerId("cust-1")
                        .setVehicleId("veh-1")
                        .setDealerId("dealer-1")
                        .setIdempotencyKey("idem-grpc-1")
                        .setCorrelationId("corr-grpc-1")
                        .build()
        ).await().atMost(Duration.ofSeconds(5));

        assertNotNull(response.getVisitId());
        assertFalse(response.getVisitId().isBlank());
        assertEquals(VisitState.HOLD_PENDING, response.getState());
    }

    @Test
    void confirmVisit_returnsConfirmedVisit() {
        ConfirmVisitResponse response = visitCommandService.confirmVisit(
                ConfirmVisitRequest.newBuilder()
                        .setVisitId("visit-1")
                        .setDealerId("dealer-1")
                        .setIdempotencyKey("idem-2")
                        .setCorrelationId("corr-2")
                        .build()
        ).await().atMost(Duration.ofSeconds(5));

        assertEquals("visit-1", response.getVisitId());
        assertEquals(VisitState.CONFIRMED, response.getState());
    }

    @Test
    void cancelVisit_returnsCanceledVisit() {
        CancelVisitResponse response = visitCommandService.cancelVisit(
                CancelVisitRequest.newBuilder()
                        .setVisitId("visit-1")
                        .setActorId("cust-1")
                        .setReason("No longer needed")
                        .setIdempotencyKey("idem-3")
                        .setCorrelationId("corr-3")
                        .build()
        ).await().atMost(Duration.ofSeconds(5));

        assertEquals("visit-1", response.getVisitId());
        assertEquals(VisitState.CANCELED, response.getState());
    }

    @Test
    void getVisit_returnsVisitSummary() {
        VisitSummary summary = visitQueryService.getVisit(
                GetVisitRequest.newBuilder()
                        .setVisitId("visit-1")
                        .build()
        ).await().atMost(Duration.ofSeconds(5));

        assertEquals("visit-1", summary.getVisitId());
    }

    @Test
    void listVisitsByCustomer_returnsEmptyList() {
        ListVisitsResponse response = visitQueryService.listVisitsByCustomer(
                ListVisitsByCustomerRequest.newBuilder()
                        .setCustomerId("cust-1")
                        .build()
        ).await().atMost(Duration.ofSeconds(5));

        assertEquals(0, response.getVisitsCount());
    }

    @Test
    void listVisitsByDealer_returnsEmptyList() {
        ListVisitsResponse response = visitQueryService.listVisitsByDealer(
                ListVisitsByDealerRequest.newBuilder()
                        .setDealerId("dealer-1")
                        .build()
        ).await().atMost(Duration.ofSeconds(5));

        assertEquals(0, response.getVisitsCount());
    }
}
