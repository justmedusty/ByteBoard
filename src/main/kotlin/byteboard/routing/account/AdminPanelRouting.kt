import byteboard.database.admin.*
import byteboard.database.useraccount.*
import byteboard.enums.Length
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
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }
                if (uid == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "UID required"))
                }

                if (!isUserAdmin(userId!!)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("Response" to "Unauthorized"))
                }


                val logResult = insertSuspendEntry(userId, true, reason!!, uid!!)

                if (!logResult) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not insert log"))
                }

                val result = suspendUser(uid, userId)

                if (!result) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "User of uid $uid was suspended"))
            }



            post("/byteboard/admin/unsuspend/{uid}") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val uid = call.parameters["uid"]?.toLongOrNull()
                val reason = call.parameters["reason"]

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }
                if (uid == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "UID required"))
                }

                if (!isUserAdmin(userId!!)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("Response" to "Unauthorized"))
                }

                val logResult = insertSuspendEntry(userId, false, reason!!, uid!!)

                if (!logResult) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not insert log"))
                }

                val result = unSuspendUser(uid, userId)

                if (!result) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "User of uid $uid was unsuspended"))
            }


            post("/byteboard/admin/giveadmin/{uid}") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val uid = call.parameters["uid"]?.toLongOrNull()
                val reason = call.parameters["reason"]

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }
                if (uid == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "UID required"))
                }
                if (reason == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "reason required"))
                }

                if (!isUserAdmin(userId!!)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("Response" to "Unauthorized"))
                }

                val logResult = insertAdminLogEntry(uid!!, userId, reason!!, true)

                if (!logResult) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not insert log"))
                }

                val finalResult = giveAdmin(uid, userId)

                if (!finalResult) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }



                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "User of uid $uid was given admin"))
            }

            post("/byteboard/admin/takeadmin/{uid}") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val uid = call.parameters["uid"]?.toLongOrNull()
                val reason = call.parameters["reason"]

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }
                if (uid == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "UID required"))
                }

                if (!isUserAdmin(userId!!)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("Response" to "Unauthorized"))
                }

                val logResult = insertAdminLogEntry(uid!!, userId, reason!!, false)

                if (!logResult) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not insert log"))
                }

                val result = takeAdmin(uid, userId)

                if (!result) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "User of uid $uid is no longer admin"))
            }



            get("/byteboard/admin/getadminlogs/{order}") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val order = call.parameters["order"]
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull()
                    ?: 25).coerceAtMost(Length.MAX_LIMIT.value.toInt())
                var orderStr: String = ""
                var list: List<AdminLog>? = null

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }
                if (order.isNullOrEmpty()) {
                    orderStr = "newest"
                }

                //There are 2 checks so the one inside the log fetch func is redundant but for now I dont care
                if (!isUserAdmin(userId!!)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("Response" to "Unauthorized"))
                }

                list = getAdminLogEntries(page, limit, userId!!, orderStr!!)

                if (list == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "List null"))
                }

                call.respond(HttpStatusCode.BadRequest, mapOf("page" to page, "limit" to limit, "result_list" to list))
            }

            get("/byteboard/admin/getadminlogsforadmin/{uid}") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val uid = call.parameters["uid"]?.toLongOrNull()

                val order = call.parameters["order"]
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull()
                    ?: 25).coerceAtMost(Length.MAX_LIMIT.value.toInt())
                var list: List<AdminLog>? = null
                var orderStr: String = ""


                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                if (order.isNullOrEmpty()) {
                    orderStr = "newest"
                }
                orderStr = order.toString()
                if (uid == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "uid cannot be null"))
                }

                //There are 2 checks so the one inside the log fetch func is redundant but for now I dont care
                if (!isUserAdmin(userId!!)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("Response" to "Unauthorized"))
                }

                list = getAdminLogEntriesByAdmin(page, limit, userId!!, orderStr, uid!!)

                if (list == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                call.respond(HttpStatusCode.BadRequest, mapOf("page" to page, "limit" to limit, "result_list" to list))
            }

        }

    }

}