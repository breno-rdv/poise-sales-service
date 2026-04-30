package com.poise.sales.infrastructure.messaging.message;

import java.time.Instant;

public record HoldVehicleCommandMessage(
        String eventId,
        String correlationId,
        String causationId,
        String idempotencyKey,
        Instant occurredAt,
        String aggregateId,
        String aggregateType,
        String visitId,
        String vehicleId,
        String customerId,
        String dealerId
) {}
