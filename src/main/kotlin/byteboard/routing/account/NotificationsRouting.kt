package byteboard.routing.account
import Notification
import byteboard.database.admin.*
import byteboard.database.useraccount.*
import getAllNotifications

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import markNotifRead
import markNotifUnread
import org.jetbrains.exposed.sql.not

fun Application.configureNotificationsRouting(){

    routing {
        authenticate("jwt") {

            get("/byteboard/notifications/fetch"){
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val page = call.parameters["page"]?.toLongOrNull() ?: 1
                val limit = call.parameters["limit"]?.toLongOrNull() ?: 25


                if(userId == null){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                val result : List<Notification>? = getAllNotifications(limit,page,userId!!)

                if(result == null){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not get result list"))
                }

                call.respond(HttpStatusCode.OK, mapOf("page" to page,"limit" to limit, "results" to result))
            }


            post("/byteboard/notifications/markread/{id}"){
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                val notificationId = call.parameters["id"]?.toLongOrNull()


                if(userId == null){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                if(notificationId == null){
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Notification ID required"))
                }

                val result = markNotifRead(notificationId!!,userId!!)

                if(!result){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not mark notification read"))
                }

                call.respond(HttpStatusCode.OK, mapOf("Response" to "Notification marked read"))


            }

            post("/byteboard/notifications/markunread/{id}"){
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                val notificationId = call.parameters["id"]?.toLongOrNull()


                if(userId == null){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                if(notificationId == null){
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Notification ID required"))
                }

                val result = markNotifUnread(notificationId!!,userId!!)

                if(!result){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not mark notification unread"))
                }

                call.respond(HttpStatusCode.OK, mapOf("Response" to "Notification marked unread"))


            }


        }
    }



}