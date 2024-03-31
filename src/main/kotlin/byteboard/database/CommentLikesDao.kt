package byteboard.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CommentLikes : Table(name = "Likes") {
    val id: Column<Long> = long("id").autoIncrement()
    val commentId: Column<Long?> = long("commentId").references(Comments.id).nullable().default(null)
    val likedById: Column<Long> = long("likedById").references(Users.id)


    override val primaryKey = PrimaryKey(id)
}


data class CommentLike(
    val id : Long ,
    val commentId: Long,
    val likedById : Long
)

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

fun unlikeComment(requesterId: Long, commentId: Long, commentLikesId: Long): Boolean {
    if (isRequesterLikeOwner(requesterId, commentId) || isUserAdmin(requesterId)) {
        try {
            return transaction {
                val success = CommentLikes.deleteWhere { id eq commentLikesId }
                success > 0
            }
        } catch (e: Exception) {
            logger.error { e.message }
            return false
        }
    } else return false
}
