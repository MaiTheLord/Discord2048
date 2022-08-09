import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

private val schemaDefinition = listOf(
    """
        CREATE TABLE leaderboard
        (
            user_id           BIGINT                              NOT NULL,
            guild_id          BIGINT                              NOT NULL,
            user_name         TEXT                                NOT NULL,
            guild_name        TEXT                                NOT NULL,
            score             INTEGER                             NOT NULL,
            turns             INTEGER                             NOT NULL,
            time_accomplished TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
        );
    """.trimIndent()
)

object Database {
    private val connection: Connection by lazy { DriverManager.getConnection(Config.databaseUrl).upgradeSchema() }

    object InsertIntoLeaderboard : Statement(connection, """
        DELETE FROM leaderboard WHERE user_id = @user_id AND guild_id = @guild_id;
        INSERT INTO leaderboard VALUES (@user_id, @guild_id, @user_name, @guild_name, @score, @turns);
    """.trimIndent()) {
        fun execute(user: User, guild: Guild, score: Int, turns: Int) {
            super.execute(
                Pair("user_id", user.idLong),
                Pair("guild_id", guild.idLong),
                Pair("user_name", user.asTag),
                Pair("guild_name", guild.name),
                Pair("score", score),
                Pair("turns", turns)
            )
        }
    }

    open class Statement(connection: Connection, sql: String) {
        private val parsed by lazy { parse(sql) }
        private val preparedStatement by lazy { connection.prepareStatement(parsed.first) }
        private val keys get() = parsed.second

        private fun parse(sql: String): Pair<String, List<String>> {
            val breakingChars = listOf(' ', ';', '\n', '\t', '(', ')', ',', ':')

            var parsed = ""
            val keys = ArrayList<String>()

            var inKey = false
            var currentKey = ""
            var lastLiteralChar = null as Char?

            for (char in sql) {
                if (inKey) {
                    if (breakingChars.contains(char)) {
                        inKey = false
                        keys.add(currentKey)
                        currentKey = ""
                        parsed += char
                    } else {
                        currentKey += char
                    }
                } else {
                    if (char != '@' || lastLiteralChar != null) parsed += char

                    when (char) {
                        '"', '\'' -> {
                            when (lastLiteralChar) {
                                char -> lastLiteralChar = null
                                null -> lastLiteralChar = char
                            }
                        }
                        '@' -> {
                            if (lastLiteralChar == null) {
                                parsed += '?'
                                inKey = true
                            }
                        }
                    }
                }
            }

            if (inKey) keys.add(currentKey)

            return Pair(parsed, keys)
        }

        @Synchronized
        private fun <T> bindAndExecute(parameters: Array<out Pair<String, Any>>, execute: (PreparedStatement) -> T): T {
            keys.forEachIndexed { index, key ->
                val value = parameters.firstOrNull { it.first == key }?.second
                preparedStatement.setObject(index + 1, value)
            }
            return execute(preparedStatement)
        }

        fun execute(vararg parameters: Pair<String, Any>) {
            bindAndExecute(parameters) { it.execute() }
        }

        fun query(vararg parameters: Pair<String, Any>): List<Map<String, Any?>> {
            val result = bindAndExecute(parameters) { it.executeQuery() }

            val rows: MutableList<Map<String, Any?>> = ArrayList()

            while (result.next()) {
                val row: MutableMap<String, Any?> = HashMap()

                for (i in 1..result.metaData.columnCount) {
                    row[result.metaData.getColumnLabel(i)] = result.getObject(i)
                }

                rows.add(row)
            }

            return rows
        }
    }
}

private fun Connection.upgradeSchema(): Connection {
    val schemaVersion = Database.Statement(this, """
        SELECT CURRENT_SETTING('schema.version', TRUE);
    """.trimIndent()).query()[0]["schema.version"] as? Int ?: 0

    for (i in schemaVersion until schemaDefinition.size) {
        Database.Statement(this, schemaDefinition[i]).execute()
        Database.Statement(this, """SET schema.version TO ${i + 1};""")
    }

    return this
}
