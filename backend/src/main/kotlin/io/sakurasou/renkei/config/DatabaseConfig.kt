package io.sakurasou.renkei.config

import io.ktor.server.application.Application
import io.sakurasou.renkei.db.DatabaseSingleton
import org.slf4j.LoggerFactory

/**
 * @author Shiina Kin
 * 2024/9/12 12:46
 */
fun Application.configureDatabase() {
    val jdbcURL = environment.config.property("ktor.application.database.url").getString()
    val driver = environment.config.property("ktor.application.database.driver").getString()
    DatabaseSingleton.init(
        jdbcURL = jdbcURL,
        driverClassName = driver,
        username = environment.config.property("ktor.application.database.username").getString(),
        password = environment.config.property("ktor.application.database.password").getString(),
    )
    log.info("Database initialized: driver={}, target={}", driver, databaseTargetForLog(jdbcURL))
}

private fun databaseTargetForLog(jdbcURL: String): String =
    when {
        jdbcURL == "jdbc:sqlite::memory:" -> "sqlite:memory"
        jdbcURL.startsWith("jdbc:sqlite:") -> "sqlite:file"
        else -> "${jdbcURL.removePrefix("jdbc:").substringBefore(':').take(40)}:configured"
    }

private val log = LoggerFactory.getLogger("DatabaseConfig")
