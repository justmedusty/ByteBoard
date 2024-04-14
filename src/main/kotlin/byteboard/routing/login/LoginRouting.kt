import byteboard.database.useraccount.User
import byteboard.database.useraccount.createUser
import byteboard.database.useraccount.getUserId
import byteboard.database.useraccount.userNameAlreadyExists
import byteboard.security.JWTConfig
import byteboard.security.createJWT
import byteboard.security.hashPassword
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Signup
 *
 * @property userName
 * @property password
 * @constructor Create empty Signup
 */
data class Signup(val userName: String, val password: String)

/**
 * login
 *and sign up routing
 */
fun Application.configureLogin() {
    // Route for user login with Basic Auth

    routing {
        authenticate("basic") {
            post("/byteboard/login") {
                val principal = call.principal<UserIdPrincipal>() ?: error("Invalid credentials")
                val userName = principal.name
                val token = (createJWT(
                    JWTConfig(
                        "byteboard",
                        "https://byteboard.tech",
                        System.getenv("JWT_SECRET"),
                        getUserId(userName),
                        100000600000,
                    ),
                ))
                call.respond(mapOf("access_token" to token))
            }

        }
        post("/byteboard/signup") {

            val signup = call.receive<Signup>()

            val user = User(
                signup.userName, null, signup.password, isAdmin = false, isModerator = false, isSuspended = false
            )
            println(signup.password)
            when {
                user.userName.length < 6 || user.userName.length > 45 -> {
                    call.respond(
                        HttpStatusCode.Conflict, mapOf("Response" to "Username must be between 6 and 45 characters")
                    )
                }

                userNameAlreadyExists(signup.userName) -> {
                    call.respond(
                        HttpStatusCode.Conflict, mapOf("Response" to "This username is taken, please try another")
                    )
                }

                else -> {
                    createUser(user)
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "Successfully created your account"))
                }
            }
        }
    }
}