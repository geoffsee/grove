// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2024 Grove Contributors

package ltd.gsio.grove.npm

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NpmModelsJsonTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun distTags_areSerializedAndDeserializedWithCorrectJsonName() {
        val doc = NpmPackageDocument(
            name = "pkg",
            versions = mapOf(
                "1.0.0" to NpmVersion(
                    name = "pkg",
                    version = "1.0.0",
                    dist = NpmVersion.Dist(
                        tarball = "http://example.test/npm/pkg/-/pkg-1.0.0.tgz",
                        shasum = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef"
                    )
                )
            ),
            distTags = mapOf("latest" to "1.0.0")
        )
        val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc)
        // Ensure the property name is "dist-tags" in JSON
        assert(json.contains("\"dist-tags\"")) { json }

        val roundTrip: NpmPackageDocument = mapper.readValue(json)
        assertEquals("pkg", roundTrip.name)
        assertEquals("1.0.0", roundTrip.distTags["latest"])
        assertEquals("1.0.0", roundTrip.versions["1.0.0"]?.version)
    }
}
