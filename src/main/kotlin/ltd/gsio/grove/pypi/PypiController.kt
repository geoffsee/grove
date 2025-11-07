package ltd.gsio.grove.pypi

import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/pypi")
class PypiController(private val svc: PypiService) {

    @GetMapping("/simple", produces = [MediaType.TEXT_HTML_VALUE])
    fun simpleIndex(): String {
        val projects = svc.listProjects()
        val links = projects.joinToString("\n") { p -> "<a href=\"/pypi/simple/$p/\">$p</a>" }
        return """
            <html>
              <head><title>Simple Index</title></head>
              <body>
                $links
              </body>
            </html>
        """.trimIndent()
    }

    @GetMapping("/simple/{project}/", produces = [MediaType.TEXT_HTML_VALUE])
    fun projectPage(@PathVariable project: String): String {
        val files = svc.listFiles(project)
        val norm = PypiUtil.normalizeProjectName(project)
        val links = files.joinToString("\n") { f -> "<a href=\"/pypi/packages/$norm/${f.filename}\">${f.filename}</a>" }
        return """
            <html>
              <head><title>$norm</title></head>
              <body>
                <h1>$norm</h1>
                $links
              </body>
            </html>
        """.trimIndent()
    }

    @PostMapping("/api/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestParam name: String,
        @RequestParam version: String,
        @RequestPart("file") file: MultipartFile
    ): Map<String, Any> = svc.upload(name, version, file)

    @GetMapping("/packages/{project}/{filename}")
    fun download(
        @PathVariable project: String,
        @PathVariable filename: String
    ): ResponseEntity<FileSystemResource> {
        val path = svc.filePath(project, filename) ?: return ResponseEntity.notFound().build()
        val res = FileSystemResource(path)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${res.filename}\"")
            .contentLength(res.contentLength())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(res)
    }
}
