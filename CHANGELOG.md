## Unreleased

- `UseCaseApi` / `client.useCases()` — like FB, CC txn, purchase; no hand-built JSON
- `EventTypes` constants; `TransactionalEvent` serializes `ownerId` + `userId`
- docs/EXPECTED_CONTRACT.md

# Changelog

SemVer from **1.0.0**. Pre-1.0 snapshots are historical only.

## [1.1.0] — 2026-08-06

Phase B — developer experience for UAfinance (resource API, streaming files, batch partial results).

**Delivery:** manual JAR email after contract — no Maven repository publish.  
Docs: `docs/OVERVIEW.md`, `docs/DELIVERY.md`, `docs/INTEGRATION.md`, `docs/ERRORS.md`.

### Added
- Resource API: `client.wallets()`, `client.events()`, `client.files()`
- `DeliveryChannel` enum (`REST`, `KAFKA`)
- Batch package: `BatchOptions`, `BatchResult`, `ItemResult`, `ItemOutcome`, `ProgressListener`
- `continueOnError` + progress callbacks for event/file/wallet-each batches
- Streaming NDJSON and JSON-array ingest (no full-file `readString`)
- Async helpers: `wallets().onboardAsync`, `events().submitAsync`, `wallets().getAsync`
- CLI: `--continue-on-error`, `--progress-every N`

### Changed
- `FileLedgerClient` parse path streams large files; `processBatch` delegates to `FileApi`
- Convenience `ingestFileRest` / `ingestFileKafka` use streaming `files().process`

### Compatibility
- **1.0.x APIs remain**: `onboardWallet`, `ingestRest`, `rest()`, etc.
- Requires ledger-engine **1.0.0+** (unchanged wire contract)

```xml
<dependency>
  <groupId>com.altech</groupId>
  <artifactId>ledger-engine-sdk</artifactId>
  <version>1.1.0</version>
</dependency>
```

---

## [1.0.0] — 2026-08-06

First production baseline for **UAfinance**.

### Included
- Agreement objects, REST / Kafka / file channels
- Typed exceptions, auth, retries, idempotency (Phase A)
- Thin library JAR; `kafka-clients` optional; CLI classifier `cli`
- `LedgerClient.forUafinance`, docs under `docs/ERRORS.md`

### Compatibility

| Component | Version |
|-----------|---------|
| ledger-engine-sdk | 1.0.0 |
| ledger-engine (recommended min) | 1.0.0 |
| Java | 17+ |

---

## Historical (not product versions)

| Tag / version | Notes |
|---------------|--------|
| `0.1.0-SNAPSHOT` | Initial scaffold |
| `0.2.0-SNAPSHOT` | Phase A harden (folded into 1.0.0) |
