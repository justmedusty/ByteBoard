package byteboard.database.comments

import byteboard.database.useraccount.Users
import byteboard.database.useraccount.isUserAdmin
import byteboard.database.useraccount.logger
import byteboard.database.posts.Posts
import byteboard.database.useraccount.deleteUser
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CommentDislikes: Table(name = "CommentDislikes") {
    val id: Column<Long> = long("id").autoIncrement()
    val commentId: Column<Long> = long("commentId").references(Comments.id,ReferenceOption.CASCADE
    )
    val dislikedById : Column<Long> = long("dislikedById").references(Users.id)


    init {
        index(true, commentId, dislikedById)
    }



    override val primaryKey = PrimaryKey(id)
}


data class Dislike(
    val commentId: Long?,
    val dislikedById : Long
)

fun isCommentLikedByUser(commentId: Long,dislikedById: Long?): Boolean{
    return if (dislikedById == null){
        false
    }
    else try {
        transaction {
            val alreadyLiked =  CommentLikes.select{
               (CommentLikes.commentId eq commentId) and (CommentLikes.likedById eq dislikedById)
            }
            alreadyLiked.count() > 0

        }
    }catch (e:Exception){
        logger.error {  e.message}
        true
    }
}

fun getDislikesForComment(commentId: Long): Long {
    return try {
        transaction {
            CommentDislikes.select {
                (CommentDislikes.commentId eq commentId)
            }.count()
        }
    } catch (e: Exception) {
        logger.error { e.message }
        -1
    }
}

fun dislikeComment(dislikedById: Long, commentId: Long): Boolean {
    if (!isCommentLikedByUser(commentId, dislikedById)) return try {
        transaction {
            CommentDislikes.insert {
                it[CommentDislikes.commentId] = commentId
                it[CommentDislikes.dislikedById] = dislikedById
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
    if(!unlikeComment(dislikedById,commentId)) return false
    return try {
        transaction {
            CommentDislikes.insert {
                it[CommentDislikes.commentId] = commentId
                it[CommentDislikes.dislikedById] = dislikedById
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun isRequesterDislikeOwner(userId: Long, commentId: Long): Boolean {
    return try {
        transaction {
            val match =
                CommentDislikes.select { (CommentDislikes.commentId eq commentId) and (CommentDislikes.dislikedById eq userId) }
            match.count() > 0
        }
    } catch (e: Exception) {
        logger.error { "Error checking who is comment poster" }
        false
    }
}

fun unDislikeComment(requesterId: Long, commentId: Long): Boolean {
        try {
            return transaction {
                val success = CommentDislikes.deleteWhere { (dislikedById eq requesterId) and (CommentDislikes.commentId eq commentId) }
                success > 0
            }
        } catch (e: Exception) {
            logger.error { e.message }
            return false
        }
}


