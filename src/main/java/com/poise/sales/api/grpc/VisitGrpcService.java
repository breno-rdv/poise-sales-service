package com.poise.sales.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

@GrpcService
public class VisitGrpcService implements VisitCommandService {

    @Override
    public Uni<ScheduleVisitResponse> scheduleVisit(ScheduleVisitRequest request) {
        return Uni.createFrom().item(ScheduleVisitResponse.newBuilder()
                .setVisitId(UUID.randomUUID().toString())
                .setState(VisitState.DRAFT)
                .build());
    }

    @Override
    public Uni<ConfirmVisitResponse> confirmVisit(ConfirmVisitRequest request) {
        return Uni.createFrom().item(ConfirmVisitResponse.newBuilder()
                .setVisitId(request.getVisitId())
                .setState(VisitState.CONFIRMED)
                .build());
    }

    @Override
    public Uni<CancelVisitResponse> cancelVisit(CancelVisitRequest request) {
        return Uni.createFrom().item(CancelVisitResponse.newBuilder()
                .setVisitId(request.getVisitId())
                .setState(VisitState.CANCELED)
                .build());
    }
}
