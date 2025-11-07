// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2024 Grove Contributors

package ltd.gsio.grove.pypi

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.nio.file.Files
import java.nio.file.Path

@SpringBootTest
@AutoConfigureMockMvc
class PypiIntegrationTest {

    companion object {
        @JvmStatic
        @TempDir
        lateinit var tempDir: Path

        @JvmStatic
        @DynamicPropertySource
        fun props(reg: DynamicPropertyRegistry) {
            reg.add("grove.pypi.storage-dir") { tempDir.resolve("pypi").toString() }
            reg.add("grove.pypi.base-url") { "http://example.test/pypi/" }
        }
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun uploadListAndDownload() {
        val nameVariants = listOf("Demo_Pkg", "demo-pkg", "demo.pkg")
        val norm = "demo-pkg"
        val version = "0.1.0"
        val filename = "demo_pkg-0.1.0-py3-none-any.whl"
        val file = MockMultipartFile("file", filename, "application/octet-stream", "wheel".toByteArray())

        // Upload using mixed case name
        val req = MockMvcRequestBuilders.multipart("/pypi/api/upload")
            .file(file)
            .param("name", nameVariants.first())
            .param("version", version)

        mockMvc.perform(req)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.project").value(norm))
            .andExpect(jsonPath("$.version").value(version))
            .andExpect(jsonPath("$.filename").value(filename))
            .andExpect(jsonPath("$.url").value("http://example.test/pypi".removeSuffix("/") + "/packages/$norm/$filename"))

        // Simple root lists normalized project
        mockMvc.perform(get("/pypi/simple"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andExpect(content().string(containsString("<a href=\"/pypi/simple/$norm/\">$norm</a>")))

        // Project page shows file link
        mockMvc.perform(get("/pypi/simple/$norm/"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andExpect(content().string(containsString("<a href=\"/pypi/packages/$norm/$filename\">$filename</a>")))

        // Download works
        mockMvc.perform(get("/pypi/packages/$norm/$filename"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", containsString("filename=\"$filename\"")))
            .andExpect(content().contentType("application/octet-stream"))

        // Missing file 404
        mockMvc.perform(get("/pypi/packages/$norm/missing.whl"))
            .andExpect(status().isNotFound)

        // Files actually persisted
        val projDir = tempDir.resolve("pypi").resolve(norm)
        assertTrue(Files.exists(projDir.resolve(filename)))
    }
}
