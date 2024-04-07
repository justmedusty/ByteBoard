package byteboard.routing.posts

import byteboard.database.posts.deletePost
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
        }
    }
}