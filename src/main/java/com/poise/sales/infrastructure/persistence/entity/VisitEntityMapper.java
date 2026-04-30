package com.poise.sales.infrastructure.persistence.entity;

import com.poise.sales.domain.visit.Visit;
import com.poise.sales.domain.visit.VisitDeadline;
import com.poise.sales.domain.visit.VisitId;

public final class VisitEntityMapper {

    private VisitEntityMapper() {}

    public static VisitEntity toEntity(Visit visit) {
        VisitEntity entity = new VisitEntity();
        entity.visitId = visit.visitId().value();
        entity.customerId = visit.customerId();
        entity.vehicleId = visit.vehicleId();
        entity.dealerId = visit.dealerId();
        entity.proposedTime = visit.proposedTime();
        entity.state = visit.state();
        entity.holdId = visit.holdId();
        entity.confirmationDeadline = visit.confirmationDeadline() != null
                ? visit.confirmationDeadline().value()
                : null;
        entity.cancelReason = visit.cancelReason();
        entity.correlationId = visit.correlationId();
        entity.idempotencyKey = visit.idempotencyKey();
        entity.createdAt = visit.createdAt();
        entity.updatedAt = visit.updatedAt();
        return entity;
    }

    public static Visit toDomain(VisitEntity entity) {
        VisitDeadline deadline = entity.confirmationDeadline != null
                ? new VisitDeadline(entity.confirmationDeadline)
                : null;
        return Visit.reconstitute(
                VisitId.of(entity.visitId),
                entity.customerId,
                entity.vehicleId,
                entity.dealerId,
                entity.proposedTime,
                entity.state,
                entity.holdId,
                deadline,
                entity.cancelReason,
                entity.correlationId,
                entity.idempotencyKey,
                entity.createdAt,
                entity.updatedAt);
    }
}
