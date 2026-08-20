# Versioning — SDK ↔ ledger-engine

## Current

| Artifact | Version |
|----------|---------|
| **ledger-engine-sdk** | **1.2.0** (`SdkVersions.VERSION`) |
| **ledger-engine** | 1.0.0+ (product LedgeRX) |

Handshake: `GET /integrations/sdk-info` → `minSdkVersion`, `recommendedSdkVersion`, `features`.

```java
client.verifyEngine(); // throws if this JAR < engine.minSdkVersion
```

## Compatibility matrix

| SDK | Min engine | Notes |
|-----|------------|--------|
| 1.0.x | 1.0.0 | wallets + raw events |
| 1.1.x | 1.0.0 | resource API, file batch |
| **1.2.0** | **1.0.0** with catalog + sdk-info | `catalog()`, `invoke`, dry-run, Result unwrap, richer IngestionResult |

Engine without `/integrations/sdk-info` → `verifyEngine()` fails network/404 — deploy engine **#69+** (catalog) and sdk-info endpoint.

## SemVer policy (SDK)

| Bump | When |
|------|------|
| **MAJOR** | Breaking Java API or wire field rename without alias |
| **MINOR** | New methods / catalog fields (backward compatible) |
| **PATCH** | Bugfix, docs |

## Delivery

1. Tag `v1.2.0`  
2. `mvn clean package` → thin JAR + SHA256  
3. Email client + [QUICKSTART_UAF.md](QUICKSTART_UAF.md)  
4. CHANGELOG entry  

## Idempotency & retries

- Business key: **eventId**  
- Transport: **Idempotency-Key** = eventId (SDK default)  
- Safe to retry network failures with the **same** eventId  

## Feature flags (sdk-info)

| Feature | Meaning |
|---------|---------|
| useCasesCatalog | `GET /integrations/use-cases` |
| webhookDryRun | `POST …/transactions/dry-run` |
| balanceUpdatedKafka | outbound balance events |
| factorSet / postingRecipes / coaTransactionCode | engine capabilities |
