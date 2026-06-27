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

Local validation has passed for:

- Backend compile
- Happy path E2E
- Negative/security E2E
- WebSocket smoke test

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

## Remaining High-Priority Gaps

### Security Hardening

- Move rate limiting to Redis before multi-instance deployment.
- Add explicit admin UI support for unlocking temporarily locked accounts.
- Consider refresh token/session management when frontend/mobile authentication matures.

### Subscription Lifecycle

- Enforce subscription `endDate`.
- Define expired subscription behavior.
- Add subscription status model if needed.
- Add scheduled notification for subscription expiration.

### Workflow Concurrency

- Add optimistic or pessimistic locking around offer selection and confirmation.
- Prevent double-select or double-confirm race conditions at database level.
- Consider idempotency for selected workflow endpoints.

### Admin Operations Console

The backend still needs operator-grade endpoints for:

- Company suspension
- Verification notes
- Admin load inspection and cancellation
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

1. Security hardening sprint
2. Subscription lifecycle sprint
3. Workflow concurrency sprint
4. Admin operations console sprint
5. Admin dashboard and reporting sprint
6. Production operations sprint
7. Event-driven workflow foundation sprint

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
