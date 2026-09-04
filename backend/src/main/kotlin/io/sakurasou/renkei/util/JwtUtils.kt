package io.sakurasou.renkei.util

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.sakurasou.renkei.config.JwtConfig.audience
import io.sakurasou.renkei.config.JwtConfig.issuer
import io.sakurasou.renkei.config.JwtConfig.secret
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.toJavaInstant

/**
 * @author ShiinaKin
 * 2024/9/14 13:06
 */
object JwtUtils {
    fun generateJwtToken(
        deviceUniqueID: String,
        expireDuration: String = "9999d",
    ): String =
        JWT
            .create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("device_id", deviceUniqueID)
            .withExpiresAt(
                Clock.System
                    .now()
                    .plus(Duration.parse(expireDuration))
                    .toJavaInstant(),
            ).sign(Algorithm.HMAC256(secret))

    fun verifier(): JWTVerifier =
        JWT
            .require(Algorithm.HMAC256(secret))
            .withAudience(audience)
            .withIssuer(issuer)
            .build()
}
