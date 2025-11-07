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

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/maven")
class MavenController(private val svc: MavenService) {

    @PutMapping("/**", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        request: HttpServletRequest,
        @RequestPart("file") file: MultipartFile
    ): Map<String, Any> {
        val path = extractPath(request)
        return svc.upload(path, file)
    }

    @GetMapping("/**")
    fun download(
        request: HttpServletRequest
    ): ResponseEntity<FileSystemResource> {
        val path = extractPath(request)
        val p = svc.filePath(path) ?: return ResponseEntity.notFound().build()
        val res = FileSystemResource(p)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${res.filename}\"")
            .contentLength(res.contentLength())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(res)
    }

    private fun extractPath(request: HttpServletRequest): String {
        val full = request.requestURI // e.g. /maven/com/foo/bar/1.0/bar-1.0.jar
        val prefix = request.contextPath + "/maven/"
        return if (full.startsWith(prefix)) full.substring(prefix.length) else ""
    }
}
