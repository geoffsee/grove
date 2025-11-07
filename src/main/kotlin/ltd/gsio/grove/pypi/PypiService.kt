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

package ltd.gsio.grove.pypi

import ltd.gsio.grove.config.PypiProps
import ltd.gsio.grove.util.StorageUtil
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

@Service
class PypiService(
    private val props: PypiProps,
    private val storage: StorageUtil
) {
    private fun projectDir(project: String): Path = props.storageDir.resolve(PypiUtil.normalizeProjectName(project))

    fun listProjects(): List<String> {
        val dir = props.storageDir
        if (!Files.exists(dir)) return emptyList()
        return Files.list(dir)
            .use { stream -> stream.filter { Files.isDirectory(it) }.map { it.name }.sorted().toList() }
    }

    data class ProjectFile(val filename: String, val url: String)

    fun listFiles(project: String): List<ProjectFile> {
        val dir = projectDir(project)
        if (!Files.exists(dir)) return emptyList()
        return Files.list(dir).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .map { p -> ProjectFile(p.fileName.toString(), PypiUtil.normalizeUrl("${props.baseUrl}/packages/${PypiUtil.normalizeProjectName(project)}/${p.fileName}")) }
                .sorted(compareBy { it.filename })
                .toList()
        }
    }

    fun upload(name: String, version: String, file: MultipartFile): Map<String, Any> {
        val norm = PypiUtil.normalizeProjectName(name)
        val dir = projectDir(norm)
        storage.ensureDir(dir)
        val filename = file.originalFilename?.takeIf { it.isNotBlank() }
            ?: "$norm-$version.tar.gz"
        val target = dir.resolve(filename)
        storage.saveMultipart(target, file)
        val url = PypiUtil.normalizeUrl("${props.baseUrl}/packages/$norm/$filename")
        return mapOf("ok" to true, "project" to norm, "version" to version, "filename" to filename, "url" to url)
    }

    fun filePath(project: String, filename: String): Path? {
        val path = projectDir(project).resolve(filename)
        return if (Files.exists(path)) path else null
    }
}
