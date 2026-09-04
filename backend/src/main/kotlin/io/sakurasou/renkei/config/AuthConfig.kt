package io.sakurasou.renkei.config

import io.ktor.server.application.Application

/**
 * @author Shiina Kin
 * 2024/9/12 12:44
 */

fun Application.configureAuth() {
    configureBasic()
    configureJwt()
}

fun Application.configureBasic() {
    val basicUsername = environment.config.property("basic.username").getString()
    val basicPassword = environment.config.property("basic.password").getString()

    AuthConfig.init(basicUsername, basicPassword)
}

fun Application.configureJwt() {
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtRealm = environment.config.property("jwt.realm").getString()

    JwtConfig.init(jwtSecret, jwtIssuer, jwtAudience, jwtRealm)
}

object AuthConfig {
    lateinit var basicUsername: String
    lateinit var basicPassword: String

    fun init(
        basicUsername: String,
        basicPassword: String,
    ) {
        this.basicUsername = basicUsername
        this.basicPassword = basicPassword
    }
}

object JwtConfig {
    lateinit var secret: String
    lateinit var issuer: String
    lateinit var audience: String
    lateinit var realm: String

    fun init(
        jwtSecret: String,
        jwtIssuer: String,
        jwtAudience: String,
        jwtRealm: String,
    ) {
        this.secret = jwtSecret
        this.issuer = jwtIssuer
        this.audience = jwtAudience
        this.realm = jwtRealm
    }
}
