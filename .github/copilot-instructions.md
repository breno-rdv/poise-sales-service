## Service Purpose
- This repository should evolve into the Sales Service for visit and purchase coordination.
- Treat it as a workflow orchestrator, not a CRUD API.
- The service owns the `Visit` lifecycle, booking saga orchestration with Inventory, dealer confirmation deadlines, sales-domain events, and a low-latency read model for BFF/frontend queries.

## Domain Boundaries
- **Inventory Service** is the source of truth for vehicles and availability. It owns vehicle lifecycle and hold/booking commands and publishes availability-related events.
- **Sales Service** owns the `Visit` aggregate and orchestrates the hold -> confirm -> cancel/expire workflow. It must enforce idempotency, deadlines, and business rules.
- **Notification Service** is a side-effect processor. It consumes sales events and must stay idempotent.

## Target Architecture
Prefer these four slices under `src/main/java/com/poise/sales`:

```text
com.poise.sales
  api
    grpc
  application
    command
    query
    saga
    port
  domain
    visit
    event
    command
    exception
  infrastructure
    persistence
    messaging
    scheduler
    config
```

Use this architecture as the default direction for new work. When replacing scaffold/demo code, move toward these slices instead of extending the hello-world structure.

## Current Implementation Status
- Implemented: Quarkus bootstrap, demo gRPC server infrastructure, hello proto stub, local Kafka in `docker-compose.yml`
- Planned: visit aggregate and state machine, saga orchestration, Kafka producers/consumers, persistence, read model, customer-facing gRPC API

## Core Aggregate
The write model centers on the `Visit` aggregate.

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

Model transitions through explicit aggregate methods, not ad hoc service logic:
- `schedule()`
- `markHoldRequested()`
- `markTentative(holdId, deadline)`
- `confirm(dealerId)`
- `cancel(actorId, reason)`
- `expire()`
- `complete()`

Invalid transitions should fail inside the aggregate.

## Application Flows
Implement handlers and consumers around these flows:

### Schedule Visit
1. Create a `Visit` in `DRAFT`
2. Persist it
3. Publish `HoldVehicleCommand`
4. Transition to `HOLD_PENDING`
5. Emit `VisitScheduled` and `VisitHoldRequested`

### Inventory Hold Confirmed
1. Consume `VehicleHoldConfirmed`
2. Load the `Visit`
3. Transition `HOLD_PENDING -> TENTATIVE`
4. Start the confirmation deadline timer
5. Emit a downstream-friendly event

### Inventory Hold Failed
1. Consume `VehicleHoldFailed`
2. Load the `Visit`
3. Transition `HOLD_PENDING -> CANCELED`
4. Emit `VisitCanceled`

### Dealer Confirms Visit
1. Load the `Visit`
2. Transition `TENTATIVE -> CONFIRMED`
3. Cancel any pending expiration timer
4. Emit `VisitConfirmed`

### Cancel Visit
1. Load the `Visit`
2. Allow cancellation from `DRAFT`, `HOLD_PENDING`, `TENTATIVE`, or `CONFIRMED`
3. Publish `ReleaseHoldCommand` if a hold exists
4. Transition to `CANCELED`
5. Emit `VisitCanceled`

### Expire Visit
1. Trigger when the confirmation deadline fires
2. Verify the `Visit` is still `TENTATIVE`
3. Publish `ReleaseHoldCommand`
4. Transition to `EXPIRED`
5. Emit `VisitExpired`

### Complete Visit
1. Load the `Visit`
2. Transition `CONFIRMED -> COMPLETED`
3. Emit `VisitCompleted`

## gRPC Guidance
- Replace the placeholder `hello.proto` with visit-focused contracts.
- Prefer proto files like:
  - `src/main/proto/visit_commands.proto`
  - `src/main/proto/visit_queries.proto`
- Core RPCs should cover:
  - `ScheduleVisit`
  - `ConfirmVisit`
  - `CancelVisit`
  - `GetVisit`
  - `ListVisitsByCustomer`
  - `ListVisitsByDealer`
- Common request fields should include:
  - `customer_id`
  - `vehicle_id`
  - `dealer_id`
  - `proposed_time`
  - `reason`
  - `idempotency_key`
  - `correlation_id`
- The existing demo service should evolve into `VisitGrpcService`.

## Kafka Guidance
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

## Persistence and Read Model
A pragmatic first implementation does **not** require full event sourcing.

Recommended transactional tables:
- `visits`
- `visit_outbox`
- `processed_messages`

Recommended read-model tables:
- `visit_read_model`
- optionally `vehicle_visit_summary` later

Responsibilities:
- `visits`: current aggregate state
- `visit_outbox`: reliable publication of commands/events
- `processed_messages`: idempotent consumer handling
- `visit_read_model`: low-latency query support

If stronger auditability is needed later, add `visit_state_transitions`.

Suggested read-model fields:
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

Primary query use cases:
- `GetVisit(visitId)`
- list visits by customer
- list visits by dealer
- list pending dealer confirmations
- list visits nearing expiration

## Configuration Guidance
Prefer focused configuration in `src/main/resources/application.properties` for:
- gRPC service settings
- Kafka bootstrap servers
- topic names
- consumer group ids
- deadline duration (default `48h`)
- per-region deadline overrides
- retry and backoff policy
- dead-letter topics

Suggested property groups:
- `sales.kafka.*`
- `sales.deadline.*`
- `sales.idempotency.*`

## Testing Guidance
When replacing demo behavior, also replace demo tests with visit-oriented tests.

Expected coverage:
- Unit: `VisitTest`, `VisitPolicyTest`, transition validation
- Application: schedule, confirm, cancel, expire, complete handlers
- Messaging: idempotency, message mapping, outbox publication
- Integration: gRPC contracts, Kafka flows, persistence, projections

The current `HelloGrpcServiceTest` is a placeholder and should eventually be replaced.

## Delivery Approach
Build the system in thin vertical slices:
1. Replace hello gRPC contracts with visit command/query contracts (implemented)
2. Implement the `Visit` aggregate and transition rules (Implemented)
3. Add persistence for `visits` (Implemented)
4. Implement `ScheduleVisit` and publish `HoldVehicleCommand` (implemented)
5. Consume `VehicleHoldConfirmed` and `VehicleHoldFailed`
6. Add deadline scheduling and expiration flow
7. Implement `ConfirmVisit` and `CancelVisit`
8. Add the read model and query RPCs
9. Add outbox and idempotency hardening
10. Add tracing, dead-letter handling, and operational metrics

## Working Rules for Copilot
- Prefer extending the service toward the target architecture above instead of reinforcing demo scaffolding.
- Keep business rules in the domain/application layers, not in gRPC transport classes.
- Enforce visit transition rules inside the aggregate.
- Design messaging and consumers with idempotency in mind from the start.
- Favor outbox-style reliability when adding command/event publication.
- Optimize query/read-model code for BFF and frontend latency, not strict domain normalization.
- When touching placeholders like `hello.proto` or `HelloGrpcServiceTest`, treat them as migration points toward visit-focused contracts and tests.
