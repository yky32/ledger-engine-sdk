# ledger-engine-sdk

Java client SDK for **[ledger-engine](https://github.com/yky32/ledger-engine)**.

Product clients (e.g. UAfinance backend) depend on this library, build the **agreed objects**, and shoot work into the engine over:

1. **REST**
2. **Kafka MQ**
3. **File-based batch** (optional)

No separate `*-api` module — contracts + transports live in this single SDK.

## Maven

```xml
<dependency>
  <groupId>com.altech</groupId>
  <artifactId>ledger-engine-sdk</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Install locally:

```bash
mvn clean install
```

Or publish to your Maven repo / GitHub Packages.

## Runnable JAR (file ingest CLI)

```bash
mvn clean package
java -jar target/ledger-engine-sdk-0.1.0-SNAPSHOT.jar \
  --base-url http://localhost:8080 \
  --file ./events.ndjson \
  --delivery REST
```

## Quick start

### 1) Create client

```java
import com.altech.ledger.sdk.LedgerClient;
import com.altech.ledger.sdk.LedgerClientConfig;

LedgerClient client = LedgerClient.create(
    LedgerClientConfig.builder()
        .baseUrl("https://ledger.uafinance.internal")
        .kafkaBootstrapServers("kafka:9092")          // optional
        .kafkaTopic("ledger.transaction.events")
        .defaultCurrency("LP")
        .build()
);
```

### 2) Phase 1 — wallet for customer (1:1)

```java
import com.altech.ledger.sdk.model.OnboardWalletRequest;

client.onboardWallet(OnboardWalletRequest.builder()
    .userId("UAF-10001")
    .currency("LP")
    .name("Member 10001")
    .externalId("UAF-10001")
    .externalType("uafinance")
    .build());

// bulk (max 1000 per call)
client.onboardWalletsBatch(listOfRequests);
```

### 3) Phase 2 — shoot transaction (3 channels)

**Agreed object:**

```java
import com.altech.ledger.sdk.model.TransactionalEvent;
import java.math.BigDecimal;
import java.time.Instant;

TransactionalEvent event = TransactionalEvent.builder()
    .eventId("pos-20260806-001")
    .userId("UAF-10001")
    .eventType("PURCHASE")   // engine rule → EARN
    .amount(new BigDecimal("150.00"))
    .currency("LP")
    .occurredAt(Instant.now())
    .build();
```

| Channel | SDK call | Engine behaviour |
|---|---|---|
| **1. REST** | `client.ingestRest(event)` | Sync process; returns `IngestionResult` (points, EARNED/…) |
| **2. Kafka** | `client.ingestKafka(event)` | Async; engine consumer on `ledger.transaction.events` |
| **3. File** | `client.ingestFileRest(path)` / `ingestFileKafka(path)` | NDJSON or JSON array of events |

```java
// REST
var result = client.ingestRest(event);
// result.getStatus() == EARNED, result.getPoints() ...

// Kafka
client.ingestKafka(event);

// File (NDJSON)
client.ingestFileRest(Path.of("purchases-2026-08-06.ndjson"));
```

### 4) Engine applies formula → LP wallet

Engine-side rules (not in SDK) map `eventType` → formula (`AMOUNT` / `FIXED:n` / `RATE:n`) and post to wallet account `wallet:{userId}:LP`.

```java
var wallet = client.getWallet("UAF-10001", "LP");
// wallet.getBalance().getBalance()  or account ledgerBalance
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
├── LedgerClient              # facade
├── LedgerClientConfig
├── model/                    # agreement objects
├── rest/RestLedgerClient
├── kafka/KafkaLedgerClient
├── file/FileLedgerClient
└── cli/FileIngestCli         # runnable JAR main
```

## Requirements

- Java 17+
- ledger-engine reachable (HTTP and/or Kafka enabled)

## License / status

`0.1.0-SNAPSHOT` — align versions with your ledger-engine deployment.
