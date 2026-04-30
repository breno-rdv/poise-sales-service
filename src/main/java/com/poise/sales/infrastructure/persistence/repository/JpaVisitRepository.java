package com.poise.sales.infrastructure.persistence.repository;

import com.poise.sales.application.port.VisitRepository;
import com.poise.sales.domain.visit.Visit;
import com.poise.sales.domain.visit.VisitId;
import com.poise.sales.infrastructure.persistence.entity.VisitEntity;
import com.poise.sales.infrastructure.persistence.entity.VisitEntityMapper;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class JpaVisitRepository implements VisitRepository {

    @Override
    @WithTransaction
    public Uni<Void> save(Visit visit) {
        VisitEntity entity = VisitEntityMapper.toEntity(visit);
        return Panache.getSession()
                .flatMap(session -> session.merge(entity))
                .replaceWithVoid();
    }

    @Override
    public Uni<Optional<Visit>> findById(VisitId visitId) {
        return Panache.withSession(() ->
                VisitEntity.<VisitEntity>findById(visitId.value())
                        .map(entity -> Optional.ofNullable(entity).map(VisitEntityMapper::toDomain)));
    }
}
