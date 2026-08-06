# Delivery process (Altech internal)

How we ship **ledger-engine-sdk** to a product client.

## Policy

| Rule | Detail |
|------|--------|
| **No public Maven publish** | No Maven Central, no public GitHub Packages, no shared Nexus required |
| **Contract first** | JAR is delivered only after the commercial/legal contract is signed |
| **Manual handoff** | We build a **version-tagged** artifact and **email** it to the client |
| **Client owns install** | They put the JAR on their classpath / private repo; we do not push into their network |

This is intentional: each client relationship is contract-bound; distribution is controlled and auditable.

---

## Roles

| Role | Responsibility |
|------|----------------|
| **Sales / legal** | Contract signed; version/schedule agreed |
| **Engineering (Altech)** | Tag release, build pack, checksums, email package |
| **Client engineering** | Install JAR, wire app, configure engine URL / credentials |

---

## What gets delivered

### Always (minimum pack)

| Artifact | Description |
|----------|-------------|
| `ledger-engine-sdk-<VERSION>.jar` | **Thin library** — main product deliverable |
| `SHA256SUMS.txt` | SHA-256 of the jar(s) for integrity after email |
| Version identity | Git tag `v<VERSION>` and version string in the email body |

### Optional (if agreed in the contract)

| Artifact | Description |
|----------|-------------|
| `ledger-engine-sdk-<VERSION>-cli.jar` | Fat CLI for file-based batch ingest (ops use) |
| Docs bundle | PDF or zip of README + `docs/INTEGRATION.md` + `docs/ERRORS.md` |
| Engine pairing note | “Requires ledger-engine ≥ X.Y.Z” |

### Do not send as the “SDK”

| Artifact | Why not alone |
|----------|----------------|
| Only the **cli** fat jar | Wrong classpath for application code; bloated |
| `target/` with SNAPSHOT tests | Not a release |
| Source tree without agreed tag | Unreproducible |

**Product backends must use the thin jar.**

---

## Release checklist (Altech)

### 1. Freeze version

- [ ] Confirm SemVer (e.g. `1.1.0`) with contract schedule
- [ ] `CHANGELOG.md` updated for that version
- [ ] Code on `main` (or release branch) is the agreed commit

### 2. Tag

```bash
cd ledger-engine-sdk
git checkout main
git pull
git tag -a v1.1.0 -m "ledger-engine-sdk 1.1.0"
# push tag when ready:
# git push origin v1.1.0
```

### 3. Build

```bash
git checkout v1.1.0
mvn clean package
```

Produces under `target/`:

- `ledger-engine-sdk-1.1.0.jar` — **send this**
- `ledger-engine-sdk-1.1.0-cli.jar` — only if optional CLI is in scope

### 4. Checksums

```bash
cd target
shasum -a 256 ledger-engine-sdk-1.1.0.jar > SHA256SUMS.txt
# if including CLI:
# shasum -a 256 ledger-engine-sdk-1.1.0.jar ledger-engine-sdk-1.1.0-cli.jar > SHA256SUMS.txt
cat SHA256SUMS.txt
```

### 5. Email package

**To:** client technical contact (and CC commercial if required)  
**Subject:** `ledger-engine-sdk <VERSION> — delivery`

**Attachments (minimum):**

1. `ledger-engine-sdk-<VERSION>.jar`
2. `SHA256SUMS.txt`

**Body template:**

```text
ledger-engine-sdk <VERSION> delivery
====================================

Contract / reference: <REF>
Git tag: v<VERSION>

Attached:
  - ledger-engine-sdk-<VERSION>.jar     (Java library)
  - SHA256SUMS.txt

SHA-256 (library):
  <paste line from SHA256SUMS.txt>

Requirements:
  - Java 17+
  - Jackson Databind 2.18.x (and jackson-datatype-jsr310)
  - ledger-engine server version >= <ENGINE_MIN>  (see pairing table)

Optional (only if you use Kafka channel in the SDK):
  - org.apache.kafka:kafka-clients 3.8.1

How to install the jar on your side:
  See docs/INTEGRATION.md § "Install the delivered jar"

Integrity:
  shasum -a 256 -c SHA256SUMS.txt
```

### 6. Record

- [ ] Store sent version + SHA-256 + date + contract ref (internal CRM / ticket)
- [ ] Do not re-send a rebuilt jar under the same version if content changed — bump SemVer instead

---

## Client install (summary)

Full steps: [INTEGRATION.md](INTEGRATION.md).

Short form:

```bash
mvn install:install-file \
  -Dfile=ledger-engine-sdk-1.1.0.jar \
  -DgroupId=com.altech \
  -DartifactId=ledger-engine-sdk \
  -Dversion=1.1.0 \
  -Dpackaging=jar
```

Then:

```xml
<dependency>
  <groupId>com.altech</groupId>
  <artifactId>ledger-engine-sdk</artifactId>
  <version>1.1.0</version>
</dependency>
```

---

## Versioning rules (for contracts)

| Change | Version bump |
|--------|----------------|
| Breaking API or wire contract | **MAJOR** (e.g. 2.0.0) |
| New features, backward compatible | **MINOR** (e.g. 1.2.0) |
| Bugfix only | **PATCH** (e.g. 1.1.1) |

- Contract should name the **exact version** (or range, e.g. `1.1.x`) and engine minimum.
- Email always carries **one concrete version** and its **SHA-256**.

---

## What we do *not* do (by default)

- Publish to Maven Central
- Auto-deploy to client artifactory
- Email untagged SNAPSHOT builds as “production SDK”
- Support unsigned “latest” without a version string

Exceptions require an explicit contract amendment.

---

## Related

- [OVERVIEW.md](OVERVIEW.md) — product map
- [INTEGRATION.md](INTEGRATION.md) — client integration
- [CHANGELOG.md](../CHANGELOG.md) — released versions
