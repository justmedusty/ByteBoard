package byteboard.routing.account

import byteboard.database.useraccount.getProfileDataEntry
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureUserProfileRouting(){

    routing {
        authenticate("jwt") {

            post("/byteboard/profiles/get/{id}"){

                val userId : Long? = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val uuid = call.parameters["id"]?.toLongOrNull()

                if(userId == null){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                if(uuid == null){
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "UID cannot be null"))

                }

                val result = getProfileDataEntry(uuid!!)

                if(result == null){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not get profile data"))
                }

                call.respond(HttpStatusCode.OK, mapOf(result to result))

            }


        }
    }

}