package com.poise.sales.application.command;

import com.poise.sales.application.port.CommandPublisher;
import com.poise.sales.application.port.VisitRepository;
import com.poise.sales.domain.command.ScheduleVisit;
import com.poise.sales.domain.visit.Visit;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ScheduleVisitHandler {

    @Inject
    VisitRepository visitRepository;

    @Inject
    CommandPublisher commandPublisher;

    public Uni<Visit> handle(ScheduleVisit command) {
        Visit visit = Visit.schedule(
                command.customerId(),
                command.vehicleId(),
                command.dealerId(),
                command.proposedTime(),
                command.correlationId(),
                command.idempotencyKey());

        return visitRepository.save(visit)
                .invoke(visit::markHoldRequested)
                .chain(() -> commandPublisher.publishHoldVehicleCommand(visit))
                .chain(() -> visitRepository.save(visit))
                .replaceWith(visit);
    }
}
