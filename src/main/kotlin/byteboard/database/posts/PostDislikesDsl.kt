package byteboard.database.posts

import byteboard.database.useraccount.Users
import byteboard.database.useraccount.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object PostDislikes: Table(name = "Dislikes") {
    val id: Column<Long> = long("id").autoIncrement()
    val postId : Column<Long> = long("post").references(Posts.id, ReferenceOption.CASCADE)
    val dislikedById : Column<Long> = long("dislikedBy").references(Users.id)

    init {
        index(true, postId, dislikedById)
    }



    override val primaryKey = PrimaryKey(id)
}


data class PostDislike(
    val postId: Long,
    val dislikedById : Long
)


fun getDislikesForPost(postId: Long): Long {
    return try {
        transaction {
            PostDislikes.select {
                (PostDislikes.postId eq postId)
            }.count()
        }
    } catch (e: Exception) {
        println("Error getting likes for post: $e")
        -1
    }
}

fun isPostDislikedByUser(postId: Long, userId: Long): Boolean {
    return try {
        transaction {
         PostDislikes.select((PostDislikes.postId eq postId) and (PostDislikes.dislikedById eq userId)).count() > 0
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }


}
fun dislikePost(likedById: Long, postId: Long): Boolean {
    return try {
        transaction {
            PostDislikes.insert {
                it[PostDislikes.postId] = postId
                it[PostDislikes.dislikedById] = likedById
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun isRequesterPostDislikeOwner(userId: Long, postId: Long): Boolean {
    return try {
        transaction {
            val match = PostLikes.select { (PostDislikes.postId eq postId) and (PostDislikes.dislikedById eq userId) }
            match.count() > 0
        }
    } catch (e: Exception) {
        logger.error { "Error checking who is comment poster" }
        false
    }
}

fun unDislikePost(userId: Long, postId: Long): Boolean {
        try {
            return transaction {
                val success = PostDislikes.deleteWhere { (PostDislikes.postId eq postId) and (PostDislikes.dislikedById eq userId) }
                success > 0
            }
        } catch (e: Exception) {
            logger.error { e.message }
            return false
        }
}
