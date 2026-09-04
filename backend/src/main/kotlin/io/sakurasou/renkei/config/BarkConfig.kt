package io.sakurasou.renkei.config

import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig
import java.net.URI
import java.time.Duration

data class BarkConfig(
    val baseURL: URI,
    val publicBaseURL: URI,
    val title: String,
    val group: String,
    val encryptionKey: String,
    val requestTimeout: Duration,
    val messageLinkTTL: Duration,
) {
    init {
        requireSecureHttpURL(baseURL, "Bark base URL")
        requireSecureHttpURL(publicBaseURL, "Bark public base URL")
        require(baseURL.userInfo == null) { "Bark base URL must not contain credentials" }
        require(baseURL.query == null && baseURL.fragment == null) {
            "Bark base URL must not contain a query or fragment"
        }
        require(publicBaseURL.userInfo == null) { "Bark public base URL must not contain credentials" }
        require(publicBaseURL.query == null && publicBaseURL.fragment == null) {
            "Bark public base URL must not contain a query or fragment"
        }
        require(title.isNotBlank()) { "Bark notification title is required" }
        require(group.isNotBlank()) { "Bark notification group is required" }
        require(title.toByteArray(Charsets.UTF_8).size <= MAX_TITLE_BYTES) {
            "Bark notification title must not exceed $MAX_TITLE_BYTES UTF-8 bytes"
        }
        require(group.toByteArray(Charsets.UTF_8).size <= MAX_GROUP_BYTES) {
            "Bark notification group must not exceed $MAX_GROUP_BYTES UTF-8 bytes"
        }
        require(encryptionKey.length == ENCRYPTION_KEY_LENGTH && encryptionKey.all { it.code in 0x20..0x7E }) {
            "Bark encryption key must contain exactly $ENCRYPTION_KEY_LENGTH printable ASCII characters"
        }
        require(!requestTimeout.isZero && !requestTimeout.isNegative) { "Bark request timeout must be positive" }
        require(!messageLinkTTL.isZero && !messageLinkTTL.isNegative) { "Bark message link TTL must be positive" }
    }

    val pushEndpoint: URI = URI.create("${baseURL.toString().trimEnd('/')}/push")

    fun messageURL(token: String): URI = URI.create("${publicBaseURL.toString().trimEnd('/')}/notification-message#$token")

    private fun requireSecureHttpURL(
        uri: URI,
        label: String,
    ) {
        require(uri.isAbsolute && uri.host != null) { "$label must be an absolute URL" }
        val isHTTPS = uri.scheme.equals("https", ignoreCase = true)
        val isLoopbackHTTP =
            uri.scheme.equals("http", ignoreCase = true) &&
                uri.host.lowercase() in setOf("localhost", "127.0.0.1", "::1")
        require(isHTTPS || isLoopbackHTTP) {
            "$label must use HTTPS (HTTP is allowed only for loopback development URLs)"
        }
    }

    private companion object {
        const val ENCRYPTION_KEY_LENGTH = 32
        const val MAX_TITLE_BYTES = 128
        const val MAX_GROUP_BYTES = 64
    }
}

fun Application.loadBarkConfig(): BarkConfig? {
    val config = environment.config.config("bark")
    if (!config.optionalBoolean("enabled", default = false)) return null

    val timeoutMillis = config.requiredString("timeoutMillis").toLongOrNull()
    require(timeoutMillis != null && timeoutMillis > 0) { "Bark timeoutMillis must be a positive integer" }
    val messageLinkTTLSeconds = config.requiredString("messageLinkTtlSeconds").toLongOrNull()
    require(messageLinkTTLSeconds != null && messageLinkTTLSeconds > 0) {
        "Bark messageLinkTtlSeconds must be a positive integer"
    }

    return BarkConfig(
        baseURL = URI.create(config.requiredString("baseUrl")),
        publicBaseURL = URI.create(config.requiredString("publicBaseUrl")),
        title = config.requiredString("title"),
        group = config.requiredString("group"),
        encryptionKey = config.requiredString("encryptionKey"),
        requestTimeout = Duration.ofMillis(timeoutMillis),
        messageLinkTTL = Duration.ofSeconds(messageLinkTTLSeconds),
    )
}

private fun ApplicationConfig.requiredString(path: String): String =
    property(path).getString().trim().also {
        require(it.isNotEmpty()) { "Bark configuration '$path' is required" }
    }

private fun ApplicationConfig.optionalBoolean(
    path: String,
    default: Boolean,
): Boolean = propertyOrNull(path)?.getString()?.toBooleanStrictOrNull() ?: default
