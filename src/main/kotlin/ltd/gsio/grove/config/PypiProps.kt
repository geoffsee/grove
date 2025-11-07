package ltd.gsio.grove.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

@ConfigurationProperties(prefix = "grove.pypi")
data class PypiProps(
    var enabled: Boolean = true,
    var storageDir: Path = Path.of("storage/pypi"),
    /** Public base URL for PyPI endpoints, e.g. http://localhost:8080/pypi */
    var baseUrl: String = "http://localhost:8080/pypi"
)
