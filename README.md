# 🌳 Grove: Minimal Self-Hosted npm, PyPI & Maven Registry (Spring Boot + Kotlin)

Grove is a tiny, filesystem-backed registry for npm, PyPI, and Maven — ideal for air‑gapped development, CI/CD bootstrapping, or local/private publishing and install.
It is a single Spring Boot app, runnable as a jar, requiring no external database or service.

- npm: Minimal endpoints for publish (via curl) and install (unscoped)
- PyPI: Serves a PEP 503 "simple" index and supports pip install and direct uploads
- Maven: Acts as a simple Maven repository with curl-based uploads and standard resolver compatibility

> 🔒 No authentication or token required by default
> 💾 Artifacts are stored on disk — easy to back up, move, or inspect

---

## Features

### npm (under `/npm`)
- `PUT /npm/{package}` — publish a tarball package version (multipart)
- `GET /npm/{package}` — fetch npm metadata (for install)
- `GET /npm/{package}/-/{file}` — download published tarballs

### PyPI (under `/pypi`)
- `GET /pypi/simple` — global package index (PEP 503 HTML)
- `GET /pypi/simple/{project}/` — per‑project HTML page with file links
- `POST /pypi/api/upload` — upload wheels/tar.gz using multipart (`name`, `version`, `file`)
- `GET /pypi/packages/{project}/{filename}` — download stored wheel/sdist

### Maven (under `/maven`)
- `PUT /maven/<path>` — upload any file at a Maven layout path (multipart: `file`)
- `GET /maven/<path>` — download artifacts and metadata
  - Example path layout: `/maven/com/example/app/my-app/1.0.0/my-app-1.0.0.jar`
  - Automatically maintains `maven-metadata.xml` at `groupId/artifactId/`

### Storage
- All data is stored under a configured root (`storage/npm`, `storage/pypi` by default)
- npm keeps per‑package `metadata.json` and tarballs; PyPI stores uploaded files
- No database; just JSON + files

---

## Quick Start

### 1) Build & Run

```bash
./gradlew bootRun
# Or build a jar:
./gradlew bootJar
java -jar build/libs/grove-*.jar
```

Requires Java 21.

### 2) Configure (optional)

Edit `src/main/resources/application.properties` to set public URLs and storage paths.

Example:

```properties
server.port=8080
# npm
grove.npm.storage-dir=storage/npm
grove.npm.base-url=http://localhost:8080/npm
# pypi
grove.pypi.storage-dir=storage/pypi
grove.pypi.base-url=http://localhost:8080/pypi
```


---

## Usage

### Maven

#### Upload an artifact (via curl)

Example for groupId=com.example.app, artifactId=my-app, version=1.0.0

```bash
# Upload JAR
curl -X PUT "http://localhost:8080/maven/com/example/app/my-app/1.0.0/my-app-1.0.0.jar" \
  -F file=@build/libs/my-app-1.0.0.jar

# Upload POM (optional but recommended)
curl -X PUT "http://localhost:8080/maven/com/example/app/my-app/1.0.0/my-app-1.0.0.pom" \
  -F file=@build/publications/maven/pom-default.xml

# (If you publish sources/javadoc jars, upload them too)
```

After any upload, Grove updates `maven-metadata.xml` at `com/example/app/my-app/`.

#### Use from Gradle

```kotlin
repositories {
    maven {
        url = uri("http://localhost:8080/maven")
        // credentials not required in Grove MVP
        isAllowInsecureProtocol = true
    }
}

dependencies {
    implementation("com.example.app:my-app:1.0.0")
}
```

#### Use from Maven

```xml
<repositories>
  <repository>
    <id>grove</id>
    <url>http://localhost:8080/maven</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.example.app</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

### PyPI

#### Upload a package

```bash
curl -X POST http://localhost:8080/pypi/api/upload \
  -F name=demo_pkg \
  -F version=0.1.0 \
  -F file=@dist/demo_pkg-0.1.0-py3-none-any.whl
```

#### Install via pip

```bash
# Keep your main index and add Grove as an extra index
pip install demo-pkg --extra-index-url http://localhost:8080/pypi/simple
```

#### Browse the simple index

- Root: http://localhost:8080/pypi/simple
- Project: http://localhost:8080/pypi/simple/demo-pkg/

---

### npm

#### Publish a package (via curl)

```bash
curl -X PUT http://localhost:8080/npm/mypkg \
  -F version=1.0.0 \
  -F tarball=@mypkg-1.0.0.tgz
```

> Note: This MVP does not implement the full `npm publish` protocol; use the curl form above.

#### Install via npm/yarn

```bash
npm install mypkg --registry http://localhost:8080/npm
```

#### Fetch metadata directly

```bash
curl http://localhost:8080/npm/mypkg
```

---

## Storage layout

```
storage/
├─ npm/
│  └─ mypkg/
│     ├─ mypkg-1.0.0.tgz
│     └─ metadata.json
└─ pypi/
   └─ demo-pkg/
      └─ demo_pkg-0.1.0-py3-none-any.whl
```

---

## Limitations (MVP)

- No authentication/authorization
- npm: no dist‑tag management beyond auto‑update of `latest`; no scoped packages
- PyPI: simple index only; no metadata API; filenames are stored as uploaded; no hash fragments on links

---

## Development

- Kotlin + Spring Boot (see `ltd.gsio.grove.*` packages)
- Configuration properties: `NpmProps`, `PypiProps` (enabled via `@EnableConfigurationProperties`)
- Shared storage helper: `StorageUtil`
- Tests under `src/test`

---

## Legal & Licensing

### License

Grove is released under the **Apache License 2.0** (see [LICENSE](LICENSE) for full text).

In summary, you may:
- ✅ Use Grove commercially
- ✅ Modify Grove for your needs
- ✅ Distribute Grove or derivatives
- ✅ Include Grove in proprietary software

Under the condition that you:
- 📝 Include a copy of the LICENSE and NOTICE
- ⚠️ State significant changes made to Grove
- 📋 Include copyright and license notices from the original

### Contributing

We welcome community contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for:
- How to report issues and submit pull requests
- Code style and testing expectations
- Contributor license agreement (CLAs)
- Recognition and attribution

### Code of Conduct

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). We are committed to providing a welcoming and inclusive environment for all participants.

### Authors and Attribution

- Original authors and contributors are recognized in [AUTHORS](AUTHORS)
- All contributors retain copyright of their work
- See commit history for complete contribution attribution
- Third-party licenses documented in [NOTICE](NOTICE)

### Patent Grant

If you have patents that cover Grove's functionality, the Apache 2.0 license includes an explicit patent grant. See the LICENSE file for details.

### Disclaimer

Grove is provided "AS IS" without warranty. See the [LICENSE](LICENSE) file for complete terms and disclaimers.

### Security & Liability

Grove is provided as-is and is **NOT recommended for production use without hardening**. Known limitations include:

- ⚠️ No authentication or authorization (all endpoints public)
- ⚠️ No HTTPS/SSL enforcement
- ⚠️ No rate limiting
- ⚠️ No input validation hardening
- ⚠️ Artifacts stored unencrypted on disk

**Never expose Grove directly to untrusted networks.** Use behind a reverse proxy, firewall, or VPN.

### Third-Party Notices

Grove depends on third-party libraries. See [NOTICE](NOTICE) for:
- Complete list of dependencies and their licenses
- Attribution information
- License text and compatibility notes

All dependencies are compatible with Apache 2.0.

---

## References

See `project-docs/` for background on minimal PyPI and npm registries.