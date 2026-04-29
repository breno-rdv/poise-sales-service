package com.poise.sales.domain.command;

import java.time.Instant;

public record ScheduleVisit(
        String customerId,
        String vehicleId,
        String dealerId,
        Instant proposedTime,
        String idempotencyKey,
        String correlationId
) {}
