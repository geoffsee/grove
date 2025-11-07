// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2024 Grove Contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ltd.gsio.grove.oci

import ltd.gsio.grove.config.OciProps
import ltd.gsio.grove.util.StorageUtil
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class OciService(
    private val props: OciProps,
    private val storage: StorageUtil
) {
    private val base: Path get() = props.storageDir

    fun ensureBase() {
        storage.ensureDir(base)
        storage.ensureDir(base.resolve("blobs"))
        storage.ensureDir(base.resolve("uploads"))
        storage.ensureDir(base.resolve("repositories"))
    }

    fun createUpload(): String {
        ensureBase()
        val uuid = UUID.randomUUID().toString()
        val path = base.resolve("uploads").resolve(uuid)
        storage.ensureDir(path.parent)
        Files.createFile(path)
        return uuid
    }

    fun uploadPath(uuid: String): Path = base.resolve("uploads").resolve(uuid)

    fun appendToUpload(uuid: String, input: InputStream): Long {
        val path = uploadPath(uuid)
        Files.newOutputStream(path, java.nio.file.StandardOpenOption.APPEND).use { out ->
            input.copyTo(out)
        }
        return Files.size(path)
    }

    fun finalizeUpload(uuid: String, putBody: InputStream?, digest: String): String {
        val algoHex = parseDigest(digest)
        val path = uploadPath(uuid)
        if (!Files.exists(path)) throw IllegalArgumentException("upload not found")
        if (putBody != null) {
            appendToUpload(uuid, putBody)
        }
        val hex = storage.sha256(path)
        if (algoHex.first != "sha256" || algoHex.second != hex) {
            throw IllegalArgumentException("digest mismatch")
        }
        val dest = blobPathFor(hex)
        storage.ensureDir(dest.parent)
        Files.move(path, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        return "sha256:$hex"
    }

    fun blobPath(digest: String): Path? {
        val (algo, hex) = parseDigest(digest)
        if (algo != "sha256") return null
        val p = blobPathFor(hex)
        return if (Files.exists(p)) p else null
    }

    private fun blobPathFor(hex: String): Path = base.resolve("blobs").resolve("sha256").resolve(hex)

    private fun parseDigest(d: String): Pair<String, String> {
        val parts = d.split(":", limit = 2)
        if (parts.size != 2) throw IllegalArgumentException("invalid digest")
        return parts[0] to parts[1]
    }

    fun putManifest(repo: String, reference: String, bytes: ByteArray): String {
        ensureBase()
        val hex = storage.digestHex(bytes.inputStream(), "SHA-256")
        val digest = "sha256:$hex"
        val repoDir = base.resolve("repositories").resolve(repo)
        val tagDir = repoDir.resolve("manifests").resolve("tags")
        val digDir = repoDir.resolve("manifests").resolve("digests").resolve("sha256")
        storage.ensureDir(tagDir)
        storage.ensureDir(digDir)
        // write tag ref
        Files.write(tagDir.resolve(reference), bytes)
        // write by digest
        Files.write(digDir.resolve(hex), bytes)
        return digest
    }

    fun manifestPath(repo: String, reference: String): Path? {
        val repoDir = base.resolve("repositories").resolve(repo)
        val tagPath = repoDir.resolve("manifests").resolve("tags").resolve(reference)
        if (Files.exists(tagPath)) return tagPath
        // by digest
        val ok = kotlin.runCatching { parseDigest(reference) }.getOrNull()
        if (ok != null && ok.first == "sha256") {
            val p = repoDir.resolve("manifests").resolve("digests").resolve("sha256").resolve(ok.second)
            if (Files.exists(p)) return p
        }
        return null
    }

    fun manifestDigest(repo: String, reference: String): String? {
        val p = manifestPath(repo, reference) ?: return null
        val hex = storage.sha256(p)
        return "sha256:$hex"
    }

    fun listTags(repo: String): List<String> {
        val tagDir = base.resolve("repositories").resolve(repo).resolve("manifests").resolve("tags")
        if (!Files.exists(tagDir)) return emptyList()
        Files.newDirectoryStream(tagDir).use { ds ->
            return ds.filter { Files.isRegularFile(it) }.map { it.fileName.toString() }.sorted()
        }
    }
}
