package com.poise.sales.api.grpc;

import com.poise.sales.domain.command.ScheduleVisit;
import com.poise.sales.domain.visit.Visit;

import java.time.Instant;

public final class VisitGrpcMapper {

    private VisitGrpcMapper() {}

    public static ScheduleVisit toScheduleCommand(ScheduleVisitRequest request) {
        Instant proposedTime = request.hasProposedTime()
                ? Instant.ofEpochSecond(
                        request.getProposedTime().getSeconds(),
                        request.getProposedTime().getNanos())
                : Instant.now();
        return new ScheduleVisit(
                request.getCustomerId(),
                request.getVehicleId(),
                request.getDealerId(),
                proposedTime,
                request.getIdempotencyKey(),
                request.getCorrelationId());
    }

    public static ScheduleVisitResponse toScheduleResponse(Visit visit) {
        return ScheduleVisitResponse.newBuilder()
                .setVisitId(visit.visitId().value())
                .setState(toprotoState(visit.state()))
                .build();
    }

    static VisitState toprotoState(com.poise.sales.domain.visit.VisitState domainState) {
        return switch (domainState) {
            case DRAFT -> VisitState.DRAFT;
            case HOLD_PENDING -> VisitState.HOLD_PENDING;
            case TENTATIVE -> VisitState.TENTATIVE;
            case CONFIRMED -> VisitState.CONFIRMED;
            case CANCELED -> VisitState.CANCELED;
            case EXPIRED -> VisitState.EXPIRED;
            case COMPLETED -> VisitState.COMPLETED;
        };
    }
}

