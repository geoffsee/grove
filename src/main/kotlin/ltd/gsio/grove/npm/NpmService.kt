package ltd.gsio.grove.npm

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import ltd.gsio.grove.config.NpmProps
import ltd.gsio.grove.util.StorageUtil
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path

@Service
class NpmService(
    private val props: NpmProps,
    private val storage: StorageUtil,
    private val objectMapper: ObjectMapper
) {
    private fun pkgDir(name: String): Path = props.storageDir.resolve(name)
    private fun metaPath(name: String): Path = pkgDir(name).resolve("metadata.json")

    fun publish(name: String, version: String, tarball: MultipartFile): Map<String, Any> {
        val dir = pkgDir(name)
        storage.ensureDir(dir)
        val tgzName = "$name-$version.tgz"
        val tgzPath = dir.resolve(tgzName)
        storage.saveMultipart(tgzPath, tarball)
        val shasum = storage.sha1(tgzPath)

        val current = readPackage(name)
        val versions = (current?.versions ?: emptyMap()).toMutableMap()
        val distTags = (current?.distTags ?: emptyMap()).toMutableMap()

        val tarballUrl = url("${props.baseUrl}/$name/-/$tgzName")
        val npmVersion = NpmVersion(
            name = name,
            version = version,
            dist = NpmVersion.Dist(
                tarball = tarballUrl,
                shasum = shasum
            )
        )
        versions[version] = npmVersion
        distTags["latest"] = version
        val doc = NpmPackageDocument(name = name, versions = versions, distTags = distTags)
        Files.newBufferedWriter(metaPath(name)).use { w ->
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(w, doc)
        }
        return mapOf("ok" to true, "id" to name, "rev" to version)
    }

    fun readPackage(name: String): NpmPackageDocument? {
        val path = metaPath(name)
        if (!Files.exists(path)) return null
        Files.newBufferedReader(path).use { r ->
            return objectMapper.readValue<NpmPackageDocument>(r)
        }
    }

    fun tarballPath(name: String, file: String): Path? {
        val path = pkgDir(name).resolve(file)
        return if (Files.exists(path)) path else null
    }

    private fun url(s: String): String = s.replace(Regex("(?<!:)//+"), "/")
}
