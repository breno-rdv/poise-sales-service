package com.poise.sales.application.port;

import com.poise.sales.domain.visit.Visit;
import com.poise.sales.domain.visit.VisitId;
import io.smallrye.mutiny.Uni;

import java.util.Optional;

public interface VisitRepository {

    Uni<Void> save(Visit visit);

    Uni<Optional<Visit>> findById(VisitId visitId);
}
