# Error handling

All SDK failures extend `com.altech.ledger.sdk.LedgerException`. Prefer catching **typed** subclasses when recovering.

## Exception map

| Exception | HTTP | When | Retryable? |
|-----------|------|------|------------|
| `LedgerValidationException` | 400 / 422 | Client `validate()` or engine validation | No |
| `LedgerAuthException` | 401 / 403 | Missing/invalid token or API key | No — fix credentials |
| `LedgerNotFoundException` | 404 | Wallet / resource missing | No — onboard first if needed |
| `LedgerConflictException` | 409 | Duplicate / data integrity | No — often safe to treat as already applied |
| `LedgerRateLimitException` | 429 | Throttled; may include `Retry-After` | Yes — SDK auto-retries |
| `LedgerServerException` | 5xx | Engine / gateway | Yes — SDK auto-retries 500/502/503/504 |
| `LedgerNetworkException` | — | Timeout, DNS, connection reset | Yes — SDK auto-retries |
| `LedgerException` | other | Fallback | Check `isRetryable()` |

## Engine error body

ledger-engine returns JSON shaped as:

```json
{
  "timestamp": "2026-08-06T12:00:00Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "path": "/wallets",
  "fieldErrors": {
    "userId": "must not be blank"
  }
}
```

The SDK maps this to `ApiError` on the exception:

```java
try {
    client.events().submit(event);
} catch (LedgerValidationException ex) {
    log.warn("code={} fields={} requestId={}",
        ex.getCode(), ex.getFieldErrors(), ex.getRequestId());
} catch (LedgerException ex) {
    log.error("ledger fail http={} code={} requestId={}",
        ex.getHttpStatus(), ex.getCode(), ex.getRequestId());
}
```

Useful getters on `LedgerException`:

| Getter | Meaning |
|--------|---------|
| `getHttpStatus()` | HTTP status, or `-1` if not HTTP |
| `getCode()` | Engine / SDK error code |
| `getRequestId()` | Correlation id when present |
| `getBody()` | Raw response body |
| `getApiError()` | Parsed engine error, if JSON matched |
| `isRetryable()` | Hint for application-level retry |

## REST headers related to support

| Header | Purpose |
|--------|---------|
| `Idempotency-Key` | Safe retries of the same business operation |
| `X-Request-Id` | Per-attempt id — quote in support tickets |

## Patterns

```java
// Missing wallet → onboard once, then retry get
try {
    return client.wallets().get(userId, "LP");
} catch (LedgerNotFoundException e) {
    client.wallets().onboard(OnboardWalletRequest.builder()
        .userId(userId)
        .currency("LP")
        .build());
    return client.wallets().get(userId, "LP");
}
```

```java
// Stable eventId for safe REST retries
TransactionalEvent.builder()
    .eventId(order.getId())
    ...
```

```java
// Disable SDK retries (e.g. tests)
LedgerClientConfig.builder()
    .baseUrl(url)
    .noRetries()
    .build();
```

## Batch failures

With `BatchOptions.continueOnError()`, inspect `BatchResult`:

```java
if (batch.hasFailures()) {
    for (var item : batch.failures()) {
        log.error("id={} err={}", item.getId(), item.getError().getMessage());
    }
    batch.throwSummaryIfAnyFailed(); // optional
}
```
