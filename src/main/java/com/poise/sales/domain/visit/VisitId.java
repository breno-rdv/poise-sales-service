package com.poise.sales.domain.visit;

import java.util.UUID;

public record VisitId(String value) {

    public VisitId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("VisitId value must not be blank");
        }
    }

    public static VisitId generate() {
        return new VisitId(UUID.randomUUID().toString());
    }

    public static VisitId of(String value) {
        return new VisitId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
