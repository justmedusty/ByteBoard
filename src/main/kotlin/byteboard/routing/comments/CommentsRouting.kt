package byteboard.routing.comments


import byteboard.database.comments.*
import byteboard.database.posts.getPostOwnerId
import byteboard.database.useraccount.isUserSuspended
import byteboard.enums.Length
import byteboard.enums.Notif
import insertNotification
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
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull()
                    ?: 50).coerceAtMost(Length.MAX_PAGE_LIMIT.value.toInt())
                val order: String = call.parameters["order"] ?: "newest"
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()


                if (postId == null || postId < 0) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid post id"))
                }
                //TODO again we will want people to see stuff without being logged in but dont care for now so will just leave this todo here to remind me at a later date
                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                val commentList: List<Comment>? = getCommentsByPost(postId!!, limit, page, userId, order)

                if (commentList == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                call.respond(HttpStatusCode.OK, mapOf("page" to page, "limit" to limit, "comment_list" to commentList))

            }




            get("/byteboard/comments/get/user/{uid}") {

                val page: Int = call.parameters["page"]?.toInt() ?: 1
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull()
                    ?: 50).coerceAtMost(Length.MAX_PAGE_LIMIT.value.toInt())
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val uid: Long? = call.parameters["uid"]?.toLongOrNull()


                if (uid == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Requires user id"))
                }

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                val commentList: List<Comment>? = getCommentsByUser(uid!!, limit, page, userId)

                if (commentList == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                call.respond(HttpStatusCode.OK, mapOf("page" to page, "limit" to limit, "comment_list" to commentList))


            }

            get("/byteboard/comments/getchildren/{id}") {

                val page: Int = call.parameters["page"]?.toInt() ?: 1
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull()
                    ?: 50).coerceAtMost(Length.MAX_PAGE_LIMIT.value.toInt())
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val parent: Long? = call.parameters["id"]?.toLongOrNull()


                if (parent == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Requires parent id"))
                }

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                val commentList: List<Comment>? = getChildComments(parent!!, limit, page, userId)

                if (commentList == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                call.respond(HttpStatusCode.OK, mapOf("page" to page, "limit" to limit, "comment_list" to commentList))


            }

            post("/byteboard/comments/like/{commentId}") {

                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val commentId = call.parameters["commentId"]?.toLongOrNull()
                var result = false

                if (commentId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Comment ID cannot be null"))
                }

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }
                if (isCommentLikedByUser(commentId!!, userId)) {
                    result = unlikeComment(userId!!, commentId)

                    if (result) {

                        call.respond(HttpStatusCode.OK, mapOf("Response" to "Comment unliked"))

                    }

                } else {

                    result = likeComment(userId!!, commentId)

                    if (result) {

                        call.respond(HttpStatusCode.OK, mapOf("Response" to "Comment liked"))

                    }

                }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("Response" to "Could not like/unlike comment, error occurred")
                )
            }



            post("/byteboard/comments/dislike/{commentId}") {

                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val commentId = call.parameters["commentId"]?.toLongOrNull()
                val result: Boolean

                if (commentId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Comment ID cannot be null"))
                }

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                if (isCommentDisLikedByUser(commentId!!, userId)) {
                    result = unDislikeComment(userId!!, commentId)

                    if (result) {
                        call.respond(HttpStatusCode.OK, mapOf("Response" to "Comment undisliked"))
                    }

                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Error occurred"))

                } else {
                    result = dislikeComment(userId!!, commentId)

                    if (!result) {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Error occurred"))
                    }
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "Comment disliked"))

                }

            }

            post("/byteboard/comments/update/{commentId}") {


                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val commentId = call.parameters["commentId"]?.toLongOrNull()
                val update = call.parameters["update"]

                if (update == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Update cannot be null"))
                }

                if (commentId == null) {
                    call.respond(
                        HttpStatusCode.InternalServerError, mapOf("Response" to "Comment ID cannot be null")
                    )
                }

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                val result = updateComment(userId!!, commentId!!, update!!)

                if (!result) {
                    call.respond(
                        HttpStatusCode.InternalServerError, mapOf("Response" to "Could not update comment")
                    )
                }

                call.respond(HttpStatusCode.OK, mapOf("Response" to "Comment updated"))

            }
            post("/byteboard/comments/post") {
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val content = call.parameters["content"]
                val isReply: Boolean? = call.parameters["isReply"]?.toBoolean()
                val parentCommentId: Long? = call.parameters["parentId"]?.toLongOrNull()
                val postId: Long? = call.parameters["postId"]?.toLongOrNull()
                val notif: Boolean
                var parentCommentUser: Long? = null
                var poster: Long? = null


                if (content == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "content cannot be null"))
                }

                if (content!!.length > Length.MAX_COMMENT_LENGTH.value) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("Response" to "content exceeds maximum characters (5000)")
                    )
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

                if (isUserSuspended(userId!!)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("Response" to "User suspended"))
                }

                if (isReply!! && parentCommentId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Reply must have a parent"))
                }

                if (parentCommentId != null) {
                    parentCommentUser = getCommentOwnerId(parentCommentId)
                } else {
                    poster = getPostOwnerId(postId!!)
                }
                val result = postComment(content, userId, postId!!, isReply, parentCommentId)

                if (result == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not post comment"))
                }

                notif = if (isReply == true && parentCommentUser != null) {

                    insertNotification(result!!, parentCommentUser, Notif.COMMENT.value)

                } else if ((isReply == false) && (parentCommentUser == null) && (poster != null)) {

                    insertNotification(postId, poster, Notif.POST.value)

                } else {

                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid parameters"))
                    false
                }

                if (!notif) {
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "Comment posted but notification failed"))
                }


                call.respond(HttpStatusCode.OK, mapOf("Response" to "Comment posted"))

            }

            post("/byteboard/comments/delete/{commentId}") {

                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()
                val commentId: Long? = call.parameters["commentId"]?.toLongOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                if (commentId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Comment id required"))
                }

                val result = deleteCommentById(commentId!!, userId!!)

                if (!result) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could not delete post"))
                }

                call.respond(HttpStatusCode.OK, mapOf("Response" to "Comment deleted"))
            }


        }
    }
}
