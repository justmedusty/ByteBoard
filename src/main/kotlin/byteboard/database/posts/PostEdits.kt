package byteboard.database.posts

import byteboard.database.Users
import byteboard.database.comments.Comments
import byteboard.database.isUserAdmin
import byteboard.database.logger
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object PostEdits : Table(name = "PostEdits") {
    val id: Column<Long> = long("id").autoIncrement()
    val postId: Column<Long> = long("commentId").references(Comments.id)
    val posterId: Column<Long> = long("posterId").references(Users.id)
    val lastEdited: Column<LocalDateTime> = datetime("lastEdited")


    override val primaryKey = PrimaryKey(id)
}

data class PostEdit(
    val posterId: Long, val lastEdited: LocalDateTime
)

fun insertNewPostEdit(post: Long, poster: Long): Boolean {
    return try {
        transaction {
            PostEdits.insert {
                it[postId] = post
                it[posterId] = poster
                it[lastEdited] = LocalDateTime.now()
            }
            true

        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun editPost(postId: Long, userId: Long, newTitle: String?, newPostContents: String?): Boolean {
    if (verifyUserId(userId, postId) || isUserAdmin(userId)) {

        if (newTitle == null && newPostContents == null) {
            return false
        }
        return try {
            if(updatePostContents(newTitle, newPostContents, postId)){
                insertNewPostEdit(postId,userId)
            }else{
                false
            }
        } catch (e: Exception) {
            logger.error { e.message }
            return false
        }
    }
    return false
}