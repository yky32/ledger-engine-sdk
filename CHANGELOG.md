# Changelog

## 0.2.0-SNAPSHOT — Phase A (UAfinance production harden)

### Added
- Typed exceptions: `LedgerValidationException`, `LedgerNotFoundException`, `LedgerConflictException`,
  `LedgerAuthException`, `LedgerRateLimitException`, `LedgerServerException`, `LedgerNetworkException`
- Engine `ApiError` parsing (`code`, `message`, `path`, `fieldErrors`) via `ErrorMapper`
- REST auth: `bearerToken`, `apiKey` / `apiKeyHeader`, `defaultHeader`
- `Idempotency-Key` and `X-Request-Id` on mutating REST calls
- Retry with full jitter on 429 / 5xx / network (`RetryPolicy`)
- `defaultExternalType` (UAfinance: `uafinance`) and `LedgerClient.forUafinance(baseUrl)`
- Separate connect vs request timeouts
- `PublishResult` for Kafka acks (facade no longer returns raw `RecordMetadata`)
- Docs: `docs/ERRORS.md`, `examples/uafinance/UafinanceQuickstart.java`

### Changed
- Version bump `0.1.0-SNAPSHOT` → `0.2.0-SNAPSHOT`
- `kafka-clients` is **optional** (REST-only consumers do not pull Kafka transitively)
- Main artifact is a **thin** library JAR; shaded CLI is `…-cli.jar` (classifier `cli`)
- Model validation throws `LedgerValidationException`
- `@JsonIgnoreProperties(ignoreUnknown = true)` on agreement objects
- `amount(double)` on `TransactionalEvent.Builder` deprecated

### Migration from 0.1
1. Update dependency version to `0.2.0-SNAPSHOT`
2. If using Kafka, explicitly add `org.apache.kafka:kafka-clients`
3. CLI: use `ledger-engine-sdk-*-cli.jar` (not the plain jar)
4. Catch typed exceptions instead of only `LedgerException` where useful
5. `ingestKafka` now returns `PublishResult` instead of `RecordMetadata`
