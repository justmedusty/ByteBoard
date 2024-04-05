package byteboard.database.posts

import byteboard.database.useraccount.Users
import byteboard.database.comments.Comments
import byteboard.database.useraccount.isUserAdmin
import byteboard.database.useraccount.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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

fun checkLastPostEdit(postId: Long, userId: Long): LocalDateTime? {
    return try {
        transaction {
            val latestEdit = PostEdits.select { PostEdits.postId eq postId }
                .orderBy(PostEdits.lastEdited, SortOrder.DESC)
                .limit(1)
                .singleOrNull()

            latestEdit?.getOrNull(PostEdits.lastEdited)
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}