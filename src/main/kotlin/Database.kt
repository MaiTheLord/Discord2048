import java.sql.Connection
import java.sql.DriverManager

object Database {
    val connection: Connection by lazy { DriverManager.getConnection(Config.databaseUrl) }

}