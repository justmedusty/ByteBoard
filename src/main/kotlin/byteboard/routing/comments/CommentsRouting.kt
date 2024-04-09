package byteboard.routing.comments


import byteboard.database.comments.*
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

            val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
            val commentId = call.parameters["commentId"]?.toLongOrNull()

            if (commentId == null) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Comment ID cannot be null"))
            }

            if (userId == null) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
            }

            val result = likeComment(userId!!,commentId!!)

            if(!result){
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not like comment, error occurred"))
            }

            call.respond(HttpStatusCode.OK, mapOf("Response" to "Comment liked"))

        }

        post("/byteboard/comments/dislike/{commentId}"){

            val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
            val commentId = call.parameters["commentId"]?.toLongOrNull()

            if (commentId == null) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Comment ID cannot be null"))
            }

            if (userId == null) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
            }

            val result = dislikeComment(userId!!,commentId!!)

            if(!result){
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not dislike comment, error occurred"))
            }

            call.respond(HttpStatusCode.OK, mapOf("Response" to "Comment disliked"))

        }
        post("/byteboard/comments/update/{commentId}"){


            val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
            val commentId = call.parameters["commentId"]?.toLongOrNull()
            val update = call.parameters["update"]

            if(update == null){
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Update cannot be null"))
            }

            if (commentId == null) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Comment ID cannot be null"))
            }

            if (userId == null) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
            }

            val result = updateComment(userId!!,commentId!!,update!!)

            if(!result){
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not update comment"))
            }

            call.respond(HttpStatusCode.OK, mapOf("Response" to "Comment updated"))

        }
        post("/byteboard/comments/post"){


            val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
            val content = call.parameters["content"]
            val isReply : Boolean? = call.parameters["isReply"]?.toBoolean()
            val parentCommentId : Long? = call.parameters["parentId"]?.toLongOrNull()
            val postId : Long? = call.parameters["postId"]?.toLongOrNull()


            if(content == null){
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "content cannot be null"))
            }

            if (isReply == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "isReply cannot be null"))
            }


            if (postId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "postId cannot be null"))
            }

            if (isReply == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "isReply cannot be null"))
            }

            if (userId == null) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
            }

            if(isReply!! && parentCommentId == null){
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Reply must have a parent"))
            }

            val result = postComment(content!!,userId!!,postId!!,isReply!!,parentCommentId)

            if(!result){
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not post comment"))
            }

            call.respond(HttpStatusCode.OK, mapOf("Response" to "Comment posted"))



        }
        post("/byteboard/comments/delete/{commentId}"){

            val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
            val commentId : Long?= call.parameters["commentId"]?.toLongOrNull()

            if (userId == null) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
            }

            if(commentId == null){
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Comment id required"))
            }

            val result = deleteCommentById(commentId!!,userId!!)

            if(!result){
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not delete post"))
            }

            call.respond(HttpStatusCode.OK, mapOf("Response" to "Comment deleted"))
        }


    }
}