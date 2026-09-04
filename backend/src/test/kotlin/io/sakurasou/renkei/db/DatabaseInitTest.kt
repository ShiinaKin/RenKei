package io.sakurasou.renkei.db

import java.sql.DriverManager
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.exposed.v1.jdbc.Database

class DatabaseInitTest {
    @Test
    fun `creates every application table in a new SQLite database`() {
        val databaseFile = createTempDirectory("renkei-schema-test-").resolve("data/renkei.db")
        val jdbcURL = "jdbc:sqlite:$databaseFile"
        prepareSQLiteDatabasePath(jdbcURL)

        val database = Database.connect(jdbcURL, driver = "org.sqlite.JDBC")
        DatabaseInit.init(database)

        val tables =
            DriverManager.getConnection(jdbcURL).use { connection ->
                connection
                    .prepareStatement(
                        "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
                    ).use { statement ->
                        statement.executeQuery().use { rows ->
                            buildSet {
                                while (rows.next()) add(rows.getString("name").lowercase())
                            }
                        }
                    }
            }

        assertEquals(
            setOf("devices", "messages", "message_access_tokens", "subcriberelation", "notification_targets"),
            tables,
        )

        val deviceColumns =
            DriverManager.getConnection(jdbcURL).use { connection ->
                connection.prepareStatement("PRAGMA table_info(devices)").use { statement ->
                    statement.executeQuery().use { rows ->
                        buildSet {
                            while (rows.next()) add(rows.getString("name"))
                        }
                    }
                }
            }
        assertTrue("public_key" in deviceColumns)

        val messageColumns =
            DriverManager.getConnection(jdbcURL).use { connection ->
                connection.prepareStatement("PRAGMA table_info(messages)").use { statement ->
                    statement.executeQuery().use { rows ->
                        buildSet {
                            while (rows.next()) add(rows.getString("name"))
                        }
                    }
                }
            }
        assertTrue("title" in messageColumns)
    }
}
