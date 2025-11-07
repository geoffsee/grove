// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2024 Grove Contributors

package ltd.gsio.grove.oci

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.nio.file.Files
import java.nio.file.Path

@SpringBootTest
@AutoConfigureMockMvc
class OciIntegrationTest {

    companion object {
        @JvmStatic
        @TempDir
        lateinit var tempDir: Path

        @JvmStatic
        @DynamicPropertySource
        fun props(reg: DynamicPropertyRegistry) {
            reg.add("grove.oci.storage-dir") { tempDir.resolve("oci").toString() }
        }
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun ping_blobAndManifestFlow() {
        // Ping
        mockMvc.perform(get("/v2/"))
            .andExpect(status().isOk)
            .andExpect(header().string("Docker-Distribution-API-Version", containsString("registry/2.0")))

        val repo = "testrepo"
        val blobBytes = "hello blob".toByteArray()
        val hex = java.security.MessageDigest.getInstance("SHA-256").digest(blobBytes).joinToString("") { "%02x".format(it) }
        val digest = "sha256:$hex"

        // Monolithic upload via POST with digest query
        mockMvc.perform(
            post("/v2/$repo/blobs/uploads/").queryParam("digest", digest)
                .content(blobBytes)
                .contentType("application/octet-stream")
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Docker-Content-Digest", digest))
            .andExpect(header().string("Location", "/v2/$repo/blobs/$digest"))

        // GET blob
        mockMvc.perform(get("/v2/$repo/blobs/$digest"))
            .andExpect(status().isOk)
            .andExpect(header().string("Docker-Content-Digest", digest))
            .andExpect(header().string("Content-Length", blobBytes.size.toString()))

        // Put a simple manifest tagged as "latest"
        val manifest = "{" +
                "\"schemaVersion\": 2, \"mediaType\": \"application/vnd.oci.image.manifest.v1+json\", " +
                "\"config\": { \"mediaType\": \"application/vnd.oci.image.config.v1+json\", \"digest\": \"$digest\", \"size\": ${blobBytes.size} }, " +
                "\"layers\": [ { \"mediaType\": \"application/vnd.oci.image.layer.v1.tar\", \"digest\": \"$digest\", \"size\": ${blobBytes.size} } ] }"

        mockMvc.perform(
            put("/v2/$repo/manifests/latest").contentType("application/vnd.oci.image.manifest.v1+json").content(manifest)
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Docker-Content-Digest", containsString("sha256:")))

        // Retrieve manifest by tag
        val getResp = mockMvc.perform(get("/v2/$repo/manifests/latest"))
            .andExpect(status().isOk)
            .andExpect(header().string("Docker-Content-Digest", containsString("sha256:")))
            .andReturn()
        val body = getResp.response.contentAsString
        assertTrue(body.contains("schemaVersion"))

        // Tags list
        mockMvc.perform(get("/v2/$repo/tags/list"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(repo))
            .andExpect(jsonPath("$.tags[0]").value("latest"))

        // Files persisted
        val blobsDir = tempDir.resolve("oci").resolve("blobs").resolve("sha256").resolve(hex)
        assertTrue(Files.exists(blobsDir))
    }
}
