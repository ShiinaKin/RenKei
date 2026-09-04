package io.sakurasou.renkei.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import io.sakurasou.renkei.config.AuthConfig
import io.sakurasou.renkei.config.JwtConfig
import io.sakurasou.renkei.config.JwtConfig.audience
import io.sakurasou.renkei.util.JwtUtils

fun Application.configureSecurity() {
    authentication {
        basic("auth-basic") {
            realm = JwtConfig.realm
            validate { credentials ->
                if (credentials.name == AuthConfig.basicUsername && credentials.password == AuthConfig.basicPassword) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtUtils.verifier())
            validate { credential ->
                if (credential.payload.audience.contains(audience)) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
}
