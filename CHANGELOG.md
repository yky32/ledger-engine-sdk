# Changelog

SemVer from **1.0.0**. Pre-1.0 snapshots (`0.1.0-SNAPSHOT`, `0.2.0-SNAPSHOT`) are historical only and are not published as product versions.

## [1.0.0] — 2026-08-06

First production baseline for **UAfinance** (first product client).

### Included (consolidated from 0.1 / Phase A)

**Contracts & channels**
- Agreement objects: `TransactionalEvent`, wallet onboard DTOs
- Channels: REST, Kafka (optional dep), file (NDJSON / JSON array)
- Facade: `LedgerClient`, `LedgerClient.forUafinance(baseUrl)`
- CLI: shaded JAR classifier `cli` (`FileIngestCli`)

**Production harden (Phase A)**
- Typed exceptions + engine `ApiError` mapping
- REST auth (`bearerToken`, `apiKey`), `Idempotency-Key`, `X-Request-Id`
- Retry with full jitter (`RetryPolicy`)
- Thin library JAR; `kafka-clients` optional
- `defaultExternalType` / `defaultCurrency` (UAfinance: `uafinance` / `LP`)
- Docs: `docs/ERRORS.md`, `examples/uafinance/`

### Compatibility

| Component | Version |
|-----------|---------|
| ledger-engine-sdk | **1.0.0** |
| ledger-engine (recommended min) | **1.0.0** |
| Java | 17+ |

### Maven

```xml
<dependency>
  <groupId>com.altech</groupId>
  <artifactId>ledger-engine-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

---

## Historical (not product versions)

| Tag / version | Notes |
|---------------|--------|
| `0.1.0-SNAPSHOT` | Initial scaffold (REST / Kafka / file) |
| `0.2.0-SNAPSHOT` | Phase A harden (folded into 1.0.0) |
