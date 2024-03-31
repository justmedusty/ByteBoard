import byteboard.database.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object PostLikes : Table(name = "Likes") {
    private val id: Column<Long> = long("id").autoIncrement()
    val postId: Column<Long> = long("post").references(Posts.id, ReferenceOption.CASCADE)
    val likedBy: Column<Long> = long("likedBy").references(Users.id)


    override val primaryKey = PrimaryKey(id)
}


data class Like(
    val postId: Long, val likedBy: Long
)

fun getLikesForPost(postId: Long): Long {
    return try {
        transaction {
            PostLikes.select {
                (PostLikes.postId eq postId)
            }.count()
        }
    } catch (e: Exception) {
        println("Error getting likes for post: $e")
        -1
    }
}

fun likePost(likedById: Long, postId: Long): Boolean {
    return try {
        transaction {
            CommentLikes.insert {
                it[PostLikes.postId] = postId
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
