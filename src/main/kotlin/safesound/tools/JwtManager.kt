package safesound.tools

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import safesound.model.Police
import safesound.model.User

/**
 * class to manage the jwt tokens
 * creation and verifying tokens
 */
object JwtManager {
    private val issuer = "Safe&Sound"
    private val jwtSecret = System.getenv("JWT_SECRET")
    private val algorithm = Algorithm.HMAC512(jwtSecret)
    val verifier : JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()

    /**
     * @param user contains User object which we use to generate token
     * @return generated token
     */
    fun generateTokenUser(user: User) : String {
        return JWT.create()
            .withSubject("Safe&SoundUser")
            .withIssuer(issuer)
            .withClaim("email", user.user_email)
            .sign(algorithm)
    }

    /**
     * @param police contains Police object which we use to generate token
     * @return generated token
     */
    fun generateTokenPolice(police: Police) : String {
        return JWT.create()
            .withSubject("Safe&SoundPolice")
            .withIssuer(issuer)
            .withClaim("badge", police.police_badge)
            .sign(algorithm)
    }
}