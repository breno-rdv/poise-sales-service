package com.poise.sales.domain.command;

public record ConfirmVisit(
        String visitId,
        String dealerId,
        String idempotencyKey,
        String correlationId
) {}
