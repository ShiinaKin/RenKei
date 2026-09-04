package io.sakurasou.renkei.model.dao.message

import io.sakurasou.renkei.db.DatabaseSingleton
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

interface MessageAccessTokenDAO {
    suspend fun issue(
        messageID: Long,
        subscriberDeviceID: String,
        expiresAt: Long,
    ): String

    suspend fun consume(
        token: String,
        now: Long = System.currentTimeMillis(),
    ): RedeemedMessage?
}

data class RedeemedMessage(
    val messageID: Long,
    val content: String,
)

class SqlMessageAccessTokenDAO(
    private val secureRandom: SecureRandom = SecureRandom(),
) : MessageAccessTokenDAO {
    override suspend fun issue(
        messageID: Long,
        subscriberDeviceID: String,
        expiresAt: Long,
    ): String {
        val now = System.currentTimeMillis()
        require(expiresAt > now) { "Message access token expiry must be in the future" }
        val token = generateToken()
        DatabaseSingleton.dbQuery {
            MessageAccessTokens.deleteWhere {
                (MessageAccessTokens.expiresAt lessEq now) or MessageAccessTokens.consumedAt.isNotNull()
            }
            MessageAccessTokens.insert {
                it[tokenHash] = hashToken(token)
                it[MessageAccessTokens.messageID] = EntityID(messageID, Messages)
                it[MessageAccessTokens.subscriberDeviceID] = subscriberDeviceID
                it[MessageAccessTokens.expiresAt] = expiresAt
                it[consumedAt] = null
            }
        }
        return token
    }

    override suspend fun consume(
        token: String,
        now: Long,
    ): RedeemedMessage? =
        DatabaseSingleton.dbQuery {
            val hash = hashToken(token)
            val row =
                MessageAccessTokens
                    .innerJoin(Messages)
                    .selectAll()
                    .where {
                        (MessageAccessTokens.tokenHash eq hash) and
                            MessageAccessTokens.consumedAt.isNull() and
                            (MessageAccessTokens.expiresAt greater now)
                    }.singleOrNull()
                    ?: return@dbQuery null

            val consumed =
                MessageAccessTokens.update({
                    (MessageAccessTokens.tokenHash eq hash) and MessageAccessTokens.consumedAt.isNull()
                }) {
                    it[consumedAt] = now
                }
            if (consumed != 1) return@dbQuery null

            RedeemedMessage(
                messageID = row[Messages.id].value,
                content = row[Messages.content],
            )
        }

    private fun generateToken(): String =
        ByteArray(TOKEN_BYTES)
            .also(secureRandom::nextBytes)
            .let(Base64.getUrlEncoder().withoutPadding()::encodeToString)

    private fun hashToken(token: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.US_ASCII))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private companion object {
        const val TOKEN_BYTES = 32
    }
}
