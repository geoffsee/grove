// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2024 Grove Contributors

package ltd.gsio.grove.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files
import java.nio.file.Path

class StorageUtilTest {

    @TempDir
    lateinit var tempDir: Path

    private val storage = StorageUtil()

    @Test
    fun saveMultipart_createsDirectoriesAndWritesContent() {
        val content = "hello world".toByteArray()
        val mmf = MockMultipartFile("file", "hello.txt", "text/plain", content)
        val target = tempDir.resolve("sub/dir/hello.txt")

        storage.saveMultipart(target, mmf)

        assertTrue(Files.exists(target))
        assertEquals(content.size.toLong(), Files.size(target))
        assertEquals("hello world", Files.readString(target))
    }

    @Test
    fun digestCalculations_areCorrect() {
        val data = "abc".toByteArray()
        val mmf = MockMultipartFile("file", "abc.bin", "application/octet-stream", data)
        val target = tempDir.resolve("abc.bin")
        storage.saveMultipart(target, mmf)

        val sha1 = storage.sha1(target)
        val sha256 = storage.sha256(target)
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", sha1)
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256)
    }
}
