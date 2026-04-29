package com.poise.sales.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class VisitQueryGrpcService implements VisitQueryService {

    @Override
    public Uni<VisitSummary> getVisit(GetVisitRequest request) {
        return Uni.createFrom().item(VisitSummary.newBuilder()
                .setVisitId(request.getVisitId())
                .build());
    }

    @Override
    public Uni<ListVisitsResponse> listVisitsByCustomer(ListVisitsByCustomerRequest request) {
        return Uni.createFrom().item(ListVisitsResponse.newBuilder().build());
    }

    @Override
    public Uni<ListVisitsResponse> listVisitsByDealer(ListVisitsByDealerRequest request) {
        return Uni.createFrom().item(ListVisitsResponse.newBuilder().build());
    }
}
