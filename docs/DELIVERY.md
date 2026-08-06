# Client delivery (manual)

There is **no** Maven Central / Nexus / GitHub Packages publish.

Delivery model:

1. Contract signed with the client (e.g. UAfinance)
2. We build a versioned JAR locally
3. We **email the JAR** (and optional CLI + checksums) to the client
4. Client installs into their private repo or `lib/` as they prefer

## What to send (UAfinance / typical)

| File | Size (approx) | Purpose |
|------|----------------|---------|
| **`ledger-engine-sdk-1.1.0.jar`** | ~76 KB | **Main deliverable** — thin library for their backend |
| `ledger-engine-sdk-1.1.0-cli.jar` | ~20 MB | Optional ops tool (file NDJSON ingest) |
| `SHA256SUMS.txt` | tiny | Integrity check after email |
| This note / README excerpt | — | How to put jar on classpath |

Do **not** email only the fat CLI jar as the “SDK” — product code should use the **thin** jar.

## Build pack

```bash
cd ledger-engine-sdk
git checkout v1.1.0   # or main at the agreed tag
mvn clean package

# checksums
cd target
shasum -a 256 ledger-engine-sdk-1.1.0.jar ledger-engine-sdk-1.1.0-cli.jar > SHA256SUMS.txt
```

Email attachments (minimum):

- `ledger-engine-sdk-1.1.0.jar`
- `SHA256SUMS.txt`

Optional:

- `ledger-engine-sdk-1.1.0-cli.jar`
- link or copy of `README.md` + `docs/ERRORS.md` (or PDF export)

## Client: use the thin jar

**Option A — system scope / lib folder**

```xml
<dependency>
  <groupId>com.altech</groupId>
  <artifactId>ledger-engine-sdk</artifactId>
  <version>1.1.0</version>
  <scope>system</scope>
  <systemPath>${project.basedir}/lib/ledger-engine-sdk-1.1.0.jar</systemPath>
</dependency>
<!-- also: jackson-databind + jackson-datatype-jsr310 (Java 17+) -->
```

**Option B — install into their local/private Maven once**

```bash
mvn install:install-file \
  -Dfile=ledger-engine-sdk-1.1.0.jar \
  -DgroupId=com.altech \
  -DartifactId=ledger-engine-sdk \
  -Dversion=1.1.0 \
  -Dpackaging=jar
```

Then normal dependency (no `system` scope).

**Transitive deps the thin jar needs** (client must have these):

| Dependency | Version (bundled build) | Required for |
|------------|-------------------------|--------------|
| `jackson-databind` | 2.18.2 | always |
| `jackson-datatype-jsr310` | 2.18.2 | always |
| `kafka-clients` | 3.8.1 | **only** if they use Kafka channel |

## Versioning on the contract

- Tag Git `v1.1.0` (or agreed version) before building
- Put **version + SHA-256** in the email / contract schedule
- Next release = new jar + new checksum (SemVer: 1.1.1 / 1.2.0 / 2.0.0)

## Email template (short)

```text
Subject: ledger-engine-sdk 1.1.0 — delivery

Attached:
- ledger-engine-sdk-1.1.0.jar   (library)
- SHA256SUMS.txt

SHA-256 (library):
  <paste from SHA256SUMS.txt>

Requires: Java 17+, Jackson 2.18.x as above.
Optional: kafka-clients 3.8.1 if using MQ channel.
Engine: ledger-engine 1.0.0+ 

Docs: README + ERRORS (attached or portal link).
```
