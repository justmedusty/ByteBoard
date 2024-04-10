package byteboard.database.comments

import byteboard.database.useraccount.Users
import byteboard.database.useraccount.isUserAdmin
import byteboard.database.useraccount.logger
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
,   val commentLikes : Long,
    val commentDislikes : Long,
    val lastEdited : LocalDateTime?,
    val isCommentLikedByMe : Boolean,
    val isCommentDislikedByMe : Boolean
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
        logger.error { e.message }
        false
    }
}

fun getCommentById(id: Long,userId: Long?): Comment? {
    return try {
        transaction {
            val commentLikes : Long = getLikesForComment(id)
            val commentDislikes : Long = getDislikesForComment(id)
            val lastEdited : LocalDateTime? = getLastCommentEdit(id)
            val isCommentLiked : Boolean = isCommentLikedByUser(id,userId)
            val isCommentDisliked : Boolean = isCommentLikedByUser(id,userId)

            Comments.select { Comments.id eq id }.singleOrNull()?.let {
                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    it[Comments.isReply],
                    it[Comments.parentCommentId],
                    it[Comments.timeStamp],
                    commentLikes,
                    commentDislikes,
                    lastEdited,
                    isCommentLiked,
                    isCommentDisliked
                )
            }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun getCommentsByPost(postId: Long, pageSize: Int, page: Int, userId: Long?, order: String?): List<Comment>? {
    var sortOrder: SortOrder?
    var orderByColumn = Comments.id
    var orderByCount: Count? = null

    when (order) {
        "oldest" -> {
            sortOrder = SortOrder.DESC
        }
        "newest" -> {
            sortOrder = SortOrder.ASC
        }
        "likes" -> {
            orderByCount = CommentLikes.commentId.count()
            sortOrder = SortOrder.DESC
        }
        "dislikes" -> {
            orderByCount = CommentDislikes.commentId.count()
            sortOrder = SortOrder.DESC
        }
        else -> {
            sortOrder = SortOrder.DESC
        }
    }

    return try {
        transaction {
            val query = Comments
                .slice(
                    Comments.id,
                    Comments.content,
                    Comments.postId,
                    Comments.commenterId,
                    Comments.isReply,
                    Comments.parentCommentId,
                    Comments.timeStamp
                )
                .select { Comments.postId eq postId }
                .limit(pageSize, offset = ((page - 1) * pageSize).toLong())

            if (orderByCount != null) {
                query
                    .orderBy(orderByCount, sortOrder)
                    .groupBy(Comments.id)
            } else {
                query
                    .orderBy(orderByColumn, sortOrder)
                    .groupBy(Comments.id)
            }

            query.map {
                val commentLikes: Long = getLikesForComment(it[Comments.id])
                val commentDislikes: Long = getDislikesForComment(it[Comments.id])
                val lastEdited: LocalDateTime? = getLastCommentEdit(it[Comments.id])
                val isCommentLiked: Boolean = isCommentLikedByUser(it[Comments.id], userId)
                val isCommentDisliked: Boolean = isCommentLikedByUser(it[Comments.id], userId)

                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    it[Comments.isReply],
                    it[Comments.parentCommentId],
                    it[Comments.timeStamp],
                    commentLikes,
                    commentDislikes,
                    lastEdited,
                    isCommentLiked,
                    isCommentDisliked
                )
            }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}
fun getCommentsByUser(userId: Long, pageSize: Int, page: Int,requesterId: Long?): List<Comment>? {
    return try {
        transaction {
            Comments.select { Comments.commenterId eq userId }.limit(pageSize, offset = ((page - 1) * pageSize).toLong()).map {
                val commentLikes : Long = getLikesForComment(it[Comments.id])
                val commentDislikes : Long = getDislikesForComment(it[Comments.id])
                val lastEdited : LocalDateTime? = getLastCommentEdit(it[Comments.id])
                val isCommentLiked : Boolean = isCommentLikedByUser(it[Comments.id],requesterId)
                val isCommentDisliked : Boolean = isCommentLikedByUser(it[Comments.id],requesterId)
                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    it[Comments.isReply],
                    it[Comments.parentCommentId],
                    it[Comments.timeStamp],
                    commentLikes,
                    commentDislikes,
                    lastEdited,
                    isCommentLiked,
                    isCommentDisliked
                )
            }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun getChildComments(commentId: Long, pageSize: Int, page: Int,requesterId: Long?): List<Comment>? {
    return try {
        transaction {
            Comments.select { Comments.parentCommentId eq commentId }.limit(pageSize, offset = ((page - 1) * pageSize).toLong()).map {
                val commentLikes : Long = getLikesForComment(it[Comments.id])
                val commentDislikes : Long = getDislikesForComment(it[Comments.id])
                val lastEdited : LocalDateTime? = getLastCommentEdit(it[Comments.id])
                val isCommentLiked : Boolean = isCommentLikedByUser(it[Comments.id],requesterId)
                val isCommentDisliked : Boolean = isCommentLikedByUser(it[Comments.id],requesterId)
                Comment(
                    it[Comments.id],
                    it[Comments.content],
                    it[Comments.postId],
                    it[Comments.commenterId],
                    it[Comments.isReply],
                    it[Comments.parentCommentId],
                    it[Comments.timeStamp],
                    commentLikes,
                    commentDislikes,
                    lastEdited,
                    isCommentLiked,
                    isCommentDisliked
                )
            }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
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
            logger.error { e.message }
            false
        }
    } else false

}


fun updateComment(userId: Long, commentId: Long, newComment: String): Boolean {
    return if (isIdCommentPoster(userId, commentId)) {
        try {
            transaction {
                Comments.update({ Comments.id eq commentId }) {
                    it[Comments.content] = newComment
                }
            }
            insertNewCommentEdit(commentId,userId)
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }
    } else {
        false
    }
}