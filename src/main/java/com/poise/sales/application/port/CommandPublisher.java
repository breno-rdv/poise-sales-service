package com.poise.sales.application.port;

import com.poise.sales.domain.visit.Visit;
import io.smallrye.mutiny.Uni;

public interface CommandPublisher {

    Uni<Void> publishHoldVehicleCommand(Visit visit);
}
