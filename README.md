# 🌳 Grove: Minimal Self-Hosted npm, PyPI, Maven & OCI Registry (Spring Boot + Kotlin)

Grove is a tiny, filesystem-backed registry for npm, PyPI, Maven, and OCI — ideal for air‑gapped development, CI/CD bootstrapping, or local/private publishing and install.
It is a single Spring Boot app, runnable as a jar, requiring no external database or service.

- npm: Minimal endpoints for publish (via curl) and install (unscoped)
- PyPI: Serves a PEP 503 "simple" index and supports pip install and direct uploads
- Maven: Acts as a simple Maven repository with curl-based uploads and standard resolver compatibility
- OCI: Minimal Docker Distribution (v2) compatible API for blobs, manifests, and tags

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

### OCI (under `/v2`)
- `GET /v2/` — ping (Docker-Distribution-API-Version header)
- `POST /v2/{name}/blobs/uploads/` — start upload or monolithic upload with `?digest=sha256:<hex>`
- `PATCH /v2/{name}/blobs/uploads/{uuid}` — append chunk data
- `PUT /v2/{name}/blobs/uploads/{uuid}?digest=sha256:<hex>` — finalize upload; stores blob by digest
- `HEAD|GET /v2/{name}/blobs/{digest}` — fetch or check blob
- `PUT /v2/{name}/manifests/{reference}` — store manifest by tag or digest
- `HEAD|GET /v2/{name}/manifests/{reference}` — fetch manifest; returns Docker-Content-Digest
- `GET /v2/{name}/tags/list` — list tags for a repository

All routes support multi-segment names (e.g., `myorg/project/image`).

### Storage
- All data is stored under configured roots (defaults under `storage/`)
- npm: per‑package `metadata.json` + tarballs; PyPI: uploaded files; Maven: artifacts + `maven-metadata.xml`; OCI: blobs by digest and manifest/tag files
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
# maven
grove.maven.storage-dir=storage/maven
grove.maven.base-url=http://localhost:8080/maven
# oci (Docker/OCI v2)
grove.oci.storage-dir=storage/oci
grove.oci.base-url=http://localhost:8080/v2
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

### OCI

#### Ping

```bash
curl -i http://localhost:8080/v2/
```

#### Monolithic blob upload (simple example)

```bash
# Prepare a small blob and compute its sha256
printf 'hello' > blob.bin
# Linux: D=$(sha256sum blob.bin | cut -d' ' -f1)
# macOS: D=$(shasum -a 256 blob.bin | cut -d' ' -f1)

# Upload in a single POST by providing the digest query
curl -s -D - \
  -X POST \
  --data-binary @blob.bin \
  "http://localhost:8080/v2/myorg/demo/blobs/uploads/?digest=sha256:$D"
```

#### Put a manifest (tag: latest)

```bash
cat > manifest.json <<EOF
{
  "schemaVersion": 2,
  "mediaType": "application/vnd.oci.image.manifest.v1+json",
  "config": {
    "mediaType": "application/vnd.oci.image.config.v1+json",
    "digest": "sha256:0000000000000000000000000000000000000000000000000000000000000000",
    "size": 2
  },
  "layers": [
    {
      "mediaType": "application/vnd.oci.image.layer.v1.tar",
      "digest": "sha256:$D",
      "size": $(wc -c < blob.bin)
    }
  ]
}
EOF

curl -i \
  -H "Content-Type: application/vnd.oci.image.manifest.v1+json" \
  -X PUT --data-binary @manifest.json \
  http://localhost:8080/v2/myorg/demo/manifests/latest
```

#### Other handy calls

```bash
# List tags
curl http://localhost:8080/v2/myorg/demo/tags/list

# Get manifest by tag
curl -i http://localhost:8080/v2/myorg/demo/manifests/latest

# Get blob by digest
curl -I http://localhost:8080/v2/myorg/demo/blobs/sha256:$D
```

Notes:
- Multi-segment repository names are supported (e.g., myorg/project/image)
- Only sha256 digests are accepted in this MVP
- No auth; suitable for trusted/dev environments

---

## Storage layout

```
storage/
├─ npm/
│  └─ mypkg/
│     ├─ mypkg-1.0.0.tgz
│     └─ metadata.json
├─ pypi/
│  └─ demo-pkg/
│     └─ demo_pkg-0.1.0-py3-none-any.whl
├─ maven/
│  └─ com/example/app/my-app/
│     ├─ 1.0.0/
│     │  ├─ my-app-1.0.0.jar
│     │  └─ my-app-1.0.0.pom
│     └─ maven-metadata.xml
└─ oci/
   ├─ uploads/
   │  └─ {uuid}
   ├─ blobs/
   │  └─ sha256/
   │     └─ {digest-hex}
   └─ repositories/
      └─ myorg/demo/
         └─ manifests/
            ├─ tags/
            │  └─ latest
            └─ digests/
               └─ sha256/
                  └─ {digest-hex}
```

---

## Limitations (MVP)

- No authentication/authorization
- npm: no dist‑tag management beyond auto‑update of `latest`; no scoped packages
- PyPI: simple index only; no metadata API; filenames are stored as uploaded; no hash fragments on links

---

## Development

- Kotlin + Spring Boot (see `ltd.gsio.grove.*` packages)
- Configuration properties: `NpmProps`, `PypiProps`, `MavenProps`, `OciProps` (enabled via `@EnableConfigurationProperties`)
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

See docs/mvp-test-plan.md for background on minimal registries and test coverage for npm, PyPI, Maven, and OCI.