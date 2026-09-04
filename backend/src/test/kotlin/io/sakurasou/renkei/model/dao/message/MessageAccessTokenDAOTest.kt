package io.sakurasou.renkei.model.dao.message

import io.sakurasou.renkei.db.DatabaseSingleton
import io.sakurasou.renkei.model.dao.device.Devices
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

class MessageAccessTokenDAOTest {
    @Test
    fun `stores only a token hash and allows one redemption`() =
        runBlocking {
            val databaseFile = createTempDirectory("renkei-token-test-").resolve("renkei.db")
            DatabaseSingleton.init(
                jdbcURL = "jdbc:sqlite:$databaseFile",
                driverClassName = "org.sqlite.JDBC",
                username = "",
                password = "",
            )
            val messageID =
                DatabaseSingleton.dbQuery {
                    val deviceID =
                        Devices.insertAndGetId {
                            it[name] = "iPhone"
                            it[uniqueID] = "iphone"
                            it[platform] = "IOS"
                            it[publicKey] = "unused"
                        }
                    Messages
                        .insertAndGetId {
                            it[Messages.deviceID] = deviceID
                            it[previewContent] = "preview"
                            it[content] = "full message"
                            it[timestamp] = 1L
                        }.value
                }
            val dao = SqlMessageAccessTokenDAO()

            val token = dao.issue(messageID, "iphone", System.currentTimeMillis() + 60_000)
            val storedHash =
                DatabaseSingleton.dbQuery {
                    MessageAccessTokens.selectAll().single()[MessageAccessTokens.tokenHash]
                }

            assertEquals(43, token.length)
            assertEquals(64, storedHash.length)
            assertNull(storedHash.takeIf { it.contains(token) })
            assertEquals("full message", assertNotNull(dao.consume(token)).content)
            assertNull(dao.consume(token))
        }
}
