package ltd.gsio.grove.util

import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

@Component
class StorageUtil {
    fun ensureDir(dir: Path) {
        Files.createDirectories(dir)
    }

    fun saveMultipart(target: Path, file: MultipartFile) {
        ensureDir(target.parent)
        file.inputStream.use { input ->
            Files.newOutputStream(target).use { out ->
                input.copyTo(out)
            }
        }
    }

    fun sha1(path: Path): String = digestHex(path, "SHA-1")
    fun sha256(path: Path): String = digestHex(path, "SHA-256")

    fun digestHex(path: Path, algorithm: String): String {
        Files.newInputStream(path).use { input ->
            return digestHex(input, algorithm)
        }
    }

    fun digestHex(input: InputStream, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buf)
            if (read <= 0) break
            md.update(buf, 0, read)
        }
        return md.digest().joinToString("") { b -> "%02x".format(b) }
    }
}
