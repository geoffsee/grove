# Grove MVP Test Plan — Minimal Items

Scope: Validate the smallest set of behaviors that prove Grove works as a minimal, filesystem‑backed npm and PyPI registry. Focus on happy paths, basic error handling, URL generation, and storage persistence. Tests assume default config in `src/main/resources/application.properties` unless noted.

## 0) Smoke: Build & Run
- Build starts with Java 21 and the app runs: `./gradlew bootRun`.
- Server listens on `:8080` and responds to `GET /` with 404 (no root route).
- Storage directories are created on demand under `storage/npm` and `storage/pypi`.

## 1) npm (under `/npm`)

### 1.1 Publish package (happy path)
- Request: `PUT /npm/{name}` with `multipart/form-data` fields:
  - `version` (string)
  - `tarball` (file, e.g., `mypkg-1.0.0.tgz`)
- Expect: `200 OK` JSON: `{ ok: true, id: "{name}", rev: "{version}" }`.
- Filesystem:
  - Tarball saved at `storage/npm/{name}/{name}-{version}.tgz`.
  - Metadata JSON at `storage/npm/{name}/metadata.json` exists and is valid JSON.

### 1.2 Metadata retrieval
- Request: `GET /npm/{name}`.
- Expect: `200 OK` JSON document with:
  - `name: "{name}"`.
  - `versions.{version}.dist.tarball` is a fully qualified URL rooted at `grove.npm.base-url`.
  - `dist-tags.latest` equals the most recently published version.

### 1.3 Tarball download
- Request: `GET /npm/{name}/-/{name}-{version}.tgz`.
- Expect: `200 OK`, `Content-Type: application/octet-stream`, `Content-Disposition` attachment with filename, and content length > 0.

### 1.4 Multiple versions
- Publish a second version (e.g., `1.1.0`).
- Expect: metadata `versions` contains both versions; `dist-tags.latest` updates to new version.

### 1.5 Basic errors
- `GET /npm/does-not-exist` → `404 Not Found`.
- `GET /npm/{name}/-/{missing}.tgz` → `404 Not Found`.
- `PUT /npm/{name}` missing `version` or `tarball` → `400 Bad Request` (Spring validation).

### 1.6 Base URL handling
- Change `grove.npm.base-url` to a value with or without trailing slash; republish.
- Expect: generated `dist.tarball` URL contains no duplicate slashes and uses the configured base URL.

## 2) PyPI (under `/pypi`)

### 2.1 Upload package (happy path)
- Request: `POST /pypi/api/upload` with `multipart/form-data` fields:
  - `name` (string, any mix of case/`-`/`_`/`.`)
  - `version` (string)
  - `file` (wheel or sdist; filename used as stored file name)
- Expect: `200 OK` JSON: `{ ok: true, project, version, filename, url }`.
- Filesystem:
  - Project directory uses PEP 503 normalized name: `storage/pypi/{normalized-name}/`.
  - File saved exactly as uploaded under the project directory.

### 2.2 Simple index root
- Request: `GET /pypi/simple`.
- Expect: `200 OK`, `Content-Type: text/html`, anchors for each project using normalized names, e.g., `<a href="/pypi/simple/{normalized}/">`.

### 2.3 Simple project page
- Request: `GET /pypi/simple/{project}/` (accept mixed case or separators in `{project}`).
- Expect: `200 OK`, `Content-Type: text/html`, anchors linking to `/pypi/packages/{normalized}/{filename}` with visible text equal to the filename.

### 2.4 File download
- Request: `GET /pypi/packages/{normalized}/{filename}`.
- Expect: `200 OK`, `Content-Type: application/octet-stream`, `Content-Disposition` attachment with filename, and content length > 0.

### 2.5 Basic errors
- `GET /pypi/packages/{normalized}/missing.whl` → `404 Not Found`.
- `POST /pypi/api/upload` missing any of `name`, `version`, or `file` → `400 Bad Request`.

### 2.6 Name normalization
- Upload with name variants (e.g., `Demo_Pkg`, `demo-pkg`, `demo.pkg`).
- Expect: all map to the same normalized project directory and appear as a single project in the simple index.

### 2.7 Base URL usage in API response
- Set `grove.pypi.base-url` and upload.
- Expect: upload response `url` uses the configured base URL and contains no duplicate slashes.

## 3) Persistence
- Restart the server.
- Expect: previously published npm metadata and tarballs are still readable; PyPI simple index and project pages list previously uploaded files; downloads still work.

## 4) Minimal client checks (manual)
- npm: `npm install {name} --registry http://localhost:8080/npm` against a simple tarball that contains a valid `package.json`.
- pip: `pip install {project} --extra-index-url http://localhost:8080/pypi/simple` using a valid wheel that targets your environment (e.g., a trivial pure‑python wheel). Use `--no-deps` to avoid external indexes.

## 5) Out of scope for MVP
- Authentication/authorization.
- Scoped npm packages and dist‑tag management beyond `latest`.
- PyPI metadata APIs and hash fragments in simple index.
- Registry enable/disable toggles (not enforced by controllers in current code).

## Notes
- Default config in `src/main/resources/application.properties` uses local storage under `storage/` and base URLs on `http://localhost:8080`.
- When changing `storage` paths, re‑run the happy‑path tests for the affected registry to confirm persistence and URL generation.

