// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2024 Grove Contributors

package ltd.gsio.grove.pypi

import ltd.gsio.grove.config.PypiProps
import ltd.gsio.grove.util.StorageUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files
import java.nio.file.Path

class PypiServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private fun service(baseUrl: String = "http://example.test/pypi/"): PypiService {
        val props = PypiProps(storageDir = tempDir.resolve("pypi"), baseUrl = baseUrl)
        return PypiService(props, StorageUtil())
    }

    @Test
    fun upload_storesUnderNormalizedProject_andListsFilesAndProjects() {
        val svc = service()
        val nameMixed = "Demo_Pkg"
        val version = "0.1.0"
        val filename = "demo_pkg-0.1.0-py3-none-any.whl"
        val file = MockMultipartFile("file", filename, "application/octet-stream", "wheel".toByteArray())

        val resp = svc.upload(nameMixed, version, file)
        assertEquals(true, resp["ok"])
        assertEquals("demo-pkg", resp["project"])
        assertEquals(version, resp["version"])
        assertEquals(filename, resp["filename"])
        assertEquals("http://example.test/pypi/packages/demo-pkg/$filename", resp["url"])

        val projDir = tempDir.resolve("pypi").resolve("demo-pkg")
        assertTrue(Files.exists(projDir.resolve(filename)))

        val projects = svc.listProjects()
        assertEquals(listOf("demo-pkg"), projects)

        val files = svc.listFiles("demo-pkg")
        assertEquals(1, files.size)
        assertEquals(filename, files[0].filename)
        assertEquals("http://example.test/pypi/packages/demo-pkg/$filename", files[0].url)

        // filePath works
        assertNotNull(svc.filePath("demo-pkg", filename))
        assertNull(svc.filePath("demo-pkg", "missing.whl"))
    }

    @Test
    fun listFiles_sortedAndUrlNormalized() {
        val svc = service("http://example.test/pypi///")
        val proj = "example_proj"
        val f1 = MockMultipartFile("file", "b.whl", "application/octet-stream", byteArrayOf(1))
        val f2 = MockMultipartFile("file", "a.whl", "application/octet-stream", byteArrayOf(2))
        svc.upload(proj, "0.0.1", f1)
        svc.upload(proj, "0.0.2", f2)
        val list = svc.listFiles(proj)
        // Sorted by filename
        assertEquals(listOf("a.whl", "b.whl"), list.map { it.filename })
        // URL normalized (no duplicate slashes)
        assertTrue(list.all { it.url == "http://example.test/pypi/packages/example-proj/${it.filename}" })
        // explicit check
        assertEquals("http://example.test/pypi/packages/example-proj/a.whl", list[0].url)
    }
}
