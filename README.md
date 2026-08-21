# ledger-engine-sdk

Java client library for **[ledger-engine](https://github.com/yky32/ledger-engine)**.

Product systems use this SDK to onboard wallets, submit transactional events, and read balances over **REST**, **Kafka**, or **file** — without reimplementing transport, retries, or error mapping.

| Version | Notes |
|---------|--------|
| **1.2.0** | Catalog + invoke + dry-run + verifyEngine + integrator quickstart |
| 1.1.0 | Resource API, streaming file ingest, batch options |
| 1.0.0 | Auth, typed errors, retries, optional Kafka, thin JAR |

## Documentation

| Doc | For |
|-----|-----|
| **[docs/OVERVIEW.md](docs/OVERVIEW.md)** | Product map, channels, version pairing |
| **[docs/DELIVERY.md](docs/DELIVERY.md)** | **Altech:** contract → build → email JAR (no Maven publish) |
| **[docs/INTEGRATION.md](docs/INTEGRATION.md)** | **Client:** install jar, Phase 1/2, config, API |
| **[docs/QUICKSTART.md](docs/QUICKSTART.md)** | **Upstream 10-min** integrate |
| **[docs/EXPECTED_CONTRACT.md](docs/EXPECTED_CONTRACT.md)** | **Wire + UseCaseApi** — upstream must not hand-build JSON |
| **[docs/VERSIONING.md](docs/VERSIONING.md)** | SDK ↔ engine matrix |
| **[docs/ERRORS.md](docs/ERRORS.md)** | Exception types and recovery |
| **[CHANGELOG.md](CHANGELOG.md)** | Release history |

## Distribution

**Manual delivery only.** After a signed contract, we email a versioned **thin JAR** + SHA-256.  
There is no Maven Central / public registry publish.

See **[docs/DELIVERY.md](docs/DELIVERY.md)** for the full process.

```bash
# Altech build (after git tag)
mvn clean package
# → target/ledger-engine-sdk-<VERSION>.jar   ← main deliverable
# → target/ledger-engine-sdk-<VERSION>-cli.jar  (optional ops CLI)
```

## Quick start (after client installs the jar)

```java
import com.altech.ledger.sdk.LedgerClient;
import com.altech.ledger.sdk.LedgerClientConfig;
import com.altech.ledger.sdk.model.*;

try (LedgerClient client = LedgerClient.create(LedgerClientConfig.builder()
        .baseUrl("https://ledger.example.internal")
        .bearerToken(System.getenv("LEDGER_TOKEN"))
        .defaultCurrency("LP")
        .build())) {

    client.wallets().onboard(OnboardWalletRequest.builder()
        .userId("CUST-10001")
        .currency("LP")
        .externalId("CUST-10001")
        .build());

    // Preferred: use-cases (no hand-built JSON)
    client.useCases().likeFacebookPage("CUST-10001", "like-001", "ua-page");
    client.useCases().ccTxnLp("CUST-10001", "txn-1", new java.math.BigDecimal("500"), "HKD", "5411");

    // Low-level (escape hatch)
    var result = client.events().submit(TransactionalEvent.builder()
        .eventId("order-001")
        .userId("CUST-10001")
        .eventType("PURCHASE")
        .amount(new java.math.BigDecimal("150.00"))
        .currency("LP")
        .build());

    var wallet = client.wallets().get("CUST-10001", "LP");
}
```

Full guide: [docs/INTEGRATION.md](docs/INTEGRATION.md).

## Channels (summary)

| Channel | Entry |
|---------|--------|
| REST | `client.events().submit(event)` |
| Kafka | `client.events().submitKafka(event)` |
| File | `client.files().process(path, channel, options)` |

## Requirements

- Java 17+
- ledger-engine **1.0.0+**
- Jackson 2.18.x on the client classpath  
- Optional: `kafka-clients` for the Kafka channel

## License / status

Commercial distribution under client contract. Version history: [CHANGELOG.md](CHANGELOG.md).
