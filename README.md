# 🌳 Grove: Minimal Self-Hosted npm & PyPI Registry (Spring Boot + Kotlin)

Grove is a tiny, filesystem-backed registry for both npm and PyPI — ideal for air‑gapped development, CI/CD bootstrapping, or local/private publishing and install.
It is a single Spring Boot app, runnable as a jar, requiring no external database or service.

- npm: Minimal endpoints for publish (via curl) and install (unscoped)
- PyPI: Serves a PEP 503 "simple" index and supports pip install and direct uploads

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

## License

Apache 2.0 (see LICENSE)

---

## References

See `project-docs/` for background on minimal PyPI and npm registries.