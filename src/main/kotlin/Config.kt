import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

object Config {
    val token by lazy { prop("TOKEN")!! }
    val adminId by lazy { prop("ADMIN_ID")!! }
    val inviteLink by lazy { prop("INVITE_LINK") }
    val databaseUrl by lazy {
        (
            jdbcUrl(
                prop("DATABASE_URL")
            ) ?:
            jdbcUrl(
                prop("DATABASE_HOST"),
                prop("DATABASE_PORT")?.toInt(),
                prop("DATABASE_NAME"),
                prop("DATABASE_USER"),
                prop("DATABASE_PASSWORD")
            )
        )!!
    }

    private val yaml by lazy {
        val file = File("config.local.yml")

        if (file.exists()) ObjectMapper(YAMLFactory()).registerKotlinModule().readValue(file, Map::class.java)
        else null
    }

    private fun prop(name: String): String? {
        return yaml?.get(name)?.toString() ?: System.getenv(name)
    }
}

private fun jdbcUrl(url: String?): String? {
    return if (url == null) null
    else if (url.startsWith("jdbc:postgresql://")) url
    else if (url.startsWith("postgres://")) {
        val parts = url.substringAfter("://").split(':', '@', '/')
        jdbcUrl(parts[2], parts[3].toInt(), parts[4], parts[0], parts[1])
    } else null
}

private fun jdbcUrl(host: String?, port: Int?, database: String?, user: String?, password: String?): String? {
    return if (host == null || port == null || database == null || user == null || password == null) null
    else "jdbc:postgresql://$host:$port/$database?user=$user&password=$password"
}
