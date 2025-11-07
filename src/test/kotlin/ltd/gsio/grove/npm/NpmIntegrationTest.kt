// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2024 Grove Contributors

package ltd.gsio.grove.npm

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.util.MimeTypeUtils
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files
import java.nio.file.Path

@SpringBootTest
@AutoConfigureMockMvc
class NpmIntegrationTest {

    companion object {
        @JvmStatic
        @TempDir
        lateinit var tempDir: Path

        private val mapper = jacksonObjectMapper()

        @JvmStatic
        @DynamicPropertySource
        fun props(reg: DynamicPropertyRegistry) {
            reg.add("grove.npm.storage-dir") { tempDir.resolve("npm").toString() }
            // Use a base URL with trailing slash to verify normalization
            reg.add("grove.npm.base-url") { "http://example.test/npm/" }
        }
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun publishMetadataAndDownload() {
        val name = "mypkg"
        val version = "1.0.0"
        val tgzBytes = "hello tgz".toByteArray()
        val tgz = MockMultipartFile("tarball", "$name-$version.tgz", MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE, tgzBytes)

        val req = MockMvcRequestBuilders.multipart("/npm/$name")
            .file(tgz)
            .param("version", version)
            .with { request ->
                request.method = "PUT"
                request
            }

        mockMvc.perform(req)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.id").value(name))
            .andExpect(jsonPath("$.rev").value(version))

        // Verify metadata
        val meta = mockMvc.perform(get("/npm/$name"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val node: JsonNode = mapper.readTree(meta)
        assertEquals(name, node.path("name").asText())
        val tarballUrl = node.path("versions").path(version).path("dist").path("tarball").asText()
        // Should use configured base URL without duplicate slashes
        assertTrue(tarballUrl.startsWith("http://example.test/npm/".removeSuffix("/") + "/$name/-/"))
        assertEquals("$name-$version.tgz", tarballUrl.substringAfterLast('/'))
        // latest tag
        assertEquals(version, node.path("dist-tags").path("latest").asText())

        // Download the tarball
        mockMvc.perform(get("/npm/$name/-/$name-$version.tgz"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", containsString("filename=\"$name-$version.tgz\"")))
            .andExpect(content().contentType("application/octet-stream"))
            .andExpect(header().string("Content-Length", tgzBytes.size.toString()))

        // Missing package 404
        mockMvc.perform(get("/npm/does-not-exist"))
            .andExpect(status().isNotFound)

        // Missing tarball 404
        mockMvc.perform(get("/npm/$name/-/missing.tgz"))
            .andExpect(status().isNotFound)

        // Files actually persisted
        val pkgDir = tempDir.resolve("npm").resolve(name)
        assertTrue(Files.exists(pkgDir.resolve("$name-$version.tgz")))
        assertTrue(Files.exists(pkgDir.resolve("metadata.json")))
    }
}
