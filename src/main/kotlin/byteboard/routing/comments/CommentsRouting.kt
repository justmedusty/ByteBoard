package byteboard.routing.comments


import byteboard.database.comments.Comment
import byteboard.database.comments.getCommentsByPost
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCommentsRouting() {
    routing {
        authenticate("jwt") {

            get("/byteboard/comments/get/{postid}"){

                val postId : Long? = call.parameters["postid"]?.toLongOrNull()
                val page : Int = call.parameters["page"]?.toInt() ?: 1
                val limit : Int = call.parameters["limit"]?.toInt() ?: 50
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull()


                if(postId == null || postId > 0){
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Invalid post id"))
                }
                //TODO again we will want people to see stuff without being logged in but dont care for now so will just leave this todo here to remind me at a later date
                if(userId == null ){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "An error occurred"))
                }

                val commentList : List<Comment> = getCommentsByPost(postId!!,limit,page,userId)

                call.respond(HttpStatusCode.OK, mapOf(page to page,limit to limit,commentList to commentList))

            }




        }


    }
}