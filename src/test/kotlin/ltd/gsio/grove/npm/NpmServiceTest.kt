// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2024 Grove Contributors

package ltd.gsio.grove.npm

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ltd.gsio.grove.config.NpmProps
import ltd.gsio.grove.util.StorageUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files
import java.nio.file.Path

class NpmServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private fun service(baseUrl: String = "http://example.test/npm/"): NpmService {
        val props = NpmProps(storageDir = tempDir.resolve("npm"), baseUrl = baseUrl)
        return NpmService(props, StorageUtil(), jacksonObjectMapper())
    }

    @Test
    fun publish_writesFilesAndMetadata_andLatestUpdates_andUrlNormalized() {
        val svc = service("http://example.test/npm/")
        val name = "mypkg"
        val v1 = "1.0.0"
        val v2 = "1.1.0"
        val t1 = MockMultipartFile("tarball", "$name-$v1.tgz", "application/octet-stream", "tgz1".toByteArray())
        val t2 = MockMultipartFile("tarball", "$name-$v2.tgz", "application/octet-stream", "tgz2".toByteArray())

        svc.publish(name, v1, t1)
        svc.publish(name, v2, t2)

        val pkgDir = tempDir.resolve("npm").resolve(name)
        assertTrue(Files.exists(pkgDir.resolve("$name-$v1.tgz")))
        assertTrue(Files.exists(pkgDir.resolve("$name-$v2.tgz")))
        assertTrue(Files.exists(pkgDir.resolve("metadata.json")))

        val doc = svc.readPackage(name)!!
        assertEquals(name, doc.name)
        assertTrue(doc.versions.containsKey(v1))
        assertTrue(doc.versions.containsKey(v2))
        assertEquals(v2, doc.distTags["latest"])

        val tarballUrl = doc.versions[v2]!!.dist.tarball
        // baseUrl has trailing slash but should not duplicate
        assertTrue(tarballUrl.startsWith("http://example.test/npm/".removeSuffix("/") + "/$name/-/"))
        assertEquals("$name-$v2.tgz", tarballUrl.substringAfterLast('/'))

        // shasum present (non-empty hex)
        val shasum = doc.versions[v2]!!.dist.shasum
        assertNotNull(shasum)
        assertTrue(shasum!!.matches(Regex("[a-f0-9]{40}")))

        // tarballPath for missing returns null
        assertNull(svc.tarballPath(name, "missing.tgz"))
    }
}
