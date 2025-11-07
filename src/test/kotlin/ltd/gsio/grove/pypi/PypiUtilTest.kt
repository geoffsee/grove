// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2024 Grove Contributors

package ltd.gsio.grove.pypi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PypiUtilTest {

    @Test
    fun normalizeProjectName_variants() {
        val cases = mapOf(
            "Demo_Pkg" to "demo-pkg",
            "demo-pkg" to "demo-pkg",
            "demo.pkg" to "demo-pkg",
            "DEMO--__..PKG" to "demo-pkg",
            "a_b.c-d" to "a-b-c-d",
        )
        for ((input, expected) in cases) {
            assertEquals(expected, PypiUtil.normalizeProjectName(input), "normalizeProjectName('$input')")
        }
    }

    @Test
    fun normalizeUrl_preservesSchemeAndRemovesDupSlashes() {
        val input = "http://example.test/pypi//packages///demo-pkg//file.whl"
        val out = PypiUtil.normalizeUrl(input)
        assertEquals("http://example.test/pypi/packages/demo-pkg/file.whl", out)
    }
}
