# Expected contract — upstream SDK → ledger-engine

Upstream **must not** craft raw webhook JSON.  
Call **ledger-engine-sdk** methods; SDK fills the wire body.

---

## 0. Startup

```java
client.verifyEngine();
client.catalog().cacheTtl(Duration.ofMinutes(5));
```

## 1. Who owns what

| Layer | Responsibility |
|-------|----------------|
| **Upstream app** | Business moment (like page, card auth, order paid) + `ownerId` + idempotent `eventId` |
| **SDK** | Build contract, HTTP/Kafka, retries, field names |
| **Engine (ops)** | Door / Brain rules / COA / recipes — **config**, not upstream code |
| **Engine (runtime)** | Admit → score → post → balances → optional Kafka `ledger.balance.updated` |

---

## 2. Preferred upstream code

### 2a. Discover what ops configured (recommended for xapi)

UAF operators set Brain rules + COA in LedgeRX Admin. Upstream **pulls the catalog** then fills `code`:

```java
// Cache periodically (e.g. every 5 min) or on startup
List<UseCaseDescriptor> catalog = client.catalog().listUseCases();
// each: code, name, amountMode (ZERO|SPEND), formulaSummary, coaProfileCode, pointCurrency

UseCaseDescriptor like = client.catalog().get("LIKE_FB_PAGE");
client.useCases().invoke(like, ownerId, eventId, null, null, Map.of("pageId", pageId));

UseCaseDescriptor cc = catalog.stream()
    .filter(u -> "CC_TXN_LP".equals(u.getCode())).findFirst().orElseThrow();
client.useCases().invoke(cc, ownerId, txnId, spendAmount, "HKD", Map.of("mcc", mcc));
```

| amountMode | xapi amount |
|------------|-------------|
| ZERO | `null` or `0` (like page) |
| SPEND | required &gt; 0 |
| ANY | optional |

### 2b. Typed helpers (optional sugar)


```java
try (LedgerClient client = LedgerClient.forUafinance("https://ledger.internal")) {

    // optional CRM onboard — or Door auto-wallet
    client.wallets().onboard(OnboardWalletRequest.builder()
        .userId("CUST-10001")
        .currency("LP")
        .build());

    // Like Facebook page → +5 LP (Brain FIXED rule LIKE_FB_PAGE)
    IngestionResult r = client.useCases().likeFacebookPage(
        "CUST-10001",
        "like-2026-08-20-001",   // unique idempotency key
        "ua-finance-page"
    );

    // Card spend
    client.useCases().ccTxnLp(
        "CUST-10001", "txn-998877",
        new BigDecimal("500.00"), "HKD", "5411");

    // Classic purchase demo
    client.useCases().purchase(
        "CUST-10001", "ord-55",
        new BigDecimal("150"), "HKD", "5411");
}
```

| Method | Engine `eventType` | Typical Brain |
|--------|-------------------|---------------|
| `useCases().likeFacebookPage` | `LIKE_FB_PAGE` | FIXED 5 LP · amount 0 |
| `useCases().followInstagram` | `FOLLOW_IG` | FIXED |
| `useCases().ccTxnLp` | `CC_TXN_LP` | RATE / recipe |
| `useCases().purchase` | `PURCHASE` | RATE |
| `useCases().earn(...)` | any registered code | escape hatch |

---

## 3. Wire contract (SDK → engine) — **do not hand-write**

`POST {baseUrl}/integrations/webhooks/transactions`

```json
{
  "eventId": "string, required, unique (idempotency)",
  "ownerId": "string, required (= wallet ownerId / CRM id)",
  "userId": "string, optional alias of ownerId (SDK sends both)",
  "eventType": "string, required (Brain + COA + recipe key)",
  "amount": "number >= 0 (0 OK for FIXED engagement)",
  "currency": "ISO-ish 2–4 letters (spend ccy; points ccy from rule)",
  "occurredAt": "ISO-8601 instant",
  "metadata": {
    "useCase": "same as eventType (recommended)",
    "mcc": "optional",
    "pageId": "optional",
    "channel": "optional"
  }
}
```

| Field | Required | Notes |
|-------|----------|--------|
| eventId | ✅ | Idempotent movement key seed |
| ownerId | ✅ | 1:1 wallet |
| eventType | ✅ | Must match enabled DigestionRule (+ optional COA code) |
| amount | ✅ | ≥ 0 |
| currency | ✅ | e.g. HKD |
| occurredAt | ✅ | SDK defaults `now` |
| metadata | ○ | String map only |

**Dry-run:** same body → `POST .../transactions/dry-run`

---

## 4. Engine must be configured (ops, not SDK)

For `likeFacebookPage` to credit LP:

1. Door open + auto-wallet (or pre-onboard)
2. Digestion rule:
   - `eventType=LIKE_FB_PAGE`
   - `formula={ "type":"FIXED", "value":5 }`
   - `operation=EARN`
3. Optional: COA profile `code=LIKE_FB_PAGE`
4. Optional: recipe catalog entry (CREDIT_REWARD LP)

Upstream **never** sets points — Brain does.

---

## 5. Response (success shape)

```json
{
  "code": "SYS0000",
  "data": {
    "eventId": "...",
    "status": "EARNED",
    "points": 5.0,
    "movementId": 123,
    "matchedRuleCode": "LIKE_FB_PAGE",
    "eligibilityTrace": [ ... ]
  }
}
```

SDK maps into `IngestionResult`.

---

## 6. Anti-patterns

| ❌ | ✅ |
|----|----|
| Upstream builds custom JSON body | `client.useCases().…` |
| Upstream picks COA segments | Ops COA profile by eventType |
| Upstream computes LP points | Brain formula |
| Random eventType strings | `EventTypes.*` / UseCaseApi methods |
| Reuse eventId for different actions | New eventId per business fact |

---

## 7. Channels

| | |
|--|--|
| Default | REST `useCases()` / `events().submit` |
| High volume | `events().submitKafka` (same payload) |
| Batch file | `files().process` (rows → same contract) |

---

*SDK entry: `LedgerClient.useCases()` · Engine SoT: ledger-engine `docs/BOOKLET.md`*
