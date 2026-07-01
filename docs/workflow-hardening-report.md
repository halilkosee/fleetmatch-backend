# Workflow Hardening Report

## Verified workflows

- Load lifecycle remains constrained to `POSTED -> AWAITING_FLEET_CONFIRMATION -> BOOKED -> IN_TRANSIT -> DELIVERED`, with `CANCELLED` only before transit.
- Offer lifecycle remains constrained to `PENDING -> SELECTED -> CONFIRMED` or `PENDING/SELECTED -> REJECTED/WITHDRAWN`.
- Broker selection still moves the load to `AWAITING_FLEET_CONFIRMATION`.
- Fleet confirmation still moves the load to `BOOKED`.
- Fleet decline still rejects the selected offer, archives the conversation, and returns the load to `POSTED`.
- Load start and delivery still require the confirmed fleet unless the actor is an admin.

## Edge cases fixed

- Marketplace load search now applies the same verified-account guard as paged search.
- Direct load lookup now applies the verified-account guard before returning marketplace data.
- Fleet decline now applies the verified-account guard.
- Load cancellation now verifies ownership before state validation for non-admin users.
- Repeated cancel, confirm, decline, start, and delivery calls avoid duplicate side effects when the workflow is already in the requested terminal or next state.
- A fleet company can no longer create multiple active offers on the same load through different users.

## Race conditions prevented

- Offer creation now locks the load before checking status and active company offers.
- Broker offer selection now locks the load before changing it to `AWAITING_FLEET_CONFIRMATION`.
- Fleet confirm and decline now lock the load before changing load and offer state.
- Existing pessimistic locks for load start, delivery, cancellation, and offer row updates remain in place.
- Database uniqueness from prior hardening still protects against multiple selected/confirmed offers for a load.

## Business rules strengthened

- Duplicate active offers are checked at company level, not only user level.
- Rejected and withdrawn offers remain terminal because only `PENDING` can be selected and only `SELECTED` can be confirmed or declined.
- Completed loads cannot be reverted.
- In-transit and delivered loads cannot be cancelled by normal broker workflow.
- Company isolation is preserved on broker offer viewing, broker offer selection, fleet confirmation, fleet decline, start, delivery, and cancellation.

## Tests added or updated

- Duplicate active company offer rejection.
- Idempotent repeated fleet confirmation after booking.
- Idempotent repeated load cancellation.
- Idempotent repeated load delivery.
- Marketplace search verification guard assertion.
- Existing happy-path offer and load workflow tests were updated for lock-based repository access.

## Remaining risks

- There is no dedicated offer expiration field yet, so expired-offer behavior cannot be enforced without adding a new business concept.
- There is no automated fleet-confirmation timeout job yet. The current manual decline path is safe, but automatic recovery from stale `AWAITING_FLEET_CONFIRMATION` loads remains a future production workflow.
- Admin cancellation still follows the existing service rule that in-transit and delivered loads cannot be cancelled. If operations needs stronger override powers, that should be designed as an explicit admin override workflow with audit reason requirements.
- Notification de-duplication is handled by idempotent service flow for repeated actions, not by a database-level uniqueness rule.
