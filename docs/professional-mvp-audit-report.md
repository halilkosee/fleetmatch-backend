# EasyFleetMatch Professional MVP Audit Report

Last updated: 2026-06-27

## Current State

EasyFleetMatch backend has a working MVP core:

- Authentication and JWT security
- Email and phone OTP verification
- Account and company settings
- Subscription limits
- Load and offer lifecycle
- Messaging with REST and WebSocket/STOMP
- Notifications
- Admin dashboard
- Audit log module
- Local E2E and negative/security E2E scripts
- DevOps profile, Docker, Nginx, and CI foundation

The current architecture is still a good fit for the product stage:

- Controllers expose REST and WebSocket endpoints.
- Services own business rules and transactions.
- Repositories stay persistence-focused.
- DTOs keep entities from being exposed directly.
- Flyway owns schema changes.

No large redesign is recommended before beta. The next architectural improvement should be incremental event publishing for major business actions, not a rewrite.

Local validation has passed for:

- Backend compile
- Happy path E2E
- Negative/security E2E
- WebSocket smoke test

Latest local compile result:

- `./mvnw -q -DskipTests compile` passed after the latest subscription and workflow locking changes.
- Full test execution is currently environment-blocked in this sandbox because Maven cannot write to the host `.m2` cache, and the isolated workspace Maven cache cannot download missing artifacts without network access.

## Module Review Summary

### Authentication And Verification

Implemented:

- JWT login.
- Email OTP verification.
- Phone OTP verification.
- Password reset OTP.
- Strong password policy.
- Failed login counter and temporary account lock.
- Token invalidation after credential changes.

Remaining risks:

- Account unlock is not yet exposed as an admin operation.
- Refresh token/session management is not implemented yet.
- Rate limits are in-memory and must move to Redis before multi-instance DEV/PROD deployment.

### Account Settings

Implemented:

- Current user profile endpoint.
- First name and last name update.
- Password change with current password.
- Email change by OTP.
- Phone change by OTP.

Remaining risks:

- User-facing settings UI is frontend work.
- Admin support view for account verification state is still thin.

### Company Settings And Company Users

Implemented:

- Company self-service settings for owner users.
- Locked legal identity fields.
- Company user create, activate, deactivate, and role change.
- Self-deactivation and final-owner protections.
- Company user subscription limit enforcement.

Remaining risks:

- Company suspension is not modeled yet.
- Verification notes and document review outcomes are not operator-grade yet.

### Subscription

Implemented:

- Subscription plans.
- Company subscription assignment.
- Limits for vehicles, users, monthly loads, visible loads, and offers where applicable.
- Expired, future-dated, and inactive-plan subscriptions are now rejected for active business actions.

Remaining risks:

- No scheduled expiration notification job yet.
- No explicit subscription status enum yet.
- No billing provider integration yet.

### Load And Offer Workflow

Implemented:

- DAT-aligned lifecycle.
- Conversation starts after fleet selection.
- Fleet confirm moves load to booked.
- Fleet decline returns load to posted and archives conversation.
- Remaining offers are rejected after confirmation.
- Critical workflow transitions now use row locks.

Remaining risks:

- Idempotency keys are not implemented yet.
- Admin override/inspection endpoints for loads and offers are incomplete.
- Cancellation reason taxonomy exists only at a basic product level.

### Messaging

Implemented:

- REST messaging.
- WebSocket/STOMP endpoint.
- Conversation access checks.
- Archived conversation send prevention.
- Message soft delete.
- Read status and unread counts.
- Notifications for new messages.

Remaining risks:

- WebSocket authorization should be retested after every deployment.
- Future driver accounts must be blocked at both REST and WebSocket layers when that module is introduced.

### Notifications

Implemented:

- Notification entity, repository, service, controller.
- User/company scoped reads.
- Unread count.
- Mark one/read all.
- Notifications for core load, offer, message, company, and subscription events.

Remaining risks:

- Notification preferences do not exist yet.
- Broadcast notifications are not implemented.
- Subscription expiring notification requires a scheduled job.

### Audit Log

Implemented:

- Audit log entity, repository, service, admin endpoint.
- Action, entity, actor, company, IP, and date-range filters.
- Logs for core security, company, subscription, load, offer, messaging, and vehicle actions.

Important product decision:

- Audit log should track meaningful business actions, not every raw database column update.
- If every database mutation must be tracked later, add a separate entity-history/change-data-capture mechanism. Do not overload the operator audit log with low-level noise.

Remaining risks:

- Admin audit screen is frontend work, but the backend endpoint exists.
- Some future operator actions do not exist yet, so they cannot be audited yet.
- Audit details are human-readable strings; later beta may need structured JSON details for easier filtering.

### Dashboard

Implemented:

- Broker dashboard endpoint.
- Fleet dashboard endpoint.
- Basic admin dashboard endpoint.

Remaining risks:

- Admin dashboard needs production KPIs: revenue, growth, active subscriptions, pending verification volume, cancellation rate, delivered load volume.
- Dashboard query tests should be added before beta sign-off.

### Admin Operations

Implemented:

- User approve/suspend.
- Company approve/reject.
- Company document listing.
- Company listing/detail.
- Audit log search.
- Basic dashboard.
- Subscription administration.

Remaining risks:

- Admin console is not yet "no SQL required."
- Missing operator endpoints are listed below under Admin Operations Console.

### Production Readiness

Implemented:

- Profile separation.
- Docker and compose foundation.
- Nginx templates.
- GitHub Actions build foundation.
- Global API error response.
- DB indexes through migrations.

Remaining risks:

- No monitoring/metrics stack yet.
- No backup/restore runbook execution proof yet.
- No request correlation id yet.
- `open-in-view` should be reviewed before production.

## Audit Coverage Sprint

The first maturity sprint focuses on audit coverage and operator traceability.

### Added Audit Actions

- `USER_APPROVED`
- `USER_SUSPENDED`
- `COMPANY_USER_CREATED`
- `COMPANY_USER_ACTIVATED`
- `COMPANY_USER_DEACTIVATED`
- `COMPANY_USER_ROLE_CHANGED`
- `SUBSCRIPTION_PLAN_CREATED`
- `SUBSCRIPTION_PLAN_UPDATED`
- `LOAD_DUPLICATED`
- `OFFER_CONFIRMED`
- `MESSAGE_SENT`
- `MESSAGE_READ`
- `MESSAGE_DELETED`
- `VEHICLE_CREATED`
- `VEHICLE_UPDATED`
- `VEHICLE_DELETED`

### Improved Audit Coverage

The following actions now create audit logs:

- Admin user approval and suspension
- Admin company approval and rejection with actor user
- Company user create, activate, deactivate, and role change
- Vehicle create, update, and soft delete
- Load duplicate
- Offer confirmation
- Pending offer rejections caused by another fleet being confirmed
- Message send, read, and soft delete
- Subscription plan create and update
- Subscription assignment/change with admin actor

### Audit Search Improvements

`GET /api/admin/audit-logs` now supports:

- `action`
- `entityType`
- `entityId`
- `actorEmail`
- `actorCompanyId`
- `from`
- `to`
- pagination and sorting

This is the backend foundation for an Admin Audit Logs screen.

## Security Fixes Included

- Removed `/api/auth/debug/password`.
- Restricted `/api/v1/admin/subscriptions/**` to ADMIN users.
- Protected legacy `/api/company/profile` so MC and DOT numbers cannot be directly changed by company users.
- Legacy company profile update now requires company OWNER role.
- Added failed login tracking and temporary account lock after repeated failures.
- Added JWT invalidation for tokens issued before password or email credential changes.
- Added suspended-user guard for authenticated JWT requests.

## Security Hardening Sprint

### Added User Security Fields

- `failedLoginAttempts`
- `lockedUntil`
- `credentialsChangedAt`

### Login Protection

- Five failed login attempts temporarily lock the account for 15 minutes.
- Successful login resets failed attempts and lock state.
- Password reset and password change reset failed login state.

### Token Hardening

- JWT authentication now checks the latest user status from the database.
- Suspended users cannot continue using previously issued JWTs.
- Tokens issued before `credentialsChangedAt` are rejected.
- Password change, password reset, and verified email change update `credentialsChangedAt`.

## Subscription Lifecycle Sprint

### Added Subscription Usability Checks

- Active business actions now reject expired subscriptions.
- Future-dated subscriptions are not usable before their start date.
- Inactive subscription plans are not usable even if a company subscription row is active.
- Existing subscription limit logic remains centralized in `SubscriptionAccessService`.

## Workflow Concurrency Sprint

### Added Critical Row Locking

- Offer selection, confirmation, and decline now use pessimistic locking on the offer/load row path.
- Load start, delivery, and cancellation now run inside transactions and lock the load row before status validation.
- This reduces double-select, double-confirm, and conflicting status transition risk without changing public APIs.

## Admin Operations Sprint

### Added Operator Actions

- Admins can unlock temporarily locked user accounts.
- Admins can suspend companies.
- Admins can reactivate suspended companies.
- Company suspension stores optional verification/operation notes.
- Company suspension and reactivation create notifications for the affected company.
- User unlock, company suspension, and company reactivation create audit logs.
- Admins can inspect, filter, and cancel loads from operator endpoints.

### Product Behavior

- Suspended companies are no longer `APPROVED`.
- Existing core workflow checks already require company approval before load creation and offer submission.
- This means suspended companies are blocked from active marketplace operations without changing the public load or offer APIs.

## Controlled Marketplace Onboarding Sprint

### Added Onboarding Foundation

- New account lifecycle statuses model registration, verification, documents, review, approval, rejection, suspension, and activation.
- Login now supports authenticated onboarding screens before marketplace approval.
- Marketplace access guard now requires email verification, phone verification when applicable, approved company status, and approved or active user status.
- Dashboard, load, offer, messaging, and WebSocket marketplace paths remain blocked before approval.
- Added onboarding progress endpoint for the verification progress screen.
- Added market survey storage with JSONB analytics fields for operating states, equipment types, regions, tools, challenges, and future integration interest.
- Added submit-for-review flow that moves complete onboarding records into `IN_REVIEW` / `UNDER_REVIEW`.

## Admin Review And Launch Operations Sprint

### Added Operational Control

- Added approval queue endpoints for manual review waves.
- Added admin company review detail with company profile, documents, survey, notes, priority, rejection reason, and additional document requests.
- Added request-additional-documents flow with notification, email template trigger, and audit log.
- Added rejection reason support for company review.
- Added admin internal notes and manual priority support.
- Added per-document review status and admin document review endpoint.
- Added company review history timeline for submissions, approvals, rejections, document review, notes, suspension, and reactivation.
- Added configurable email template storage and admin CRUD endpoints.
- Added subscription plan browsing and selection after company approval.
- Added admin offer inspection and cancellation.
- Added admin conversation inspection and admin message review.
- Added onboarding analytics for company mix, review volume, operating states, equipment, load boards, and TMS usage.
- Added request correlation id response header and MDC value.
- Added Redis-backed rate limiting option with in-memory fallback.
- Added Prometheus metrics exposure.

## Real User Launch Hardening

### Added

- Registration no longer creates an active subscription automatically; users must be approved and then select a plan.
- Paid plan selection now creates a `PENDING_PAYMENT` subscription and does not unlock marketplace usage until payment status is activated.
- Added admin subscription payment-status endpoint for manual invoice/payment operations and future payment webhooks.
- Added subscription payment status model for `PENDING_PAYMENT`, `ACTIVE`, `TRIALING`, `PAST_DUE`, `CANCELED`, and `EXPIRED`.
- Added multipart company document upload with local secure storage, file size/type validation, and authenticated download endpoint.
- Added document storage metadata for storage key, original file name, content type, and file size.
- Added SMTP email provider option with local logging fallback.
- Added Twilio SMS provider and webhook-based SMS provider options with local logging fallback.
- Added production readiness validation so PROD fails fast without real mail, real SMS, Redis rate limiting, and a strong JWT secret.
- Added test coverage for payment-status subscription gating.

## Remaining High-Priority Gaps

### Security Hardening

- Configure `RATE_LIMIT_STORE=redis` before PROD; the readiness validator now enforces this.
- Add explicit admin UI support for unlocking temporarily locked accounts.
- Consider refresh token/session management when frontend/mobile authentication matures.

### Subscription Lifecycle

- Connect Stripe or the chosen payment provider to the new subscription payment-status model.
- Add scheduled notification for subscription expiration.

### Document Storage

- Move local document storage to S3/GCS or a managed private object store before multi-instance production.
- Add malware scanning if user-uploaded documents are exposed to staff workstations.

### Workflow Concurrency

- Consider idempotency for selected workflow endpoints.

### Admin Operations Console

The backend still needs operator-grade endpoints for:

- Admin offer inspection and cancellation
- Conversation inspection
- Broadcast notifications
- Coupons
- Campaigns
- Email templates
- Feature flags
- System settings
- Support tools

### Reporting And Dashboards

- Admin dashboard needs revenue, growth, subscription, verification, and operational metrics.
- Add query-level tests for dashboard KPI correctness.

### Production Operations

- Add request/correlation id.
- Add structured application logs.
- Review `spring.jpa.open-in-view` and close it after lazy-loading impacts are handled.
- Add metrics and monitoring.
- Add backup/restore runbook for DEV and PROD databases.

## Recommended Next Implementation Order

1. Admin operations console sprint
2. Admin dashboard and reporting sprint
3. Redis-backed rate limiting sprint
4. Production operations sprint
5. Subscription expiration notification sprint
6. Idempotent workflow endpoints sprint
7. Event-driven workflow foundation sprint

## Next Backend Sprint Recommendation

Start with Admin Operations Console.

Reason:

- The MVP workflow is working.
- The audit and notification foundations exist.
- The biggest beta-readiness gap is operator control.
- The platform should not require direct SQL for normal support tasks.

Recommended scope for the next sprint:

- Add company suspend/reactivate endpoints.
- Add admin account unlock endpoint.
- Add admin offer inspection endpoint.
- Add admin conversation inspection endpoint.
- Add verification notes for company approval/rejection.
- Ensure each operator action writes an audit log.

Keep this sprint backend-only. The frontend can consume these endpoints later for the Admin Console.

## Principle

Prefer business audit over raw database audit.

The platform should log meaningful business events that operators can understand:

- who acted
- which company they represented
- what action happened
- which entity changed
- when it happened
- from which IP address

Raw column-level history can be added later if regulatory or enterprise customer requirements demand it.
