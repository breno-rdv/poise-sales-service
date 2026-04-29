# Sales Service Project Map

This document turns ADR 0004 into a concrete implementation map for this repository.

## Purpose

The Sales Service owns the visit and purchase coordination workflow for the sales domain. It is responsible for:

- Managing the `Visit` lifecycle
- Orchestrating the booking saga with Inventory
- Handling dealer confirmation deadlines
- Emitting sales-domain events
- Maintaining a low-latency read model for the BFF and frontend

This service is not a CRUD API. It is a workflow orchestrator built around CQRS, event-driven integration, and compensating transactions.

## Target Architecture

The service should evolve into four main slices:

- `api`: gRPC services and transport mapping
- `application`: command/query handlers, saga orchestration, ports
- `domain`: aggregates, state machine, domain events, business rules
- `infrastructure`: persistence, Kafka integration, scheduling, configuration

Suggested package structure under `src/main/java`:

```text
com.poise.sales
  api
    grpc
      VisitGrpcService.java
      VisitGrpcMapper.java
  application
    command
      ScheduleVisitHandler.java
      ConfirmVisitHandler.java
      CancelVisitHandler.java
      ExpireVisitHandler.java
      CompleteVisitHandler.java
    query
      GetVisitHandler.java
      ListVisitsHandler.java
    saga
      VisitSagaCoordinator.java
      VisitDeadlineScheduler.java
    port
      VisitRepository.java
      VisitReadModelRepository.java
      EventPublisher.java
      ClockPort.java
      IdGenerator.java
  domain
    visit
      Visit.java
      VisitState.java
      VisitId.java
      VisitDeadline.java
      VisitPolicy.java
    event
      VisitScheduled.java
      VisitHoldRequested.java
      VisitTentative.java
      VisitConfirmed.java
      VisitCanceled.java
      VisitExpired.java
      VisitCompleted.java
    command
      ScheduleVisit.java
      ConfirmVisit.java
      CancelVisit.java
    exception
      InvalidVisitTransitionException.java
  infrastructure
    persistence
      entity
        VisitEntity.java
        VisitEventEntity.java
        VisitReadModelEntity.java
      repository
        JpaVisitRepository.java
        JpaVisitReadModelRepository.java
    messaging
      kafka
        SalesCommandProducer.java
        InventoryEventConsumer.java
        SalesEventProducer.java
        DeadLetterPublisher.java
      message
        HoldVehicleCommandMessage.java
        ReleaseHoldCommandMessage.java
        VehicleHoldConfirmedMessage.java
        VehicleHoldFailedMessage.java
        VehicleSoldMessage.java
    scheduler
      QuarkusDeadlineScheduler.java
    config
      KafkaTopicsConfig.java
      DeadlineConfig.java
      GrpcConfig.java
```

## Core Aggregate

The `Visit` aggregate is the write-model center of the service.

Minimum aggregate fields:

- `visitId`
- `customerId`
- `vehicleId`
- `dealerId`
- `proposedTime`
- `state`
- `holdId`
- `confirmationDeadline`
- `cancelReason`
- `correlationId`
- `idempotencyKey`
- `createdAt`
- `updatedAt`

State machine:

```text
DRAFT -> HOLD_PENDING -> TENTATIVE -> CONFIRMED -> COMPLETED
                \            \             \
                 -> CANCELED  -> CANCELED  -> CANCELED
                                              -> EXPIRED
```

The aggregate should enforce transitions through explicit methods:

- `schedule()`
- `markHoldRequested()`
- `markTentative(holdId, deadline)`
- `confirm(dealerId)`
- `cancel(actorId, reason)`
- `expire()`
- `complete()`

Invalid transitions should fail inside the aggregate instead of being handled ad hoc in service code.

## Application Flows

### Schedule Visit

`ScheduleVisitHandler` should:

1. Create a `Visit` in `DRAFT`
2. Persist the aggregate
3. Publish `HoldVehicleCommand` to Kafka
4. Transition to `HOLD_PENDING`
5. Emit `VisitScheduled` and `VisitHoldRequested`

### Inventory Hold Confirmed

`InventoryEventConsumer` should:

1. Consume `VehicleHoldConfirmed`
2. Load the `Visit`
3. Transition `HOLD_PENDING -> TENTATIVE`
4. Start the confirmation deadline timer
5. Emit an event that downstream notification processing can use

### Inventory Hold Failed

`InventoryEventConsumer` should:

1. Consume `VehicleHoldFailed`
2. Load the `Visit`
3. Transition `HOLD_PENDING -> CANCELED`
4. Emit `VisitCanceled`

### Dealer Confirms Visit

`ConfirmVisitHandler` should:

1. Load the `Visit`
2. Transition `TENTATIVE -> CONFIRMED`
3. Cancel any pending expiration timer
4. Emit `VisitConfirmed`

### Cancel Visit

`CancelVisitHandler` should:

1. Load the `Visit`
2. Allow cancellation from `DRAFT`, `HOLD_PENDING`, `TENTATIVE`, or `CONFIRMED`
3. Publish `ReleaseHoldCommand` if a hold exists
4. Transition to `CANCELED`
5. Emit `VisitCanceled`

### Expire Visit

`ExpireVisitHandler` should:

1. Trigger when the confirmation deadline fires
2. Verify the `Visit` is still `TENTATIVE`
3. Publish `ReleaseHoldCommand`
4. Transition to `EXPIRED`
5. Emit `VisitExpired`

### Complete Visit

`CompleteVisitHandler` should:

1. Load the `Visit`
2. Transition `CONFIRMED -> COMPLETED`
3. Emit `VisitCompleted`

## gRPC API

The placeholder `hello.proto` should be replaced by visit-focused contracts.

Suggested proto files:

- `src/main/proto/visit_commands.proto`
- `src/main/proto/visit_queries.proto`

Core RPCs:

- `ScheduleVisit`
- `ConfirmVisit`
- `CancelVisit`
- `GetVisit`
- `ListVisitsByCustomer`
- `ListVisitsByDealer`

Suggested request fields:

- `customer_id`
- `vehicle_id`
- `dealer_id`
- `proposed_time`
- `reason`
- `idempotency_key`
- `correlation_id`

The existing demo service should become `VisitGrpcService`.

## Kafka Integration

Stable topic boundaries:

- `sales.commands`
- `sales.events`
- `inventory.events`

Outbound command messages:

- `HoldVehicleCommand`
- `ReleaseHoldCommand`

Inbound inventory events:

- `VehicleHoldConfirmed`
- `VehicleHoldFailed`
- `VehicleSold`
- `VehicleUnavailable`

Sales-owned events:

- `VisitScheduled`
- `VisitHoldRequested`
- `VisitConfirmed`
- `VisitCanceled`
- `VisitExpired`
- `VisitCompleted`

Each Kafka message should carry:

- `eventId`
- `correlationId`
- `causationId`
- `idempotencyKey`
- `occurredAt`
- `aggregateId`
- `aggregateType`

## Persistence Model

A pragmatic first implementation does not require full event sourcing.

Recommended transactional tables:

- `visits`
- `visit_outbox`
- `processed_messages`

Recommended read-model tables:

- `visit_read_model`
- `vehicle_visit_summary` if needed later

Recommended responsibilities:

- `visits`: current aggregate state
- `visit_outbox`: reliable publication of commands and events
- `processed_messages`: idempotent consumer handling
- `visit_read_model`: low-latency query support for the BFF

If stronger auditability is needed later, add a `visit_state_transitions` table.

## Read Model

The read model should be optimized for user experience and BFF queries, not domain normalization.

Suggested projection fields:

- `visit_id`
- `customer_id`
- `vehicle_id`
- `dealer_id`
- `visit_state`
- `proposed_time`
- `confirmation_deadline`
- `vehicle_status_snapshot`
- `last_inventory_event_type`
- `cancel_reason`
- `updated_at`

Primary read use cases:

- `GetVisit(visitId)`
- list visits by customer
- list visits by dealer
- list pending dealer confirmations
- list visits nearing expiration

## Configuration

Add focused configuration under `src/main/resources/application.properties` for:

- gRPC service settings
- Kafka bootstrap servers
- topic names
- consumer group ids
- deadline duration with default `48h`
- per-region deadline overrides
- retry and backoff policy
- dead-letter topics

Suggested property groups:

- `sales.kafka.*`
- `sales.deadline.*`
- `sales.idempotency.*`

## Testing Strategy

### Unit Tests

- `VisitTest`
- `VisitPolicyTest`
- state transition validation tests

### Application Tests

- `ScheduleVisitHandlerTest`
- `ConfirmVisitHandlerTest`
- `CancelVisitHandlerTest`
- `ExpireVisitHandlerTest`
- `CompleteVisitHandlerTest`

### Messaging Tests

- consumer idempotency tests
- message mapping tests
- outbox publication tests

### Integration Tests

- gRPC contract tests
- Kafka flow tests
- persistence and projection tests

The current `HelloGrpcServiceTest` should be replaced by visit-oriented tests.

## Implementation Slices

Build the system in thin vertical slices:

1. Replace the demo hello gRPC service with visit command/query contracts
2. Implement the `Visit` aggregate and transition rules
3. Add persistence for the `visits` table
4. Implement `ScheduleVisit` and publish `HoldVehicleCommand`
5. Consume `VehicleHoldConfirmed` and `VehicleHoldFailed`
6. Add the deadline scheduler and expiration flow
7. Implement `ConfirmVisit` and `CancelVisit`
8. Add the read model and query RPCs
9. Add outbox and idempotency hardening
10. Add tracing, dead-letter handling, and operational metrics

## Current Repository Gap

Today this repository contains:

- a demo Quarkus gRPC service
- a demo proto definition
- minimal configuration
- local Kafka infrastructure in `docker-compose.yml`

It does not yet contain:

- a `Visit` aggregate
- sales-domain gRPC contracts
- Kafka producers and consumers
- persistence and read-model code
- saga orchestration
- deadline handling
- idempotency and outbox support

This document is the target map for evolving the repository from a scaffold into the Sales Service defined by ADR 0004.
