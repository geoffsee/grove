// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2024 Grove Contributors

package ltd.gsio.grove.maven

import ltd.gsio.grove.config.MavenProps
import ltd.gsio.grove.util.StorageUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files
import java.nio.file.Path

class MavenServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private fun service(baseUrl: String = "http://example.test/maven/"): MavenService {
        val props = MavenProps(storageDir = tempDir.resolve("maven"), baseUrl = baseUrl)
        return MavenService(props, StorageUtil())
    }

    @Test
    fun upload_updatesMetadata_andReturnsNormalizedUrl() {
        val svc = service("http://example.test/maven///")
        val groupPath = "com/example/app"
        val artifactId = "my-app"
        val v1 = "1.0.0"
        val v2 = "1.1.0"
        val jar1 = MockMultipartFile("file", "$artifactId-$v1.jar", "application/octet-stream", byteArrayOf(1))
        val jar2 = MockMultipartFile("file", "$artifactId-$v2.jar", "application/octet-stream", byteArrayOf(2))

        val rel1 = "$groupPath/$artifactId/$v1/$artifactId-$v1.jar"
        val rel2 = "$groupPath/$artifactId/$v2/$artifactId-$v2.jar"

        val resp1 = svc.upload(rel1, jar1)
        assertEquals(true, resp1["ok"])
        assertEquals(rel1, resp1["path"])
        assertEquals("http://example.test/maven/$rel1", resp1["url"]) // normalized (no dup slashes)

        val resp2 = svc.upload(rel2, jar2)
        assertEquals(true, resp2["ok"])

        val artifactDir = tempDir.resolve("maven").resolve("$groupPath/$artifactId")
        val metaPath = artifactDir.resolve("maven-metadata.xml")
        assertTrue(Files.exists(metaPath))
        val xml = Files.readString(metaPath)
        assertTrue(xml.contains("<groupId>com.example.app</groupId>"))
        assertTrue(xml.contains("<artifactId>$artifactId</artifactId>"))
        assertTrue(xml.contains("<version>$v1</version>"))
        assertTrue(xml.contains("<version>$v2</version>"))
        assertTrue(xml.contains("<latest>$v2</latest>"))
        assertTrue(xml.contains("<release>$v2</release>"))
    }

    @Test
    fun filePathAndTraversalProtection() {
        val svc = service()
        val rel = "org/example/lib/lib/1.0.0/lib-1.0.0.jar"
        val jar = MockMultipartFile("file", "lib-1.0.0.jar", "application/octet-stream", byteArrayOf(3))
        svc.upload(rel, jar)

        assertNotNull(svc.filePath(rel))
        assertNull(svc.filePath("org/example/lib/lib/1.0.0/missing.jar"))

        // Path traversal should be rejected
        val badRel = "../outside.txt"
        val ex = assertThrows(IllegalArgumentException::class.java) {
            svc.upload(badRel, jar)
        }
        assertTrue(ex.message!!.contains("Invalid path"))
    }
}
