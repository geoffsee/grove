package ltd.gsio.grove

import ltd.gsio.grove.config.NpmProps
import ltd.gsio.grove.config.PypiProps
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(NpmProps::class, PypiProps::class)
class GroveApplication

fun main(args: Array<String>) {
    runApplication<GroveApplication>(*args)
}
