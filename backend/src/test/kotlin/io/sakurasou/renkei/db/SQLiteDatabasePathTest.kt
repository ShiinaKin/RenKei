package io.sakurasou.renkei.db

import java.nio.file.Files
import java.sql.DriverManager
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SQLiteDatabasePathTest {
    @Test
    fun `creates a missing parent directory for a SQLite file`() {
        val temporaryDirectory = createTempDirectory("renkei-sqlite-test-")
        val databaseFile = temporaryDirectory.resolve("nested/data/renkei.db")

        prepareSQLiteDatabasePath("jdbc:sqlite:$databaseFile")

        assertTrue(Files.isDirectory(databaseFile.parent))
        assertFalse(Files.exists(databaseFile))
        assertEquals(databaseFile, sqliteDatabaseFile("jdbc:sqlite:$databaseFile"))

        DriverManager.getConnection("jdbc:sqlite:$databaseFile").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE startup_check (id INTEGER PRIMARY KEY)")
            }
        }
        assertTrue(Files.isRegularFile(databaseFile))
    }

    @Test
    fun `does not create a path for an in-memory SQLite database`() {
        prepareSQLiteDatabasePath("jdbc:sqlite::memory:")

        assertNull(sqliteDatabaseFile("jdbc:sqlite::memory:"))
        assertNull(sqliteDatabaseFile("jdbc:sqlite:file:renkei?mode=memory&cache=shared"))
    }

    @Test
    fun `ignores non-SQLite JDBC URLs`() {
        prepareSQLiteDatabasePath("jdbc:postgresql://localhost/renkei")

        assertNull(sqliteDatabaseFile("jdbc:postgresql://localhost/renkei"))
    }
}
