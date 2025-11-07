// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2024 Grove Contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
