package com.poise.sales.api.grpc;

import com.poise.sales.application.command.ScheduleVisitHandler;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@GrpcService
public class VisitGrpcService implements VisitCommandService {

    @Inject
    ScheduleVisitHandler scheduleVisitHandler;

    @Override
    public Uni<ScheduleVisitResponse> scheduleVisit(ScheduleVisitRequest request) {
        return scheduleVisitHandler
                .handle(VisitGrpcMapper.toScheduleCommand(request))
                .map(VisitGrpcMapper::toScheduleResponse);
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

