package byteboard.routing.posts

import byteboard.database.posts.createPost
import byteboard.database.posts.deletePost
import byteboard.enums.Length
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configurePostsRouting(){
    routing {
        authenticate("jwt") {

            delete("/byteboard/posts/delete/{postId}") {
                val params = call.parameters
                val postsId = params["postId"]?.toLongOrNull()
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if(userId == null || postsId == null || userId < 0 || postsId < 0){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Error Occurred"))
                }

                val result = deletePost(userId!!,postsId!!)
                if(result){
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "Successfully Deleted Post"))
                }else{
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Could Not Delete Post"))
                }
            }

            post("/byteboard/posts/create"){
                val params = call.parameters
                val title = params["title"].toString()
                val contents = params["contents"].toString()
                val topic = params["topic"].toString()
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()

                if(topic.length > Length.MAX_TOPIC_LENGTH.value){
                    call.respond(HttpStatusCode.NotAcceptable, mapOf("Response" to "Topic too large"))
                }

                if(contents.length > Length.MAX_CONTENT_LENGTH.value){
                    call.respond(HttpStatusCode.NotAcceptable, mapOf("Response" to "Content too long"))
                }

                if(title.length > Length.MAX_TITLE_LENGTH.value){
                    call.respond(HttpStatusCode.NotAcceptable, mapOf("Response" to "Title too long"))
                }

                if(userId == null){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                val postCreationSuccess = createPost(userId!!,contents,topic,title)

                if(postCreationSuccess){
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "Post published!"))
                }
                else{
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred while publishing post"))
                }

            }
        }
    }
}