import byteboard.database.admin.insertAdminLogEntry
import byteboard.database.admin.insertSuspendEntry
import byteboard.database.useraccount.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureAdminPanelRouting() {

    routing {
        authenticate("jwt") {
            post("/byteboard/admin/suspend/{uid}") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val uid = call.parameters["uid"]?.toLongOrNull()
                val reason = call.parameters["reason"]

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, "Response" to "An error occurred")
                }
                if (uid == null) {
                    call.respond(HttpStatusCode.BadRequest, "Response" to "UID required")
                }

                if (!isUserAdmin(userId!!)) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                }


                val logResult = insertSuspendEntry(userId,true,reason!!,uid!!)

                if(!logResult){
                    call.respond(HttpStatusCode.InternalServerError, "Response" to "Could not insert log")
                }

                val result = suspendUser(uid!!, userId)

                if (!result) {
                    call.respond(HttpStatusCode.InternalServerError, "Response" to "An error occurred")
                }
                call.respond(HttpStatusCode.BadRequest, "Response" to "User of uid $uid was suspended")
            }



            post("/byteboard/admin/unsuspend/{uid}") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val uid = call.parameters["uid"]?.toLongOrNull()
                val reason = call.parameters["reason"]

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, "Response" to "An error occurred")
                }
                if (uid == null) {
                    call.respond(HttpStatusCode.BadRequest, "Response" to "UID required")
                }

                if (!isUserAdmin(userId!!)) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                }

                val logResult = insertSuspendEntry(userId,false,reason!!,uid!!)

                if(!logResult){
                    call.respond(HttpStatusCode.InternalServerError, "Response" to "Could not insert log")
                }

                val result = unSuspendUser(uid!!, userId)

                if (!result) {
                    call.respond(HttpStatusCode.InternalServerError, "Response" to "An error occurred")
                }
                call.respond(HttpStatusCode.BadRequest, "Response" to "User of uid $uid was unsuspended")
            }
        }

        post("/byteboard/admin/giveadmin/{uid}") {
            val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
            val uid = call.parameters["uid"]?.toLongOrNull()
            val reason = call.parameters["reason"]

            if (userId == null) {
                call.respond(HttpStatusCode.InternalServerError, "Response" to "An error occurred")
            }
            if (uid == null) {
                call.respond(HttpStatusCode.BadRequest, "Response" to "UID required")
            }
            if (reason == null) {
            call.respond(HttpStatusCode.BadRequest, "Response" to "reason required")
        }

            if (!isUserAdmin(userId!!)) {
                call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
            }

            val logResult = insertAdminLogEntry(uid!!,userId,reason!!,true)

            if(!logResult){
                call.respond(HttpStatusCode.InternalServerError, "Response" to "Could not insert log")
            }

            val finalResult = giveAdmin(uid!!, userId)

            if (!finalResult) {
                call.respond(HttpStatusCode.InternalServerError, "Response" to "An error occurred")
            }



            call.respond(HttpStatusCode.BadRequest, "Response" to "User of uid $uid was given admin")
        }

        post("/byteboard/admin/takeadmin/{uid}") {
            val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
            val uid = call.parameters["uid"]?.toLongOrNull()
            val reason = call.parameters["reason"]

            if (userId == null) {
                call.respond(HttpStatusCode.InternalServerError, "Response" to "An error occurred")
            }
            if (uid == null) {
                call.respond(HttpStatusCode.BadRequest, "Response" to "UID required")
            }

            if (!isUserAdmin(userId!!)) {
                call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
            }

            val logResult = insertAdminLogEntry(uid!!,userId,reason!!,false)

            if(!logResult){
                call.respond(HttpStatusCode.InternalServerError, "Response" to "Could not insert log")
            }

            val result = takeAdmin(uid!!, userId)

            if (!result) {
                call.respond(HttpStatusCode.InternalServerError, "Response" to "An error occurred")
            }
            call.respond(HttpStatusCode.BadRequest, "Response" to "User of uid $uid is no longer admin")
        }
    }


}