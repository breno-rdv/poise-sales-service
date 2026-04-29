package com.poise.sales.domain.exception;

import com.poise.sales.domain.visit.VisitState;

public class InvalidVisitTransitionException extends RuntimeException {

    private final VisitState from;
    private final String transitionName;

    public InvalidVisitTransitionException(VisitState from, String transitionName) {
        super("Cannot perform '%s' from state %s".formatted(transitionName, from));
        this.from = from;
        this.transitionName = transitionName;
    }

    public VisitState from() {
        return from;
    }

    public String transitionName() {
        return transitionName;
    }
}
