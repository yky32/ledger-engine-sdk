# Integration guide (client engineers)

How to use **ledger-engine-sdk** against a deployed **ledger-engine**.

The SDK is delivered as a **versioned JAR by email** after contract (see [DELIVERY.md](DELIVERY.md)). It is not on Maven Central.

---

## 1. Prerequisites

| Requirement | Notes |
|-------------|--------|
| Java **17+** | Compile and runtime |
| **ledger-engine** reachable | HTTP base URL; Kafka only if you use MQ channel |
| Jackson | `jackson-databind` + `jackson-datatype-jsr310` (e.g. 2.18.x) |
| Optional: Kafka clients | `org.apache.kafka:kafka-clients` only if using `DeliveryChannel.KAFKA` |

---

## 2. Install the delivered jar

### Recommended — install into your Maven repo (local or private)

```bash
mvn install:install-file \
  -Dfile=ledger-engine-sdk-1.1.0.jar \
  -DgroupId=com.altech \
  -DartifactId=ledger-engine-sdk \
  -Dversion=1.1.0 \
  -Dpackaging=jar
```

```xml
<dependency>
  <groupId>com.altech</groupId>
  <artifactId>ledger-engine-sdk</artifactId>
  <version>1.1.0</version>
</dependency>

<!-- Required if not already on the classpath -->
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <version>2.18.2</version>
</dependency>
<dependency>
  <groupId>com.fasterxml.jackson.datatype</groupId>
  <artifactId>jackson-datatype-jsr310</artifactId>
  <version>2.18.2</version>
</dependency>

<!-- Only for Kafka channel -->
<!--
<dependency>
  <groupId>org.apache.kafka</groupId>
  <artifactId>kafka-clients</artifactId>
  <version>3.8.1</version>
</dependency>
-->
```

### Alternative — `lib/` + system path

```xml
<dependency>
  <groupId>com.altech</groupId>
  <artifactId>ledger-engine-sdk</artifactId>
  <version>1.1.0</version>
  <scope>system</scope>
  <systemPath>${project.basedir}/lib/ledger-engine-sdk-1.1.0.jar</systemPath>
</dependency>
```

Verify integrity before install:

```bash
shasum -a 256 -c SHA256SUMS.txt
```

---

## 3. Create a client

```java
import com.altech.ledger.sdk.LedgerClient;
import com.altech.ledger.sdk.LedgerClientConfig;

LedgerClient client = LedgerClient.create(LedgerClientConfig.builder()
    .baseUrl("https://ledger.example.internal")  // your engine base URL
    .bearerToken(System.getenv("LEDGER_TOKEN"))  // if engine requires auth
    // .apiKey("...")
    .defaultCurrency("LP")                       // optional default
    .defaultExternalType("your-system-id")       // optional; applied on onboard
    .build());

// always close when done (or use try-with-resources)
client.close();
```

| Config | Purpose |
|--------|---------|
| `baseUrl` | Engine origin (no trailing slash required) |
| `bearerToken` / `apiKey` | Sent on REST if set |
| `defaultCurrency` | Fills empty currency on wallet onboard |
| `defaultExternalType` | Fills empty `externalType` on wallet onboard |
| `kafkaBootstrapServers` / `kafkaTopic` | Required only for Kafka channel |
| `retryPolicy` / `noRetries()` | REST retries (429 / 5xx / network) |

---

## 4. Phase 1 — wallets

One **owner + currency** maps to one wallet (typical loyalty: one LP wallet per customer).

```java
import com.altech.ledger.sdk.model.OnboardWalletRequest;

client.wallets().onboard(OnboardWalletRequest.builder()
    .userId("CUST-10001")
    .currency("LP")
    .name("Customer 10001")
    .externalId("CUST-10001")
    .externalType("crm")
    .build());

// lookup
var wallet = client.wallets().get("CUST-10001", "LP");
```

Bulk:

- Engine batch: `client.wallets().onboardBatch(list)` — max **1000** per call  
- Per-item results / continue-on-error: `client.wallets().onboardEach(list, BatchOptions.continueOnError())`

Wallets should exist **before** Phase 2 traffic, or events may be skipped.

---

## 5. Phase 2 — transactional events

Agreement object: `TransactionalEvent`.

```java
import com.altech.ledger.sdk.model.TransactionalEvent;
import java.math.BigDecimal;
import java.time.Instant;

var event = TransactionalEvent.builder()
    .eventId("order-20260806-001")   // stable id → also REST Idempotency-Key
    .userId("CUST-10001")
    .eventType("PURCHASE")           // mapped by engine rules
    .amount(new BigDecimal("150.00"))
    .currency("LP")
    .occurredAt(Instant.now())
    .build();

// REST (sync result)
var result = client.events().submit(event);
// result.getStatus(), result.getPoints(), ...

// Kafka (async; needs kafka-clients + bootstrap config)
// client.events().submitKafka(event);
```

Prefer **stable `eventId`** (order id / POS id) so retries are safe.

### Batch / file

```java
import com.altech.ledger.sdk.DeliveryChannel;
import com.altech.ledger.sdk.batch.BatchOptions;
import com.altech.ledger.sdk.batch.ProgressListener;

// in-memory list
client.events().submitBatch(events, DeliveryChannel.REST, BatchOptions.continueOnError());

// NDJSON or JSON array file (streamed; suitable for large files)
client.files().process(
    Path.of("events.ndjson"),
    DeliveryChannel.REST,
    BatchOptions.builder()
        .continueOnError(true)
        .progress(ProgressListener.loggingEvery(500))
        .build());
```

**NDJSON** (one event JSON object per line) or **JSON array** are supported.

---

## 6. Resource API map

| Resource | Main methods |
|----------|----------------|
| `client.wallets()` | `onboard`, `onboardBatch`, `onboardEach`, `get`, `list`, `*Async` |
| `client.events()` | `submit` / `submitRest` / `submitKafka`, `submitBatch`, `submitAsync` |
| `client.files()` | `parse`, `process(path, channel, options)` |

1.0-style shortcuts still work: `onboardWallet`, `ingestRest`, `getWallet`, etc.

Low-level: `client.rest()`, `client.kafka()`, `client.file()`.

---

## 7. Headers the SDK sends (REST)

| Header | When |
|--------|------|
| `Authorization: Bearer …` | `bearerToken` set |
| `X-Api-Key` (configurable name) | `apiKey` set |
| `Idempotency-Key` | Mutating calls — event uses `eventId`; wallet uses `wallet:{userId}:{currency}` |
| `X-Request-Id` | Each attempt (correlation) |

---

## 8. Errors

All failures extend `LedgerException`. Prefer typed catches:

- `LedgerValidationException`, `LedgerNotFoundException`, `LedgerConflictException`
- `LedgerAuthException`, `LedgerRateLimitException`, `LedgerServerException`, `LedgerNetworkException`

Full table and engine JSON shape: [ERRORS.md](ERRORS.md).

---

## 9. Optional CLI (if delivered)

Fat jar classifier `cli` (only if included in your package):

```bash
java -jar ledger-engine-sdk-1.1.0-cli.jar \
  --base-url https://ledger.example.internal \
  --file ./events.ndjson \
  --delivery REST \
  --continue-on-error
```

---

## 10. Compatibility

| SDK | Engine (minimum) |
|-----|------------------|
| 1.0.x / 1.1.x | 1.0.0 |

Engine integration semantics: [ledger-engine INTEGRATION.md](https://github.com/yky32/ledger-engine/blob/main/INTEGRATION.md).
