import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

object Config {
    val token by lazy { prop("TOKEN")!! }
    val adminId by lazy { prop("ADMIN_ID")!! }
    val inviteLink by lazy { prop("INVITE_LINK") }

    private val yaml by lazy {
        val file = File("config.local.yml")

        if (file.exists()) ObjectMapper(YAMLFactory()).registerKotlinModule().readValue(file, Map::class.java)
        else null
    }

    private fun prop(name: String): String? {
        return yaml?.get(name)?.toString() ?: System.getenv(name)
    }
}
