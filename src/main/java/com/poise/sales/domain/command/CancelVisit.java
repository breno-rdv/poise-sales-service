package com.poise.sales.domain.command;

public record CancelVisit(
        String visitId,
        String actorId,
        String reason,
        String idempotencyKey,
        String correlationId
) {}
