# Error handling (UAfinance)

All failures extend `com.altech.ledger.sdk.LedgerException`. Prefer catching the **typed** subclasses.

## Exception map

| Exception | HTTP | When | Retry? |
|-----------|------|------|--------|
| `LedgerValidationException` | 400 / 422 | Client `validate()` or engine validation | No |
| `LedgerAuthException` | 401 / 403 | Missing/invalid token or API key | No (fix credentials) |
| `LedgerNotFoundException` | 404 | Wallet / resource missing | No (onboard first) |
| `LedgerConflictException` | 409 | Duplicate / data integrity | No (treat as success if idempotent) |
| `LedgerRateLimitException` | 429 | Throttled; may set `Retry-After` | Yes (SDK auto-retries) |
| `LedgerServerException` | 5xx | Engine / gateway | Yes (SDK auto-retries 500/502/503/504) |
| `LedgerNetworkException` | — | Timeout, DNS, connection reset | Yes (SDK auto-retries) |
| `LedgerException` | other | Fallback | Check `isRetryable()` |

## Engine error body

ledger-engine returns:

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

The SDK parses this into `ApiError` on the exception:

```java
try {
    client.ingestRest(event);
} catch (LedgerValidationException ex) {
    log.warn("code={} fields={} requestId={}",
        ex.getCode(), ex.getFieldErrors(), ex.getRequestId());
} catch (LedgerException ex) {
    log.error("ledger fail http={} code={} requestId={} body={}",
        ex.getHttpStatus(), ex.getCode(), ex.getRequestId(), ex.getBody());
}
```

## Headers the SDK sends

| Header | Purpose |
|--------|---------|
| `Authorization: Bearer …` | When `bearerToken` configured |
| `X-Api-Key` (configurable) | When `apiKey` configured |
| `Idempotency-Key` | Transaction: `eventId`; wallet: `wallet:{userId}:{currency}` |
| `X-Request-Id` | New UUID per attempt (correlation) |

## Safe patterns for UAfinance

```java
// 1) Onboard is idempotent by design on engine (alreadyExists in batch)
client.onboardWallet(OnboardWalletRequest.builder()
    .userId(customerId)
    .externalId(customerId)
    .build()); // defaultExternalType=uafinance from config

// 2) Always use stable eventId from POS / order id
TransactionalEvent.builder()
    .eventId(order.getId()) // enables safe retries + Idempotency-Key
    ...

// 3) 404 wallet → onboard then retry once
try {
    return client.getWallet(userId, "LP");
} catch (LedgerNotFoundException e) {
    client.onboardWallet(...);
    return client.getWallet(userId, "LP");
}
```

## Disable retries (tests / special cases)

```java
LedgerClientConfig.builder()
    .baseUrl(url)
    .noRetries()
    .build();
```
