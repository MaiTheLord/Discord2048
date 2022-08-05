import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

object Config {
    val token get() = properties.token
    val adminId get() = properties.adminId

    private data class Properties(val token: String, val adminId: String)

    private val properties: Properties by lazy {
        ObjectMapper(YAMLFactory())
            .registerKotlinModule()
            .readValue(File("config.local.yml"), Properties::class.java)
    }
}
