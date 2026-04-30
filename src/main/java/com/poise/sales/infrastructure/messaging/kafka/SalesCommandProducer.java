package com.poise.sales.infrastructure.messaging.kafka;

import com.poise.sales.application.port.CommandPublisher;
import com.poise.sales.domain.visit.Visit;
import com.poise.sales.infrastructure.messaging.message.HoldVehicleCommandMessage;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class SalesCommandProducer implements CommandPublisher {

    @Inject
    @Channel("sales-commands")
    MutinyEmitter<HoldVehicleCommandMessage> commandEmitter;

    @Override
    public Uni<Void> publishHoldVehicleCommand(Visit visit) {
        HoldVehicleCommandMessage message = new HoldVehicleCommandMessage(
                UUID.randomUUID().toString(),
                visit.correlationId(),
                visit.idempotencyKey(),
                visit.idempotencyKey(),
                Instant.now(),
                visit.visitId().value(),
                "Visit",
                visit.visitId().value(),
                visit.vehicleId(),
                visit.customerId(),
                visit.dealerId());
        return commandEmitter.send(message);
    }
}
