## Tech Stack
- Runtime: Quarkus 3.32.3, Java 25
- Reactive: Mutiny (`Uni`/`Multi`) for all async operations
- Transport: gRPC (quarkus-grpc) — inter-service calls
- Messaging: SmallRye Reactive Messaging + Kafka (quarkus-smallrye-reactive-messaging-kafka) [PLANNED]
- Persistence: Hibernate Reactive + Panache, PostgreSQL (quarkus-hibernate-reactive-panache, quarkus-reactive-pg-client) [PLANNED]
- DI: Quarkus Arc (CDI)
- Root package: `com.poise`

## Package Structure (PLANNED)
```
com.poise.visit          # Visit aggregate, state machine, repository
com.poise.saga           # Saga orchestration (hold → confirm → cancel)
com.poise.events         # Kafka event classes (inbound & outbound)
com.poise.api            # gRPC service endpoints (schedule/confirm/cancel)
com.poise.infra          # Kafka producers, gRPC client stubs, DB config
```

## Implementation Status
- [IMPLEMENTED] Quarkus framework bootstrap
- [IMPLEMENTED] gRPC server infrastructure (quarkus-grpc, hello.proto stub)
- [IMPLEMENTED] Kafka broker provisioned in docker-compose
- [PLANNED] Visit domain aggregate & state machine
- [PLANNED] Saga orchestration (hold request → confirmation → expiry)
- [PLANNED] Kafka consumers/producers
- [PLANNED] Persistence layer (Panache + PostgreSQL)
- [PLANNED] Customer-facing gRPC API (schedule/confirm/cancel)

## 1) Domain & Service Boundaries
Inventory Service (Source of Truth for Vehicles & Availability)
Owns Vehicle lifecycle: AVAILABLE | HELD | BOOKED | SOLD | OFFLINE.
Owns time-slot availability per vehicle.
Exposes commands to create/confirm/release holds and bookings.
Publishes events: VehicleSold, AvailabilityHeld, AvailabilityHoldExpired, VisitBooked, VisitCanceled, VehicleUnavailable.
Sales Service (Orchestrator + Visit/Lead Domain)
Owns Visit aggregate: DRAFT → HOLD_PENDING → TENTATIVE → CONFIRMED → COMPLETED | CANCELED | EXPIRED.
Orchestrates saga: request hold, confirm booking, handle cancellations/timeouts.
Maintains idempotency, business policy (regions/dealer assignment), and deadlines.
Consumes inventory events to update visit state.
Exposes customer API: schedule/confirm/cancel via gRPC.
Notification Service (Side-effect Processor)
Stateless; consumes VisitConfirmed, VisitCanceled, HoldCreated, HoldExpired.
Sends push/SMS/email to customer and dealer.
Fully idempotent with dedup keys (e.g., eventId).