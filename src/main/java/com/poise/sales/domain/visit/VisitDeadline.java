package com.poise.sales.domain.visit;

import java.time.Instant;

public record VisitDeadline(Instant value) {

    public VisitDeadline {
        if (value == null) {
            throw new IllegalArgumentException("VisitDeadline value must not be null");
        }
    }

    public boolean hasExpired(Instant now) {
        return now.isAfter(value);
    }
}
