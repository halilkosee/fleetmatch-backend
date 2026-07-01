# Database Health Report

## Versioned entities

Optimistic locking was added to entities that can be updated after creation:

- `Company`
- `CompanyDocument`
- `Load`
- `Offer`
- `CompanySubscription`
- `Conversation`
- `Message`
- `Notification`
- `SupportTicket`
- `User`

`GlobalExceptionHandler` now maps optimistic locking failures to `409 Conflict` with code `CONCURRENT_UPDATE`.

## Indexes added

`V40__database_concurrency_and_index_hardening.sql` adds incremental indexes for high-traffic filters and joins:

- Load marketplace search and broker dashboards: status, pickup date, equipment type, broker company, created date.
- Offer workflows: load/status, fleet/status, created date.
- Vehicle filtering: company/active, company/type/active, status.
- Subscription gating: company/active/payment status, end date.
- Company documents: company/review status, company/document type.
- Notifications: user or company unread timelines.
- Audit lookup: actor company and entity timeline.
- Support queue: company/status/date and priority/date.
- Verification code lookups: user/purpose/expiry.

Existing single-column indexes from earlier migrations were preserved and not duplicated.

## Integrity constraints added

The migration adds database-level guards for business rules already enforced by services:

- One active subscription per company.
- One selected or confirmed offer per load.
- No duplicate capability row for the same vehicle.

## Foreign key review

Existing foreign keys cover the reviewed relationships for companies, users, loads, offers, conversations, messages, notifications, support tickets, subscriptions, documents, push deliveries, and vehicle capabilities. No broad cascade change was introduced because delete behavior is not currently exposed as a core workflow, and changing cascades broadly would risk altering business behavior.

## Fetch optimizations

Most relationships were already `LAZY`. `CompanySubscription` company and plan relationships were changed to `LAZY`; the subscription service now uses explicit transaction boundaries so response mapping remains safe with production `open-in-view=false`.

Existing `EntityGraph` usage in messaging repositories was preserved for conversation/message response views.

## Transaction improvements

Subscription write workflows are now transactional, including:

- Plan creation/update.
- Admin assignment of a company subscription.
- User subscription selection after approval.
- Free plan assignment.
- Payment status updates.

Read-heavy methods in company, load, offer, and subscription services now use read-only transaction scope where they materialize lazy relationships into API responses.

Critical load and offer state transitions still use existing pessimistic locks. No automatic retry was added because confirming offers and changing load lifecycle state are not safe to replay blindly.

## Remaining risks

- `Vehicle` and push-device token updates could be candidates for optimistic locking in a later sprint if concurrent admin/device writes become common.
- Some older foreign keys do not define explicit `ON DELETE` behavior. That is acceptable for the current no-orphan posture, but future hard-delete workflows should define cascade/nullification rules per aggregate.
- Partial unique indexes can fail on deployment if production data already violates the intended rule. Production rollout should run a preflight duplicate check before applying `V40`.
