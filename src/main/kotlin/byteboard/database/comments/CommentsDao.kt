package byteboard.database.comments

import byteboard.database.Users
import byteboard.database.isUserAdmin
import byteboard.database.logger
import byteboard.database.posts.Posts
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object Comments : Table(name = "Comments") {
    val id: Column<Long> = long("id").autoIncrement()
    val content: Column<String> = text("postContent")
    val postId: Column<Long> = long("post").references(Posts.id, ReferenceOption.CASCADE)
    val commenterId: Column<Long> = long("commenterId").references(Users.id)
    val isReply: Column<Boolean> = bool("isReply").default(false)
    val parentCommentId: Column<Long?> =
        long("parentCommentId").references(id, onDelete = ReferenceOption.CASCADE).nullable().default(null)
    val timeStamp: Column<LocalDateTime> = datetime("time_posted").defaultExpression(CurrentDateTime)


    override val primaryKey = PrimaryKey(id)
}


data class Comment(
    val id: Long,
    val content: String,
    val postId: Long,
    val commenterId: Long,
    val isReply: Boolean,
    val parentCommentId: Long?,
    val timeStamp: LocalDateTime
)

fun postComment(content: String, commenterId: Long, postId: Long, isReply: Boolean, parentCommentId: Long?): Boolean {
    return try {
        transaction {
            Comments.insert {
                it[Comments.content] = content
                it[Comments.postId] = postId
                it[Comments.commenterId] = commenterId
                it[Comments.isReply] = isReply
                it[Comments.parentCommentId] = parentCommentId
                it[timeStamp] = LocalDateTime.now()
            }
            true
        }
    } catch (e: Exception) {
        println("Error posting comment: $e")
        false
    }
}

fun getCommentById(id: Long): Comment? {
    return try {
        transaction {
            Comments.select { Comments.id eq id }.singleOrNull()?.let {
                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    it[Comments.isReply],
                    it[Comments.parentCommentId],
                    it[Comments.timeStamp]
                )
            }
        }
    } catch (e: Exception) {
        println("Error getting comment by ID: $e")
        null
    }
}

fun getCommentsByPost(postId: Long, pageSize: Int, page: Long): List<Comment> {
    return try {
        transaction {
            Comments.select { Comments.postId eq postId }.limit(pageSize, offset = (page - 1) * pageSize).map {
                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    it[Comments.isReply],
                    it[Comments.parentCommentId],
                    it[Comments.timeStamp]
                )
            }
        }
    } catch (e: Exception) {
        println("Error getting comments by post: $e")
        emptyList()
    }
}

fun getCommentsByUser(userId: Long, pageSize: Int, page: Long): List<Comment> {
    return try {
        transaction {
            Comments.select { Comments.commenterId eq userId }.limit(pageSize, offset = (page - 1) * pageSize).map {
                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    it[Comments.isReply],
                    it[Comments.parentCommentId],
                    it[Comments.timeStamp]
                )
            }
        }
    } catch (e: Exception) {
        println("Error getting comments by post: $e")
        emptyList()
    }
}

fun isIdCommentPoster(userId: Long, commentId: Long): Boolean {
    return try {
        transaction {
            val match = Comments.select { (Comments.id eq commentId) and (Comments.commenterId eq userId) }
            match.count() > 0
        }
    } catch (e: Exception) {
        logger.error { "Error checking who is comment poster" }
        false
    }
}

fun deleteCommentById(commentId: Long, requesterId: Long): Boolean {
    return if (isUserAdmin(requesterId) || isIdCommentPoster(requesterId, commentId)) {
        try {
            transaction {
                val success = Comments.deleteWhere { id eq commentId }
                success > 0 // Check if any rows were deleted
            }
        } catch (e: Exception) {
            println("Error deleting comment: $e")
            false
        }
    } else false

}

