package byteboard.routing.profilechanges

import byteboard.corefunctions.cryptography.isValidOpenPGPPublicKey
import byteboard.database.useraccount.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureProfileChanges() {


    routing {
        authenticate("jwt") {
            post("/byteboard/profile/changeUserName") {
                val postParams = call.receiveParameters()
                val newUserName = postParams["newUser"] ?: error("No new value provided")
                val principal = call.principal<JWTPrincipal>()
                val id = principal?.payload?.subject?.toLongOrNull()

                if(id == null){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Error occurred"))
                }

                if (newUserName.isEmpty() || !userAndPasswordValidation(newUserName, "")) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("Response" to "Please provide a valid username. Must be between 6 and 45 characters and be unique"),
                    )
                } else {
                    try {
                        updateUserCredentials(getUserName(id!!).toString(), false, newUserName)
                        call.respond(HttpStatusCode.OK, mapOf("Response" to "Username updated successfully"))
                        logger.info { "user with id : $id changed userName to $newUserName" }
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("Response" to e.message))
                    }
                }
            }
            post("/byteboard/profile/changePassword") {
                val postParams = call.receiveParameters()
                val newPassword = postParams["newPassword"].toString()
                val principal = call.principal<JWTPrincipal>()
                val id = principal?.payload?.subject?.toLongOrNull()

                if(id == null){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Error occurred"))
                }

                if (newPassword.isEmpty() || !userAndPasswordValidation("", newPassword)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("Response" to "Please provide a valid password. Must be at least 8 characters"),
                    )
                } else {
                    try {
                        updateUserCredentials(
                            getUserName(id!!).toString(),
                            true,
                            newPassword,
                        )
                        call.respond(HttpStatusCode.OK, mapOf("Response" to "Password updated successfully"))
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("Response" to e.message))
                        logger.info { "user with id : $id changed their password" }
                    }
                }
            }
            delete("/byteboard/profile/deleteAccount") {
                val principal = call.principal<JWTPrincipal>()
                val id = principal?.payload?.subject

                val userId = id?.toLongOrNull()
                if (userId != null) {
                    deleteUser(userId)
                    call.respond(HttpStatusCode.OK, mapOf("Response" to "Account Deleted"))
                    logger.info { "user with id : $id deleted account" }
                } else {
                    call.respond(HttpStatusCode.Conflict, mapOf("Response" to "No Id Found"))
                }
            }

            post("/byteboard/profile/changeBio"){
                val postParams = call.receiveParameters()
                val principal = call.principal<JWTPrincipal>()
                val id = principal?.payload?.subject?.toLongOrNull()
                val bioContents =  postParams["newBio"].toString()

                if (id == null){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Error Occurred"))
                }
                if(bioContents.isNullOrEmpty()){
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "New Bio Empty"))
                }
                try {
                    updateBio(userId = id!!, bioContents )
                }catch (e:Exception){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Error occurred while updating bio"))
                }

                call.respond(HttpStatusCode.OK, mapOf("Response" to "Successfully updated bio"))
            }
            post("/byteboard/profile/changePublicKey"){
                val postParams = call.receiveParameters()
                val principal = call.principal<JWTPrincipal>()
                val id = principal?.payload?.subject?.toLongOrNull()
                val publicKey =  postParams["publicKey"].toString()
                if (id == null){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Error Occurred"))
                }
                if(publicKey.isEmpty()){
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Public key empty"))
                }
                if(!isValidOpenPGPPublicKey(publicKey)){
                    call.respond(HttpStatusCode.NotAcceptable, mapOf("Response" to "Public key is not valid"))
                }
                try {
                    updatePublicKey(userId = id!!, publicKey )
                }catch (e:Exception){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Error occurred while updating public key"))
                }

                call.respond(HttpStatusCode.OK, mapOf("Response" to "Successfully updated public key"))
            }

            post("/byteboard/profile/changeProfilePhoto"){
                val postParams = call.receiveParameters()
                val principal = call.principal<JWTPrincipal>()
                val id = principal?.payload?.subject?.toLongOrNull()
                val photo: ByteArray? =  postParams["profilePhoto"]?.toByteArray()
                if (id == null || photo == null){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Error Occurred"))
                }
                if(photo!!.size > 1048576 ){
                    call.respond(HttpStatusCode.PayloadTooLarge, mapOf("Response" to "Photo too large, must be < 10mb"))
                }
                if(photo.isEmpty()){
                    call.respond(HttpStatusCode.BadRequest, mapOf("Response" to "Photo file empty"))
                }

                try {
                    updateProfilePhoto(userId = id!!, photo )
                }catch (e:Exception){
                    call.respond(HttpStatusCode.InternalServerError, mapOf("Response" to "Error occurred while updating public key"))
                }

                call.respond(HttpStatusCode.OK, mapOf("Response" to "Successfully updated profile photo"))
            }



        }
    }
}