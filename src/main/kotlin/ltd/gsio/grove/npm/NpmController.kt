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

package ltd.gsio.grove.npm

import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/npm")
class NpmController(private val svc: NpmService) {

    @GetMapping("/{name}")
    fun metadata(@PathVariable name: String): ResponseEntity<Any> {
        val doc = svc.readPackage(name) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(doc)
    }

    @PutMapping("/{name}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun publish(
        @PathVariable name: String,
        @RequestParam version: String,
        @RequestPart("tarball") tarball: MultipartFile
    ): ResponseEntity<Map<String, Any>> {
        val result = svc.publish(name, version, tarball)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{name}/-/{file}")
    fun download(
        @PathVariable name: String,
        @PathVariable file: String
    ): ResponseEntity<FileSystemResource> {
        val path = svc.tarballPath(name, file) ?: return ResponseEntity.notFound().build()
        val res = FileSystemResource(path)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${res.filename}\"")
            .contentLength(res.contentLength())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(res)
    }
}
