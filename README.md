# ledger-engine-sdk

Java client SDK for **[ledger-engine](https://github.com/yky32/ledger-engine)**.

**First product client: UAfinance** — 1 customer = 1 LP wallet; POS/order events → engine rules → LP balance.

| Version | Status |
|---------|--------|
| **`1.1.0`** | Phase B DX: resource API, streaming file, batch partial results |
| `1.0.0` | Production baseline (auth, retries, typed errors) |

## Maven

```xml
<dependency>
  <groupId>com.altech</groupId>
  <artifactId>ledger-engine-sdk</artifactId>
  <version>1.1.0</version>
</dependency>

<!-- Only if you use Kafka channel -->
<dependency>
  <groupId>org.apache.kafka</groupId>
  <artifactId>kafka-clients</artifactId>
  <version>3.8.1</version>
</dependency>
```

```bash
mvn clean install
```

### Compatibility

| SDK | ledger-engine (min) |
|-----|---------------------|
| 1.0.x / 1.1.x | 1.0.0 |

## UAfinance quick start (resource API)

```java
import com.altech.ledger.sdk.*;
import com.altech.ledger.sdk.batch.*;
import com.altech.ledger.sdk.model.*;

try (LedgerClient client = LedgerClient.forUafinance("https://ledger.uafinance.internal")) {

    // Phase 1 — wallet
    client.wallets().onboard(OnboardWalletRequest.builder()
        .userId("UAF-10001")
        .name("Member 10001")
        .externalId("UAF-10001")
        .build());

    // Phase 2 — event (REST)
    var result = client.events().submit(TransactionalEvent.builder()
        .eventId("pos-20260806-001")
        .userId("UAF-10001")
        .eventType("PURCHASE")
        .amount(new java.math.BigDecimal("150.00"))
        .currency("LP")
        .build());

    // Large file / 70K-style batch — stream NDJSON, continue on error
    BatchResult<IngestionResult> batch = client.files().process(
        Path.of("purchases.ndjson"),
        DeliveryChannel.REST,
        BatchOptions.builder()
            .continueOnError(true)
            .progress(ProgressListener.loggingEvery(500))
            .build());
    System.out.println(batch); // success / failed / skipped counts
    batch.throwSummaryIfAnyFailed(); // optional

    var wallet = client.wallets().get("UAF-10001", "LP");
}
```

1.0-style shortcuts still work: `client.onboardWallet(...)`, `client.ingestRest(...)`.

Error handling: [`docs/ERRORS.md`](docs/ERRORS.md)  
Example: [`examples/uafinance/UafinanceQuickstart.java`](examples/uafinance/UafinanceQuickstart.java)

## Resource API

| Resource | Methods |
|----------|---------|
| `wallets()` | `onboard`, `onboardBatch`, `onboardEach`, `get`, `list`, `*Async` |
| `events()` | `submit` / `submitRest` / `submitKafka`, `submitBatch`, `submitAsync` |
| `files()` | `parse`, `process(path, channel, BatchOptions)` — **streaming** |

### Batch options

```java
BatchOptions.failFast()              // default — stop on first error
BatchOptions.continueOnError()       // collect per-item ItemResult
BatchOptions.builder()
    .continueOnError(true)
    .progress((i, total, item) -> { ... })
    .engineBatchSize(500)            // wallet chunks (max 1000)
    .build();
```

`BatchResult` exposes `successCount()`, `failureCount()`, `successes()`, `throwIfAnyFailed()`.

## Channels

| Channel | Call | Notes |
|---------|------|--------|
| **REST** | `events().submit(event)` | Sync; retries 429/5xx |
| **Kafka** | `events().submitKafka(event)` | Needs `kafka-clients` |
| **File** | `files().process(path, …)` | NDJSON or JSON array, streamed |

## Auth & reliability (from 1.0)

```java
LedgerClientConfig.builder()
    .baseUrl(url)
    .bearerToken(token)
    .defaultExternalType("uafinance")
    .defaultCurrency("LP")
    .build();
```

| Header | Value |
|--------|--------|
| `Idempotency-Key` | Event: `eventId`; wallet: `wallet:{userId}:{currency}` |
| `X-Request-Id` | UUID per attempt |

## Runnable CLI

```bash
mvn clean package
java -jar target/ledger-engine-sdk-1.1.0-cli.jar \
  --base-url http://localhost:8080 \
  --file ./events.ndjson \
  --delivery REST \
  --continue-on-error \
  --progress-every 500
```

## Package layout

```text
com.altech.ledger.sdk
├── LedgerClient                 # facade
├── api/                         # WalletApi, EventApi, FileApi
├── batch/                       # BatchOptions, BatchResult, ProgressListener
├── error/                       # ApiError, ErrorMapper, RetryPolicy
├── model/                       # agreement objects
├── rest/ · kafka/ · file/ · cli/
```

## Versioning

- SemVer from **1.0.0**
- 1.1.x: non-breaking DX features
- Breaking → 2.0.0

See [CHANGELOG.md](CHANGELOG.md).

## Requirements

- Java 17+
- ledger-engine **1.0.0+**
- Optional: `kafka-clients` for MQ channel
