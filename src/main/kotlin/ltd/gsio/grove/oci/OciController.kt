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

import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/v2")
class OciController(private val svc: OciService) {

    private fun apiHeaders(headers: HttpHeaders): HttpHeaders {
        headers.add("Docker-Distribution-API-Version", "registry/2.0")
        return headers
    }

    @GetMapping("/")
    fun ping(): ResponseEntity<Void> {
        val headers = apiHeaders(HttpHeaders())
        return ResponseEntity.status(HttpStatus.OK).headers(headers).build()
    }

    // Start upload (or monolithic if digest provided and body present)
    @PostMapping("/{name:.+}/blobs/uploads/")
    fun startUpload(
        @PathVariable name: String,
        request: HttpServletRequest,
        @RequestParam(required = false) digest: String?
    ): ResponseEntity<Void> {
        return if (digest != null) {
            // monolithic upload: finalize immediately with request body
            val uuid = svc.createUpload()
            val len = request.inputStream.readAllBytes()
            val body = len.inputStream()
            val computed = try {
                svc.finalizeUpload(uuid, body, digest)
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
            }
            val headers = apiHeaders(HttpHeaders())
            headers.add("Docker-Content-Digest", computed)
            headers.add(HttpHeaders.LOCATION, "/v2/$name/blobs/$computed")
            ResponseEntity.status(HttpStatus.CREATED).headers(headers).build()
        } else {
            val uuid = svc.createUpload()
            val headers = apiHeaders(HttpHeaders())
            headers.add("Docker-Upload-UUID", uuid)
            headers.add(HttpHeaders.LOCATION, "/v2/$name/blobs/uploads/$uuid")
            headers.add("Range", "0-0")
            ResponseEntity.status(HttpStatus.ACCEPTED).headers(headers).build()
        }
    }

    @PatchMapping("/{name:.+}/blobs/uploads/{uuid}")
    fun patchUpload(
        @PathVariable name: String,
        @PathVariable uuid: String,
        request: HttpServletRequest
    ): ResponseEntity<Void> {
        svc.appendToUpload(uuid, request.inputStream)
        val size = java.nio.file.Files.size(svc.uploadPath(uuid))
        val headers = apiHeaders(HttpHeaders())
        headers.add(HttpHeaders.LOCATION, "/v2/$name/blobs/uploads/$uuid")
        headers.add("Range", "0-${size}")
        headers.add("Docker-Upload-UUID", uuid)
        return ResponseEntity.status(HttpStatus.ACCEPTED).headers(headers).build()
    }

    @PutMapping("/{name:.+}/blobs/uploads/{uuid}")
    fun putUpload(
        @PathVariable name: String,
        @PathVariable uuid: String,
        request: HttpServletRequest,
        @RequestParam digest: String
    ): ResponseEntity<Void> {
        val computed = try {
            svc.finalizeUpload(uuid, request.inputStream, digest)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
        val headers = apiHeaders(HttpHeaders())
        headers.add("Docker-Content-Digest", computed)
        headers.add(HttpHeaders.LOCATION, "/v2/$name/blobs/$computed")
        return ResponseEntity.status(HttpStatus.CREATED).headers(headers).build()
    }

    @GetMapping("/{name:.+}/blobs/{digest}")
    fun getBlob(
        @PathVariable name: String,
        @PathVariable digest: String
    ): ResponseEntity<FileSystemResource> {
        val p = svc.blobPath(digest) ?: return ResponseEntity.notFound().build()
        val res = FileSystemResource(p)
        val headers = apiHeaders(HttpHeaders())
        headers.add("Docker-Content-Digest", digest)
        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(res.contentLength())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(res)
    }

    @RequestMapping("/{name:.+}/blobs/{digest}", method = [RequestMethod.HEAD])
    fun headBlob(
        @PathVariable name: String,
        @PathVariable digest: String
    ): ResponseEntity<Void> {
        val p = svc.blobPath(digest) ?: return ResponseEntity.notFound().build()
        val headers = apiHeaders(HttpHeaders())
        headers.add("Docker-Content-Digest", digest)
        headers.add("Content-Length", java.nio.file.Files.size(p).toString())
        return ResponseEntity.ok().headers(headers).build()
    }

    @PutMapping("/{name:.+}/manifests/{reference}")
    fun putManifest(
        @PathVariable name: String,
        @PathVariable reference: String,
        request: HttpServletRequest,
        @RequestHeader(HttpHeaders.CONTENT_TYPE, required = false) ct: String?
    ): ResponseEntity<Void> {
        val bytes = request.inputStream.readAllBytes()
        val digest = svc.putManifest(name, reference, bytes)
        val headers = apiHeaders(HttpHeaders())
        headers.add("Docker-Content-Digest", digest)
        headers.add(HttpHeaders.LOCATION, "/v2/$name/manifests/$reference")
        return ResponseEntity.status(HttpStatus.CREATED).headers(headers).build()
    }

    @GetMapping("/{name:.+}/manifests/{reference}")
    fun getManifest(
        @PathVariable name: String,
        @PathVariable reference: String
    ): ResponseEntity<FileSystemResource> {
        val p = svc.manifestPath(name, reference) ?: return ResponseEntity.notFound().build()
        val res = FileSystemResource(p)
        val digest = svc.manifestDigest(name, reference)!!
        val headers = apiHeaders(HttpHeaders())
        headers.add("Docker-Content-Digest", digest)
        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(res.contentLength())
            .contentType(MediaType.APPLICATION_JSON)
            .body(res)
    }

    @RequestMapping("/{name}/manifests/{reference}", method = [RequestMethod.HEAD])
    fun headManifest(
        @PathVariable name: String,
        @PathVariable reference: String
    ): ResponseEntity<Void> {
        val p = svc.manifestPath(name, reference) ?: return ResponseEntity.notFound().build()
        val digest = svc.manifestDigest(name, reference)!!
        val headers = apiHeaders(HttpHeaders())
        headers.add("Docker-Content-Digest", digest)
        headers.add("Content-Length", java.nio.file.Files.size(p).toString())
        return ResponseEntity.ok().headers(headers).build()
    }

    @GetMapping("/{name:.+}/tags/list")
    fun tags(@PathVariable name: String): Map<String, Any> {
        val tags = svc.listTags(name)
        return mapOf("name" to name, "tags" to tags)
    }
}
