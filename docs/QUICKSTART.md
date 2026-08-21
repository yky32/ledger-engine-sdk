# Upstream integrator — 10-minute Quickstart

Integrate **LedgeRX** with only the SDK JAR. No hand-built webhook JSON.

## 1. Install

```xml
<dependency>
  <groupId>com.altech</groupId>
  <artifactId>ledger-engine-sdk</artifactId>
  <version>1.2.0</version>
</dependency>
```

(or `mvn install:install-file` from delivered JAR + SHA256)

## 2. Bootstrap (once)

```java
try (LedgerClient ledger = LedgerClient.forLoyalty(System.getenv("LEDGER_BASE_URL"))
    // or: LedgerClient.forIntegration(baseUrl, "your-partner-id")) {
    // optional: .bearerToken / .apiKey via LedgerClientConfig.builder()

    SdkInfo info = ledger.verifyEngine(); // fail-fast if JAR too old
    ledger.catalog().cacheTtl(Duration.ofMinutes(5));

    // …
}
```

Env:

| | |
|--|--|
| `LEDGER_BASE_URL` | e.g. `https://ledger.uaf.internal` |
| `LEDGER_TOKEN` / API key | if engine auth enabled |

## 3. Discover ops config

Operators configure Brain + COA in LedgeRX Admin. xapi **pulls**:

```java
List<UseCaseDescriptor> catalog = ledger.catalog().listUseCasesCached();
// code, name, amountMode (ZERO|SPEND), formulaSummary, coaProfileCode, …

UseCaseDescriptor like = ledger.catalog().require("LIKE_FB_PAGE");
```

## 4. Business calls

```java
// Like Facebook page → +N LP (Brain FIXED). amountMode=ZERO
IngestionResult r = ledger.useCases().invoke(
    like,
    ownerId,                    // CRM id = wallet ownerId
    "like-" + businessId,       // UNIQUE eventId = idempotency
    null,                       // amount
    null,
    Map.of("pageId", pageId)
);
// r.getPoints(), r.getMovementId(), r.getMatchedRuleCode(), r.getRequestId()

// Dry-run first (no books)
ledger.useCases().invokeDryRun(like, ownerId, "dry-" + id, null, null, Map.of());

// Card spend (amountMode=SPEND)
UseCaseDescriptor cc = ledger.catalog().require("CC_TXN_LP");
ledger.useCases().invoke(cc, ownerId, txnId, amount, "HKD", Map.of("mcc", mcc));
```

## 5. Idempotency (critical)

| | |
|--|--|
| **eventId** | One per business fact (one like, one txn). Never reuse for a different action. |
| **Idempotency-Key** | SDK sends `eventId` automatically on POST. |
| Retry | Same eventId → safe; engine returns DUPLICATE / same result. |

## 6. Read balance

```java
var w = ledger.wallets().get(ownerId, "LP");
// or list
ledger.wallets().list(ownerId);
```

Auto-wallet: if Door `isAutoCreateWallet`, first event can create HKD+LP without onboard.

## 7. What you never do

- Build raw JSON bodies  
- Hardcode LP formulas  
- Choose COA entity/type segments  
- Invent eventType strings not in catalog  

## 8. Support

Quote `requestId` from `IngestionResult` / logs.  
See [ERRORS.md](ERRORS.md), [EXPECTED_CONTRACT.md](EXPECTED_CONTRACT.md), [VERSIONING.md](VERSIONING.md).
