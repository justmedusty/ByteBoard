package byteboard.routing.posts

import byteboard.database.posts.*
import byteboard.database.useraccount.isUserSuspended
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

                if(isUserSuspended(userId!!)){
                    call.respond(HttpStatusCode.Unauthorized, mapOf("Response" to "User suspended"))
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

            post("/byteboard/posts/like/{postId}") {
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
            post("/byteboard/posts/dislike/{postId}") {
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
            get("/byteboard/posts/{order}") {

                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                val page = call.parameters["page"]?.toIntOrNull() ?: 1

                val limit = call.parameters["limit"]?.toIntOrNull() ?: 25

                var postList:List<Post> = emptyList()

                val order = call.parameters["order"] ?: "recent"

                //TODO decide how to handle non logged in users , will decide later. This can stay for now
                if (userId == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }
                when (order) {
                    "recent" -> {
                        postList = fetchMostRecentPosts(page,limit,userId)
                    }

                    "old" -> {
                        postList= fetchPostsByOldest(page,limit,userId)
                    }

                    "liked" -> {
                        postList = fetchMostLikedPosts(page,limit,userId!!)
                    }

                    "disliked" -> {
                        postList = fetchMostDislikedPosts(page,limit,userId!!)
                    }

                    else -> {
                        call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid order"))

                    }
                }

            if (postList.isNotEmpty()) {

                call.respond(HttpStatusCode.OK, mapOf(page to page, limit to limit, postList to postList))

            } else call.respond(
                HttpStatusCode.NoContent,
                mapOf("Response" to "Could not fetch posts, an error may have occurred")
            )


        }
        get("/byteboard/posts/{topic}/{order}") {

            val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

            val page = call.parameters["page"]?.toIntOrNull() ?: 1

            val limit = call.parameters["limit"]?.toIntOrNull() ?: 25

            var postList:List<Post> = emptyList()
            val topic = call.parameters["topic"]
            val order = call.parameters["order"]

            if (topic.isNullOrEmpty()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Topic is required"))
            }
            //TODO decide how to handle non logged in users , will decide later. This can stay for now
            if (userId == null) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
            }
            when (order) {
                "recent" -> {
                    postList = fetchPostsByTopic(topic!!,page,limit,userId!!)
                }

                "old" -> {
                    postList= fetchPostsByOldestAndTopic(page,limit,topic!!,userId)
                }

                "liked" -> {
                    postList = fetchPostsByTopicAndLikes(page,limit, topic!!,userId!!)
                }

                "disliked" -> {
                    postList = fetchPostsByTopicAndDislikes(page,limit,topic!!,userId!!)
                }

                else -> {
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid order"))

                }
            }
            if (postList.isNotEmpty()) {

                call.respond(HttpStatusCode.OK, mapOf(page to page, limit to limit, postList to postList))

            } else call.respond(
                HttpStatusCode.NoContent,
                mapOf("Response" to "Could not fetch posts, an error may have occurred")
            )



        }
        get("/byteboard/posts/search/{query}") {
            val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

            val page = call.parameters["page"]?.toIntOrNull() ?: 1

            val limit = call.parameters["limit"]?.toIntOrNull() ?: 25

            val postList:List<Post>
            val query = call.parameters["query"]

            if(query.isNullOrEmpty()){
                call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Query cannot be empty"))
            }

            postList = searchPostByTitleOrContents(userId,query!!,limit,page)

            call.respond(HttpStatusCode.OK,mapOf(page to page,limit to limit, postList to postList))

        }

    }
}
}
