// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2024 Grove Contributors

package ltd.gsio.grove.maven

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
class MavenIntegrationTest {

    companion object {
        @JvmStatic
        @TempDir
        lateinit var tempDir: Path

        @JvmStatic
        @DynamicPropertySource
        fun props(reg: DynamicPropertyRegistry) {
            reg.add("grove.maven.storage-dir") { tempDir.resolve("maven").toString() }
            reg.add("grove.maven.base-url") { "http://example.test/maven/" }
        }
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun uploadAndDownloadAndMetadata() {
        val groupPath = "com/example/app"
        val artifactId = "my-app"
        val v1 = "1.0.0"
        val jar1 = MockMultipartFile("file", "$artifactId-$v1.jar", "application/octet-stream", "jar1".toByteArray())

        fun put(path: String, file: MockMultipartFile) =
            MockMvcRequestBuilders.multipart(path)
                .file(file)
                .with { req -> req.method = "PUT"; req }

        val uploadPath1 = "/maven/$groupPath/$artifactId/$v1/$artifactId-$v1.jar"
        mockMvc.perform(put(uploadPath1, jar1))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.path").value(uploadPath1.removePrefix("/maven/")))
            .andExpect(jsonPath("$.url").value("http://example.test/maven".removeSuffix("/") + "/" + uploadPath1.removePrefix("/maven/")))

        // Download works
        mockMvc.perform(get(uploadPath1))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", containsString("filename=\"$artifactId-$v1.jar\"")))
            .andExpect(content().contentType("application/octet-stream"))

        // Metadata file exists and contains version
        val metaPath = tempDir.resolve("maven").resolve("$groupPath/$artifactId/maven-metadata.xml")
        assertTrue(Files.exists(metaPath))
        val metaContent1 = Files.readString(metaPath)
        assertTrue(metaContent1.contains("<version>$v1</version>"))

        // Upload a second version and verify metadata updated
        val v2 = "1.1.0"
        val jar2 = MockMultipartFile("file", "$artifactId-$v2.jar", "application/octet-stream", "jar2".toByteArray())
        val uploadPath2 = "/maven/$groupPath/$artifactId/$v2/$artifactId-$v2.jar"
        mockMvc.perform(put(uploadPath2, jar2))
            .andExpect(status().isOk)

        val metaContent2 = Files.readString(metaPath)
        assertTrue(metaContent2.contains("<version>$v1</version>"))
        assertTrue(metaContent2.contains("<version>$v2</version>"))
        // latest/release should be set to v2
        assertTrue(metaContent2.contains("<latest>$v2</latest>"))
        assertTrue(metaContent2.contains("<release>$v2</release>"))

        // Missing file 404
        mockMvc.perform(get("/maven/$groupPath/$artifactId/$v2/missing.jar"))
            .andExpect(status().isNotFound)
    }
}
