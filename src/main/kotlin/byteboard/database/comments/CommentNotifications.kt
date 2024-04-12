package byteboard.database.comments

import byteboard.database.posts.PostNotifications
import byteboard.database.useraccount.Users
import byteboard.database.useraccount.logger
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

/*
* Notes for me to think on:
* Every reply needs to generate an entry
* probably need to return the new id in the post comment function so that this can be properly inserted
* need to be sure that the reply is not the parent comment owner
*
*
 */
object CommentNotifications : Table(name = "CommentNotifications") {
    val id: Column<Long> = long("id").autoIncrement()
    val read: Column<Boolean> = bool("read").default(false)
    val commentId: Column<Long> = long("comment_id").references(Comments.id, onDelete = ReferenceOption.CASCADE)
    val userId : Column<Long> = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}

fun insertCommentNotification(comment: Long, user: Long): Boolean {
    return try {
        transaction {
            CommentNotifications.insert {
                it[commentId] = comment
                it[userId] = user
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}