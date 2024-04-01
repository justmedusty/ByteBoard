package byteboard.database.posts

import byteboard.database.comments.CommentLikes
import byteboard.database.Users
import byteboard.database.isUserAdmin
import byteboard.database.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object PostLikes : Table(name = "Likes") {
    val id: Column<Long> = long("id").autoIncrement()
    val postId: Column<Long> = long("post").references(Posts.id, ReferenceOption.CASCADE)
    val likedById: Column<Long> = long("likedById").references(Users.id)


    override val primaryKey = PrimaryKey(id)
}


data class Like(
    val postId: Long,
    val likedBy: Long
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
                it[PostLikes.likedById] = likedById
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun isRequesterPostLikeOwner(userId: Long, postId: Long): Boolean {
    return try {
        transaction {
            val match = PostLikes.select { (PostLikes.postId eq postId) and (PostLikes.likedById eq userId) }
            match.count() > 0
        }
    } catch (e: Exception) {
        logger.error { "Error checking who is comment poster" }
        false
    }
}

fun unlikePost(requesterId: Long, postId: Long, postLikeId: Long): Boolean {
    if (isRequesterPostLikeOwner(requesterId, postId) || isUserAdmin(requesterId)) {
        try {
            return transaction {
                val success = PostLikes.deleteWhere { id eq postLikeId }
                success > 0
            }
        } catch (e: Exception) {
            logger.error { e.message }
            return false
        }
    } else return false
}
