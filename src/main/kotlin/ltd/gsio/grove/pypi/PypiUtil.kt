package ltd.gsio.grove.pypi

object PypiUtil {
    // PEP 503 normalization: replace runs of -, _, . with -, and lowercase
    fun normalizeProjectName(name: String): String =
        name.lowercase().replace(Regex("[-_.]+"), "-")
}
