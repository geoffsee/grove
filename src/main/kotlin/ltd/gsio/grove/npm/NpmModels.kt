package ltd.gsio.grove.npm

import com.fasterxml.jackson.annotation.JsonProperty

// Minimal npm metadata document to satisfy npm client
// See: https://github.com/npm/registry/blob/master/docs/REGISTRY-API.md (simplified)
data class NpmVersion(
    val name: String,
    val version: String,
    val dist: Dist
) {
    data class Dist(
        val tarball: String,
        val shasum: String?
    )
}

data class NpmPackageDocument(
    val name: String,
    val versions: Map<String, NpmVersion>,
    @field:JsonProperty("dist-tags")
    @get:JsonProperty("dist-tags")
    val distTags: Map<String, String>
)
