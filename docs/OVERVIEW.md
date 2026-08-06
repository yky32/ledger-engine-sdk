# ledger-engine-sdk — Overview

## What this is

**ledger-engine-sdk** is the Java client library for **[ledger-engine](https://github.com/yky32/ledger-engine)**.

Product backends use it to:

1. Build **agreement objects** (wallets, transactional events)
2. Deliver work into the engine over **REST**, **Kafka**, or **file**
3. Read results / balances without reimplementing HTTP, retries, or error mapping

It is **not** a separate public API product. Contracts and transports live in **one** Maven artifact (`com.altech:ledger-engine-sdk`). There is no `*-api` module.

| Piece | Repository | Role |
|-------|------------|------|
| **Engine** | `ledger-engine` | Server: COA, wallets, journals, rules, balances |
| **SDK** | `ledger-engine-sdk` | Client library + optional file-ingest CLI |

## How clients go live (engine + SDK)

```text
Phase 1  Onboard wallets (CRM / membership → 1 owner + currency = 1 wallet)
Phase 2  Shoot transactional events (POS / order / campaign → rules → balance)
```

Engine applies rules (`AMOUNT` / `FIXED:n` / `RATE:n`, etc.). The SDK only **submits** events and **queries** wallets.

Detail: engine [INTEGRATION.md](https://github.com/yky32/ledger-engine/blob/main/INTEGRATION.md).

## Channels

| Channel | Sync result? | Typical use |
|---------|--------------|-------------|
| **REST** | Yes (`IngestionResult`) | Online path, low/medium volume |
| **Kafka** | No (async on engine) | High volume / decoupled producers |
| **File** | Per-line via REST (or publish via Kafka) | Batch files, backfills, offline export |

## Version pairing

| SDK | Engine (min) | Notes |
|-----|--------------|--------|
| 1.0.x | 1.0.0 | Production baseline |
| 1.1.x | 1.0.0 | Resource API, streaming file, batch options |

SemVer from **1.0.0**. See [CHANGELOG.md](../CHANGELOG.md).

## Distribution

SDK is **not** published to Maven Central or a public registry.

**Delivery = signed contract → versioned JAR emailed to the client.**

See [DELIVERY.md](DELIVERY.md).

## Documentation map

| Doc | Audience |
|-----|----------|
| [DELIVERY.md](DELIVERY.md) | **Altech** — how we package and hand off the JAR |
| [INTEGRATION.md](INTEGRATION.md) | **Client engineers** — how to use the library |
| [ERRORS.md](ERRORS.md) | **Client engineers** — exception types and recovery |
| [../README.md](../README.md) | Quick index + short examples |
| [../CHANGELOG.md](../CHANGELOG.md) | Version history |
