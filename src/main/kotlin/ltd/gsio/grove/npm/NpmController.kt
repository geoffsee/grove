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
