package byteboard.routing.privatemessages

import Message
import byteboard.database.useraccount.getUserId
import byteboard.database.useraccount.userNameAlreadyExists
import byteboard.enums.Length
import getAllMessages
import getMessagesFromUser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import sendMessage

/**
 * Configure message routes
 *
 */
fun Application.configureMessageRoutes() {
    routing {
        authenticate("jwt") {
            post("/byteboard/messages/send") {
                val postParams = call.receiveParameters()
                val message = postParams["message"]
                val receiver = postParams["receiver"]
                val principal = this.call.principal<JWTPrincipal>()
                val id = principal?.payload?.subject?.toLongOrNull()


                if (receiver == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Receiver cannot be null"))
                }

                if (message == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Message cannot be null"))
                }

                if (id == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                if (message!!.length > Length.MAX_DM_MESSAGE_LENGTH.value) {
                    call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("Response" to "Message size exceeds the maximum limit (5000 characters)")
                    )
                }

                if (!userNameAlreadyExists(receiver!!)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "User does not exist"))
                }

                val result = sendMessage(id!!, getUserId(receiver), message)

                if (!result) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not send message"))
                }

                call.respond(HttpStatusCode.OK, mapOf("Response" to "Message sent"))
            }

            get("/byteboard/messages/fetchAll") {
                val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toLongOrNull()
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 25


                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                val messages: List<Message>? = getAllMessages(userId!!, page, limit)

                if (messages == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }
                call.respond(HttpStatusCode.OK, mapOf(page to page, limit to limit, messages to messages))
            }

            get("/byteboard/messages/fetchByUser") {
                val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toLongOrNull()
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 25
                val senderUsername = call.parameters["sender"]

                if(senderUsername.isNullOrEmpty()){
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to ""))
                }

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                val messages: List<Message>? = getMessagesFromUser(userId!!, getUserId(senderUsername!!), page, limit)

                if (messages == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }
                call.respond(HttpStatusCode.OK, mapOf(page to page, limit to limit, messages to messages))
            }
        }
    }
}