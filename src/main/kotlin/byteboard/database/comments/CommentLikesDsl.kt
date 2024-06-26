package byteboard.database.comments

import byteboard.database.useraccount.Users
import byteboard.database.useraccount.isUserAdmin
import byteboard.database.useraccount.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CommentLikes : Table(name = "CommentLikes") {
    val id: Column<Long> = long("id").autoIncrement()
    val commentId: Column<Long> = long("commentId").references(Comments.id, ReferenceOption.CASCADE)
    val likedById: Column<Long> = long("likedById").references(Users.id)

    init {
        index(true, commentId, likedById)
    }

    override val primaryKey = PrimaryKey(id)
}

fun isCommentDisLikedByUser(commentId: Long, likedById: Long?): Boolean {
    return if (likedById == null) {
        return false
    } else {
        try {
            transaction {
                val alreadyLiked = CommentDislikes.select {
                    (CommentDislikes.commentId eq commentId) and (CommentDislikes.dislikedById eq likedById)
                }
                alreadyLiked.count() > 0

            }
        } catch (e: Exception) {
            logger.error { e.message }
            true
        }
    }

}

fun getLikesForComment(commentId: Long): Long {
    return try {
        transaction {
            CommentLikes.select {
                (CommentLikes.commentId eq commentId)
            }.count()
        }
    } catch (e: Exception) {
        logger.error { e.message }
        -1
    }
}

fun likeComment(likedById: Long, commentId: Long): Boolean {

    if (!isCommentDisLikedByUser(commentId, likedById)) return try {
        transaction {
            CommentLikes.insert {
                it[CommentLikes.commentId] = commentId
                it[CommentLikes.likedById] = likedById
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
    if(!unDislikeComment(likedById, commentId)) return false
    return try {
        transaction {
            CommentLikes.insert {
                it[CommentLikes.commentId] = commentId
                it[CommentLikes.likedById] = likedById
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun isRequesterLikeOwner(userId: Long, commentId: Long): Boolean {
    return try {
        transaction {
            val match =
                CommentLikes.select { (CommentLikes.commentId eq commentId) and (CommentLikes.likedById eq userId) }
            match.count() > 0
        }
    } catch (e: Exception) {
        logger.error { "Error checking who is comment poster" }
        false
    }
}

fun unlikeComment(requesterId: Long, commentId: Long): Boolean {
    return try {
            transaction {
                val success = CommentLikes.deleteWhere { (likedById eq requesterId) and (CommentLikes.commentId eq commentId) }
                success > 0
            }
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }
}




