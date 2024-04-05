package byteboard.database.posts

import byteboard.database.useraccount.Users
import byteboard.database.useraccount.getUserName
import byteboard.database.useraccount.isUserAdmin
import byteboard.database.useraccount.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object Posts : Table(name = "Posts") {
    val id: Column<Long> = long("id").autoIncrement()
    val posterId: Column<Long> = long("posterId").references(Users.id)
    val topic: Column<String> = varchar("topic", 60)
    val timestamp: Column<LocalDateTime> = datetime("timestamp").defaultExpression(CurrentDateTime)


    override val primaryKey = PrimaryKey(id)
}


data class Post(
    val posterUserName: String,
    val topic: String,
    val timeStamp: LocalDateTime,
    val content: String,
    val likeCount: Long,
    val dislikeCount: Long,
    val likedByMe: Boolean,
    val lastedEdited: LocalDateTime?
)


fun createPost(userId: Long, content: String, topic: String, title: String): Boolean {
    return try {
        transaction {

            val postId = insertAndGetId(userId, topic)
            postId != (-1).toLong() && addPostContents(content, postId, title)


        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun insertAndGetId(poster: Long, postTopic: String): Long {
    return try {
        transaction {
            Posts.insert {
                it[posterId] = poster
                it[topic] = postTopic
                it[timestamp] = LocalDateTime.now()
            } get Posts.id
        }
    } catch (e: Exception) {
        logger.error { }
        -1
    }
}

fun verifyUserId(userId: Long, postId: Long): Boolean {
    return try {
        transaction {
            Posts.select {
                (Posts.id eq postId) and (Posts.posterId eq userId)

            }.count() > 0
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun deletePost(userId: Long, postId: Long): Boolean {
    return if (verifyUserId(userId, postId) || isUserAdmin(userId)) {
        try {
            Posts.deleteWhere { id eq postId }
            true
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }

    } else {
        false
    }
}


fun fetchPostsByTopic(postTopic: String, page: Int, limit: Int, userId: Long): List<Post> {
    return try {
        val relevantPostIds = Posts.select { Posts.topic eq postTopic }.map { it[Posts.id] }
        transaction {
            (Posts innerJoin PostLikes innerJoin PostDislikes innerJoin PostContents leftJoin PostEdits).slice(
                Posts.id,
                Posts.posterId,
                Posts.topic,
                Posts.timestamp,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count(),
                PostEdits.lastEdited
            ).select { Posts.id inList relevantPostIds }.groupBy(Posts.id).orderBy(Posts.id)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername)
                    if(username == null){ emptyList<Post>() }
                    val isPostLikedByMe = isPostLikedByUser(postId, userId)
                    val lastEdited = checkLastPostEdit(postId, userId)
                    Post(
                        username!!,
                        it[Posts.topic],
                        it[Posts.timestamp],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        lastEdited

                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }
}


fun fetchMostRecentPosts(page: Int, limit: Int, userId: Long): List<Post> {
    return try {
        transaction {
            (Posts innerJoin PostLikes innerJoin PostDislikes innerJoin PostContents leftJoin PostEdits).slice(
                Posts.id,
                Posts.posterId,
                Posts.topic,
                Posts.timestamp,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).selectAll().groupBy(Posts.id).orderBy(Posts.id, SortOrder.DESC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername)
                    if (username == null) {
                        emptyList<Post>()
                    }
                    val isPostLikedByMe = isPostLikedByUser(postId, userId)
                    val lastEdited = checkLastPostEdit(postId, userId)
                    Post(
                        username!!,
                        it[Posts.topic],
                        it[Posts.timestamp],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        lastEdited

                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }
}

fun fetchMostDislikedPosts(page: Int, limit: Int, userId: Long): List<Post> {
    return try {
        transaction {
            (Posts innerJoin PostLikes innerJoin PostDislikes innerJoin PostContents leftJoin PostEdits).slice(
                Posts.id,
                Posts.posterId,
                Posts.topic,
                Posts.timestamp,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).selectAll().groupBy(Posts.id).orderBy(PostDislikes.id.count(), SortOrder.DESC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername)
                    if (username == null) {
                        emptyList<Post>()
                    }
                    val isPostLikedByMe = isPostLikedByUser(postId, userId)
                    val lastEdited = checkLastPostEdit(postId, userId)
                    Post(
                        username!!,
                        it[Posts.topic],
                        it[Posts.timestamp],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        lastEdited

                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }
}

fun fetchMostLikedPosts(page: Int, limit: Int, userId: Long): List<Post> {
    return try {
        transaction {
            (Posts innerJoin PostLikes innerJoin PostDislikes innerJoin PostContents leftJoin PostEdits).slice(
                Posts.id,
                Posts.posterId,
                Posts.topic,
                Posts.timestamp,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).selectAll().groupBy(Posts.id).orderBy(PostLikes.id.count(), SortOrder.DESC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername)
                    if (username == null) {
                       emptyList<Post>()
                    }
                    val isPostLikedByMe = isPostLikedByUser(postId, userId)
                    val lastEdited = checkLastPostEdit(postId, userId)

                    Post(
                        username!!,
                        it[Posts.topic],
                        it[Posts.timestamp],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        lastEdited

                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList()
    }
}

fun fetchPostsFromUser(page: Int, limit: Int, userId: Long): List<Post> {
    return try {
        transaction {
            (Posts innerJoin PostLikes innerJoin PostDislikes innerJoin PostContents leftJoin PostEdits).slice(
                Posts.id,
                Posts.posterId,
                Posts.topic,
                Posts.timestamp,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).select(Posts.posterId eq userId).groupBy(Posts.id).orderBy(PostLikes.id.count(), SortOrder.DESC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername)
                    if(username == null){
                        emptyList<Post>()
                    }
                    val isPostLikedByMe = isPostLikedByUser(postId, userId)
                    val lastEdited = checkLastPostEdit(postId, userId)
                    Post(
                        username!!,
                        it[Posts.topic],
                        it[Posts.timestamp],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        lastEdited

                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }
}









