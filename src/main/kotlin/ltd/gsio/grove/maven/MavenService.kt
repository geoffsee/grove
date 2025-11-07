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

package ltd.gsio.grove.maven

import ltd.gsio.grove.config.MavenProps
import ltd.gsio.grove.util.StorageUtil
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path

@Service
class MavenService(
    private val props: MavenProps,
    private val storage: StorageUtil
) {
    private fun safeResolve(rel: String): Path {
        val target = props.storageDir.resolve(rel).normalize()
        if (!target.startsWith(props.storageDir.normalize())) {
            throw IllegalArgumentException("Invalid path")
        }
        return target
    }

    fun filePath(rel: String): Path? {
        val p = safeResolve(rel)
        return if (Files.exists(p) && Files.isRegularFile(p)) p else null
    }

    fun upload(rel: String, file: MultipartFile): Map<String, Any> {
        val dest = safeResolve(rel)
        storage.ensureDir(dest.parent)
        storage.saveMultipart(dest, file)
        // Try to update maven-metadata.xml for artifact if layout matches group/artifact/version/file
        updateMetadataIfApplicable(rel)
        val url = normalizeUrl("${props.baseUrl}/$rel")
        return mapOf("ok" to true, "path" to rel, "url" to url)
    }

    private fun updateMetadataIfApplicable(rel: String) {
        val parts = rel.split('/').filter { it.isNotBlank() }
        if (parts.size < 4) return // need at least group/.../artifact/version/file
        val artifact = parts[parts.size - 3]
        val groupPathParts = parts.subList(0, parts.size - 3)
        if (groupPathParts.isEmpty()) return
        val artifactDir = props.storageDir.resolve((groupPathParts + artifact).joinToString("/"))
        val groupId = groupPathParts.joinToString(".")
        if (!Files.exists(artifactDir)) return
        val versions = Files.list(artifactDir).use { s ->
            s.filter { Files.isDirectory(it) }
                .map { it.fileName.toString() }
                .sorted()
                .toList()
        }
        if (versions.isEmpty()) return
        val latest = versions.last()
        val ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(java.time.ZoneOffset.UTC)
            .format(java.time.Instant.now())
        val metadata = buildString {
            append("""
                <metadata>
                  <groupId>$groupId</groupId>
                  <artifactId>$artifact</artifactId>
                  <versioning>
                    <latest>$latest</latest>
                    <release>$latest</release>
                    <versions>
            """.trimIndent())
            for (v in versions) {
                append("\n                      <version>").append(v).append("</version>")
            }
            append("""
                    </versions>
                    <lastUpdated>$ts</lastUpdated>
                  </versioning>
                </metadata>
            """.trimIndent())
        }
        val metaPath = artifactDir.resolve("maven-metadata.xml")
        Files.newBufferedWriter(metaPath).use { w -> w.write(metadata) }
    }

    private fun normalizeUrl(s: String): String = s.replace(Regex("(?<!:)//+"), "/")
}
