# Architecture Hardening Report

## Scope

This cleanup preserved the approved backend architecture, public APIs, database schema, workflows, and business rules. The work focused on package organization, repository hygiene, JavaDoc for workflow services, and verification.

## Files Moved

Company:

- `company/document/**` moved to `company/documents/**`.

Notification:

- In-app notification persistence moved under `notification/inapp/**`.
- Push device, delivery, and provider classes moved under `notification/push/**`.
- `NotificationType` moved under `notification/event`.
- Notification DTOs and controller stayed in their existing public-facing package areas.

Support:

- Support ticket controllers, ticket entity, ticket repository, and service moved under `support/ticket/**`.
- Support message entity and repository moved under `support/message/**`.
- Support reply template entity and repository moved under `support/template/**`.
- Support category and priority enums moved under `support/category`.
- Support DTOs stayed under `support/dto`.

Tests:

- Push notification tests moved to match the new `notification/push/service` package.

## Dead Code Removed

- No additional application classes were removed.
- A repository-wide scan did not identify code that was safely confirmable as unused without risking behavior changes.
- Previously tracked hygiene artifacts remain removed from git tracking:
  - `.idea/**`
  - `target/**`

## Duplicate Code Removed

- No behavioral duplicate logic was removed in this pass.
- Mapping and validation logic was left intact where extraction would have increased risk or changed service boundaries.

## Package Improvements

- Company document classes now use the plural `documents` package consistently.
- Notification now separates in-app storage, push delivery, and event typing.
- Support now separates ticket, message, template, and category concerns.
- Existing endpoint classes retain their request mappings; no API path changes were made.

## JavaDoc Added

JavaDoc was added only to workflow-level service methods:

- Company approval and rejection workflows.
- In-app notification creation with push fan-out.
- Push notification dispatch fan-out.
- Support ticket open, reply, and close workflows.

## Validation

Commands run successfully:

```bash
./mvnw clean test
./mvnw -DskipTests compile
```

Result:

- 37 tests passed.
- Compile passed.
- No database migration was added or changed in this cleanup.
- No endpoint behavior was intentionally changed.

## Remaining Recommendations

- Add integration tests for admin review and support endpoints after frontend flows settle.
- Consider mapper classes only where response mapping duplication becomes painful.
- Keep package moves incremental from here; avoid large renames once frontend integration starts.
- Add static analysis later for unused code detection, but keep manual review before deletion.
