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
Exposes customer API: schedule/confirm/cancel.
Notification Service (Side-effect Processor)
Stateless; consumes VisitConfirmed, VisitCanceled, HoldCreated, HoldExpired.
Sends push/SMS/email to customer and dealer.
Fully idempotent with dedup keys (e.g., eventId).