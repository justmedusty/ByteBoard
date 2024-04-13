package byteboard.configuration

import byteboard.database.useraccount.verifyCredentials
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureSecurity() {
    // Please read the jwt property from the config file if you are using EngineMain
    val jwtAudience = "byteboard"
    val jwtDomain = "https://byteboard.tech"
    val jwtRealm = "byteboard"
    val jwtSecret = System.getenv("JWT_SECRET")
    authentication {
        jwt(name = "jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
    authentication {
        basic(name = "basic") {
            realm = "byteboard"
            validate { credentials ->
                if (verifyCredentials(credentials.name, credentials.password)
                ) UserIdPrincipal(credentials.name) else null
            }
        }


    }
}
