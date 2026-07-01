# Beta Hardening Notes

## WebSocket Authorization Smoke

WebSocket authorization is enforced in `WebSocketConfig` on inbound STOMP frames:

- `CONNECT` requires an `Authorization: Bearer <jwt>` native header.
- `/topic/conversations/{conversationId}` subscriptions require an authenticated user.
- The user must belong to a company, must not be a driver, must pass marketplace verification, and must belong to the broker or fleet company on the conversation.

Manual DEV smoke:

1. Login as an approved broker or fleet user and keep the JWT.
2. Connect to `wss://api-dev.easyfleetmatch.com/ws` with `Authorization: Bearer <jwt>`.
3. Subscribe to a conversation belonging to that user's company. The subscription should succeed.
4. Subscribe to another company's conversation id. The subscription should be rejected with access denied.
5. Repeat with an onboarding `IN_REVIEW` user. Conversation subscription should be rejected.

## Idempotency Proposal

Current offer and load transitions use pessimistic row locks through `findByIdWithLoadForUpdate` and `findByIdForUpdate`, which protects the core state changes from concurrent writes. For beta, avoid a large redesign and add idempotency in a narrow follow-up:

- Accept optional `Idempotency-Key` on mutation endpoints that can be double-clicked or retried by clients:
  - offer select
  - assignment confirm
  - assignment decline
  - load start
  - load deliver
  - load cancel
- Store keys in a small `idempotency_keys` table with: key, actor user id, endpoint/action, request hash, response status, response body reference, created_at, expires_at.
- Enforce uniqueness on `(actor_user_id, action, key)`.
- If the same key and same request hash repeats, return the original result.
- If the same key is reused with a different request hash, return `409 Conflict`.
- Keep the existing pessimistic locks; idempotency should prevent duplicate client retries, not replace transition guards.

## Open-In-View Review

Production profile explicitly sets `spring.jpa.open-in-view=false`. Service methods that build DTOs from lazy relationships should remain transactional or fetch required relationships before returning. Beta tests cover the high-risk transition services, but admin listing/detail endpoints should get follow-up integration tests against a real database profile before production launch.
