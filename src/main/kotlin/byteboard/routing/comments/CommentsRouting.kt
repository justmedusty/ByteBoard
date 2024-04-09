package byteboard.routing.comments


import byteboard.database.comments.Comment
import byteboard.database.comments.getCommentsByPost
import byteboard.database.comments.getCommentsByUser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCommentsRouting() {
    routing {
        authenticate("jwt") {

            get("/byteboard/comments/get/{postid}") {

                val postId: Long? = call.parameters["postid"]?.toLongOrNull()
                val page: Int = call.parameters["page"]?.toInt() ?: 1
                val limit: Int = call.parameters["limit"]?.toInt() ?: 50
                val order: String = call.parameters["order"] ?: "newest"
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()


                if (postId == null || postId > 0) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid post id"))
                }
                //TODO again we will want people to see stuff without being logged in but dont care for now so will just leave this todo here to remind me at a later date
                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                val commentList: List<Comment> = getCommentsByPost(postId!!, limit, page, userId, order)

                call.respond(HttpStatusCode.OK, mapOf(page to page, limit to limit, commentList to commentList))

            }


        }

        get("/byteboard/comments/get/user/{uid}") {

            val page: Int = call.parameters["page"]?.toInt() ?: 1
            val limit: Int = call.parameters["limit"]?.toInt() ?: 50
            val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
            val uid: Long? = call.parameters["uid"]?.toLongOrNull()
            val commentList: List<Comment>

            if (uid == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Requires user id"))
            }

            if (userId == null) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
            }

            commentList = getCommentsByUser(uid!!, limit, page, userId)

            call.respond(HttpStatusCode.OK, mapOf(page to page, limit to limit, commentList to commentList))


        }

        post("/byteboard/comments/like/{commentId}"){

        }
        post("/byteboard/comments/dislike/{commentId}"){

        }
        post("/byteboard/comments/update/{commentId}"){

        }
        post("/byteboard/comments/post"){

        }
        post("/byteboard/comments/delete/{commentId}"){

        }


    }
}