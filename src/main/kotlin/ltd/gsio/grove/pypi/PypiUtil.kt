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

package ltd.gsio.grove.pypi

object PypiUtil {
    // PEP 503 normalization: replace runs of -, _, . with -, and lowercase
    fun normalizeProjectName(name: String): String =
        name.lowercase().replace(Regex("[-_.]+"), "-")

    // Normalize URL to avoid duplicate slashes while preserving scheme (e.g., http://)
    fun normalizeUrl(s: String): String = s.replace(Regex("(?<!:)//+"), "/")
}
