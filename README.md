# ledger-engine-sdk

Java client SDK for **[ledger-engine](https://github.com/yky32/ledger-engine)**.

**First product client: UAfinance** — 1 customer = 1 LP wallet; POS/order events → engine rules → LP balance.

Product backends depend on this library, build the **agreed objects**, and shoot work into the engine over:

1. **REST** (sync result)
2. **Kafka MQ** (async)
3. **File-based batch** (optional)

No separate `*-api` module — contracts + transports live in this single SDK.

| Version | Status |
|---------|--------|
| **`1.0.0`** | Production baseline (UAfinance). SemVer starts here. |

## Maven

```xml
<dependency>
  <groupId>com.altech</groupId>
  <artifactId>ledger-engine-sdk</artifactId>
  <version>1.0.0</version>
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

| SDK | ledger-engine (min) | Notes |
|-----|---------------------|--------|
| 1.0.x | 1.0.0 | UAfinance baseline |

## UAfinance quick start

```java
import com.altech.ledger.sdk.LedgerClient;
import com.altech.ledger.sdk.LedgerClientConfig;
import com.altech.ledger.sdk.model.*;

try (LedgerClient client = LedgerClient.create(LedgerClientConfig.builder()
        .baseUrl("https://ledger.uafinance.internal")
        .bearerToken(System.getenv("LEDGER_TOKEN"))   // optional until engine requires it
        .defaultCurrency("LP")
        .defaultExternalType("uafinance")
        .build())) {

    // Phase 1 — wallet
    client.onboardWallet(OnboardWalletRequest.builder()
        .userId("UAF-10001")
        .name("Member 10001")
        .externalId("UAF-10001")
        .build());

    // Phase 2 — transaction (Idempotency-Key = eventId)
    var result = client.ingestRest(TransactionalEvent.builder()
        .eventId("pos-20260806-001")
        .userId("UAF-10001")
        .eventType("PURCHASE")
        .amount(new java.math.BigDecimal("150.00"))
        .currency("LP")
        .build());
    // result.getStatus() == EARNED, result.getPoints() ...

    var wallet = client.getWallet("UAF-10001", "LP");
}
```

Shorthand:

```java
LedgerClient client = LedgerClient.forUafinance("https://ledger.uafinance.internal");
```

Full snippet: [`examples/uafinance/UafinanceQuickstart.java`](examples/uafinance/UafinanceQuickstart.java)  
Error handling: [`docs/ERRORS.md`](docs/ERRORS.md)

## Channels

| Channel | SDK call | Notes |
|---------|----------|--------|
| **REST** | `client.ingestRest(event)` | Sync; retries 429/5xx; returns `IngestionResult` |
| **Kafka** | `client.ingestKafka(event)` | Needs `kafka-clients` + bootstrap; returns `PublishResult` |
| **File** | `client.ingestFileRest(path)` | NDJSON or JSON array |

## Auth & reliability

```java
LedgerClientConfig.builder()
    .baseUrl(url)
    .bearerToken(token)              // Authorization: Bearer …
    // .apiKey(key)                  // X-Api-Key (header name configurable)
    .defaultHeader("X-Tenant", "uaf")
    .sendIdempotencyKey(true)        // default true
    .sendRequestId(true)             // default true
    // .noRetries()                  // disable for tests
    .build();
```

| Header | Value |
|--------|--------|
| `Idempotency-Key` | Event: `eventId`; wallet: `wallet:{userId}:{currency}` |
| `X-Request-Id` | UUID per attempt |

## Runnable CLI (file ingest)

Thin library JAR is the main artifact. Shaded CLI:

```bash
mvn clean package
java -jar target/ledger-engine-sdk-1.0.0-cli.jar \
  --base-url http://localhost:8080 \
  --file ./events.ndjson \
  --delivery REST \
  --token "$LEDGER_TOKEN"
```

## File formats

**NDJSON**

```json
{"eventId":"e1","userId":"UAF-1","eventType":"PURCHASE","amount":10,"currency":"LP"}
{"eventId":"e2","userId":"UAF-2","eventType":"REDEEM","amount":5,"currency":"LP"}
```

**JSON array**

```json
[
  {"eventId":"e1","userId":"UAF-1","eventType":"PURCHASE","amount":10,"currency":"LP"}
]
```

## Package layout

```text
com.altech.ledger.sdk
├── LedgerClient              # facade (+ forUafinance)
├── LedgerClientConfig
├── LedgerException + typed subclasses
├── error/                    # ApiError, ErrorMapper, RetryPolicy
├── model/                    # agreement objects
├── rest/RestLedgerClient
├── kafka/KafkaLedgerClient, PublishResult
├── file/FileLedgerClient
└── cli/FileIngestCli         # classifier cli JAR
```

## Versioning

- **SemVer from 1.0.0** — `MAJOR.MINOR.PATCH`
- Breaking API / wire contract → major
- New channels / non-breaking APIs → minor
- Fixes → patch
- Pre-1.0 SNAPSHOTs (`0.1`, `0.2`) are obsolete; do not depend on them

See [CHANGELOG.md](CHANGELOG.md).

## Requirements

- Java 17+
- ledger-engine **1.0.0+** reachable (HTTP and/or Kafka enabled)
- Optional: `kafka-clients` for MQ channel
