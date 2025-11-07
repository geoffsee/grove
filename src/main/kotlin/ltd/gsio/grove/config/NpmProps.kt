package ltd.gsio.grove.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

@ConfigurationProperties(prefix = "grove.npm")
data class NpmProps(
    var enabled: Boolean = true,
    var storageDir: Path = Path.of("storage/npm"),
    /** Public base URL for npm endpoints, e.g. http://localhost:8080/npm */
    var baseUrl: String = "http://localhost:8080/npm"
)
