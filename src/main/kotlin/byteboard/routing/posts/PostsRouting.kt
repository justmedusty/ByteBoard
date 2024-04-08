package byteboard.routing.posts

import byteboard.database.posts.*
import byteboard.enums.Length
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configurePostsRouting() {
    routing {
        authenticate("jwt") {

            delete("/byteboard/posts/delete/{postId}") {
                val params = call.parameters
                val postsId = params["postId"]?.toLongOrNull()
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (userId == null || postsId == null || userId < 0 || postsId < 0) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Error Occurred"))
                }

                val result = deletePost(userId!!, postsId!!)
                if (result) {
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "Successfully Deleted Post"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could Not Delete Post"))
                }
            }

            post("/byteboard/posts/create") {
                val params = call.parameters
                val title = params["title"].toString()
                val contents = params["contents"].toString()
                val topic = params["topic"].toString()
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (topic.length > Length.MAX_TOPIC_LENGTH.value) {
                    call.respond(HttpStatusCode.NotAcceptable, mapOf("Response" to "Topic too large"))
                }

                if (contents.length > Length.MAX_CONTENT_LENGTH.value) {
                    call.respond(HttpStatusCode.NotAcceptable, mapOf("Response" to "Content too long"))
                }

                if (title.length > Length.MAX_TITLE_LENGTH.value) {
                    call.respond(HttpStatusCode.NotAcceptable, mapOf("Response" to "Title too long"))
                }

                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                val postCreationSuccess = createPost(userId!!, contents, topic, title)

                if (postCreationSuccess) {
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "Post published!"))
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("Response" to "An error occurred while publishing post")
                    )
                }

            }

            post("/byteboard/posts/like/{id}") {
                val params = call.parameters
                val postsId = params["postId"]?.toLongOrNull()
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (postsId == null || userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                if (isPostLikedByUser(postsId!!, userId!!)) {
                    val success = (unlikePost(userId, postsId))
                    if (success) {
                        call.respond(HttpStatusCode.OK, mapOf("Response" to "Post Unliked"))
                    } else {
                        call.respond(HttpStatusCode.NotAcceptable, mapOf("Response" to "Cannot unlike post"))
                    }
                } else {
                    val success = likePost(userId, postsId)
                    if (success) {
                        call.respond(HttpStatusCode.OK, mapOf("Response" to "Post liked"))
                    } else {
                        call.respond(HttpStatusCode.NotAcceptable, mapOf("Response" to "Cannot like post"))
                    }
                }


            }
            post("/byteboard/posts/dislike/{id}") {
                val params = call.parameters
                val postsId = params["postId"]?.toLongOrNull()
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if (postsId == null || userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                if (isPostDislikedByUser(postsId!!, userId!!)) {
                    val success = (unDislikePost(userId, postsId))
                    if (success) {
                        call.respond(HttpStatusCode.OK, mapOf("Response" to "Post Undisliked"))
                    } else {
                        call.respond(HttpStatusCode.NotAcceptable, mapOf("Response" to "Cannot undislike post"))
                    }
                } else {
                    val success = dislikePost(userId, postsId)
                    if (success) {
                        call.respond(HttpStatusCode.OK, mapOf("Response" to "Post disliked"))
                    } else {
                        call.respond(HttpStatusCode.NotAcceptable, mapOf("Response" to "Cannot dislike post"))
                    }
                }


            }
            get("/byteboard/posts/latest") {

                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                val page = call.parameters["page"]?.toIntOrNull() ?: 1

                val limit = call.parameters["limit"]?.toIntOrNull() ?: 25

                val postList = fetchMostRecentPosts(page, limit, userId)

                if (postList.isNotEmpty()) {

                    call.respond(HttpStatusCode.OK, mapOf(page to page, limit to limit, postList to postList))

                } else call.respond(HttpStatusCode.NoContent, mapOf("Response" to "Could not fetch posts, an error may have occurred"))


        }
        get("/byteboard/posts/oldest") {

        }
        get("/byteboard/posts/topicNewest") {

        }
        get("/byteboard/posts/topicOldest") {

        }
        get("/byteboard/posts/topicLiked") {

        }
        get("/byteboard/posts/topicDisliked") {

        }
        get("/byteboard/posts/likedByMe") {

        }
        get("/byteboard/posts/dislikedByMe") {

        }
        get("/byteboard/posts/{id}") {

        }
        get("/byteboard/posts/{query}") {

        }

    }
}
}
