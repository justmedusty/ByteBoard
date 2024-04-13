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
    val id: Long,
    val posterUserName: String,
    val topic: String,
    val timeStamp: String,
    val title: String,
    val content: String,
    val likeCount: Long,
    val dislikeCount: Long,
    val likedByMe: Boolean,
    val dislikedByMe: Boolean,
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

fun getPostOwnerId(postId: Long): Long? {
    return try {
        transaction {
            val result = Posts.select { Posts.id eq postId }.singleOrNull()
            result?.get(Posts.posterId)
        }
    } catch (e: Exception) {
        null
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
                PostContents.title,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count(),
                PostEdits.lastEdited
            ).select { Posts.id inList relevantPostIds }.groupBy(Posts.id).orderBy(Posts.id)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: return@transaction emptyList<Post>()
                    val isPostLikedByMe = isPostLikedByUser(postId, userId)
                    val isPostDislikedByMe = isPostDislikedByUser(postId, userId)
                    val lastEdited = checkLastPostEdit(postId)
                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        isPostDislikedByMe,
                        lastEdited

                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }
}


fun fetchMostRecentPosts(page: Int, limit: Int, userId: Long?): List<Post> {
    return try {
        transaction {
            (Posts innerJoin PostLikes innerJoin PostDislikes innerJoin PostContents leftJoin PostEdits).slice(
                Posts.id,
                Posts.posterId,
                Posts.topic,
                Posts.timestamp,
                PostContents.title,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).selectAll().groupBy(Posts.id).orderBy(Posts.id, SortOrder.DESC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterId = it[Posts.posterId]
                    val username = getUserName(posterId) ?: "Error getting username"
                    val isPostLikedByMe : Boolean
                    val isPostDislikedByMe : Boolean

                    if(userId ==null){
                        isPostDislikedByMe = false
                        isPostLikedByMe = false
                    }else{
                        isPostLikedByMe =  isPostLikedByUser(postId, userId)
                        isPostDislikedByMe = isPostDislikedByUser(postId, userId)
                    }

                    val lastEdited = checkLastPostEdit(postId)
                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        isPostDislikedByMe,
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
                PostContents.title,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).selectAll().groupBy(Posts.id).orderBy(PostDislikes.id.count(), SortOrder.DESC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: return@transaction emptyList<Post>()
                    val isPostLikedByMe = isPostLikedByUser(postId, userId)
                    val isPostDislikedByMe = isPostDislikedByUser(postId, userId)
                    val lastEdited = checkLastPostEdit(postId)
                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        isPostDislikedByMe,
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
                PostContents.title,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).selectAll().groupBy(Posts.id).orderBy(PostLikes.id.count(), SortOrder.DESC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: return@transaction emptyList<Post>()
                    val isPostLikedByMe = isPostLikedByUser(postId, userId)
                    val isPostDislikedByMe = isPostDislikedByUser(postId, userId)
                    val lastEdited = checkLastPostEdit(postId)

                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        isPostDislikedByMe,
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
                PostContents.title,
                Posts.timestamp,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).select(Posts.posterId eq userId).groupBy(Posts.id).orderBy(PostLikes.id.count(), SortOrder.DESC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: return@transaction emptyList<Post>()
                    val isPostLikedByMe = isPostLikedByUser(postId, userId)
                    val isPostDislikedByMe = isPostDislikedByUser(postId, userId)
                    val lastEdited = checkLastPostEdit(postId)
                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        isPostDislikedByMe,
                        lastEdited

                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }
}

fun fetchPostsByTopicAndLikes(page: Int, limit: Int, topic: String, userId: Long?): List<Post> {
    return try {
        transaction {
            (Posts innerJoin PostLikes innerJoin PostDislikes innerJoin PostContents leftJoin PostEdits).slice(
                Posts.id,
                Posts.posterId,
                Posts.topic,
                PostContents.title,
                Posts.timestamp,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).select(Posts.topic eq topic).groupBy(Posts.id).orderBy(PostLikes.postId.count(), SortOrder.DESC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: return@transaction emptyList()

                    val isPostLikedByMe: Boolean = if (userId == null) {
                        false
                    } else {
                        isPostLikedByUser(postId, userId)
                    }

                    val isPostDislikedByMe: Boolean = if (userId == null) {
                        false
                    } else {
                        isPostDislikedByUser(postId, userId)
                    }

                    val lastEdited = checkLastPostEdit(postId)
                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        isPostDislikedByMe,
                        lastEdited
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }
}

fun fetchPostsByTopicAndDislikes(page: Int, limit: Int, topic: String, userId: Long?): List<Post> {
    return try {
        transaction {
            (Posts innerJoin PostLikes innerJoin PostDislikes innerJoin PostContents leftJoin PostEdits).slice(
                Posts.id,
                Posts.posterId,
                Posts.topic,
                Posts.timestamp,
                PostContents.title,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).select(Posts.topic eq topic).groupBy(Posts.id).orderBy(PostDislikes.postId.count(), SortOrder.DESC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: return@transaction emptyList<Post>()
                    val isPostLikedByMe: Boolean = if (userId == null) {
                        false
                    } else {
                        isPostLikedByUser(postId, userId)
                    }
                    val isPostDislikedByMe: Boolean = if (userId == null) {
                        false
                    } else {
                        isPostDislikedByUser(postId, userId)
                    }

                    val lastEdited = checkLastPostEdit(postId)
                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        isPostDislikedByMe,

                        lastEdited
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }

}


fun fetchPostsByOldestAndTopic(page: Int, limit: Int, topic: String, userId: Long?): List<Post> {
    return try {
        transaction {
            (Posts innerJoin PostLikes innerJoin PostDislikes innerJoin PostContents leftJoin PostEdits).slice(
                Posts.id,
                Posts.posterId,
                Posts.topic,
                Posts.timestamp,
                PostContents.title,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).select(Posts.topic eq topic).groupBy(Posts.id).orderBy(Posts.id, SortOrder.ASC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: return@transaction emptyList<Post>()
                    val isPostLikedByMe: Boolean = if (userId == null) {
                        false
                    } else {
                        isPostLikedByUser(postId, userId)
                    }
                    val isPostDislikedByMe: Boolean = if (userId == null) {
                        false
                    } else {
                        isPostDislikedByUser(postId, userId)
                    }

                    val lastEdited = checkLastPostEdit(postId)
                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        isPostDislikedByMe,
                        lastEdited
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }

}

fun fetchPostsLikedByMe(page: Int, limit: Int, topic: String, userId: Long): List<Post> {
    return try {
        transaction {
            (Posts innerJoin PostLikes innerJoin PostDislikes innerJoin PostContents leftJoin PostEdits).slice(
                Posts.id,
                Posts.posterId,
                Posts.topic,
                Posts.timestamp,
                PostContents.title,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).select(PostLikes.likedById eq userId).groupBy(Posts.id).orderBy(Posts.id, SortOrder.DESC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: return@transaction emptyList<Post>()
                    val lastEdited = checkLastPostEdit(postId)
                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        likedByMe = true,
                        dislikedByMe = false,
                        lastedEdited = lastEdited
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }

}

fun fetchPostsByDislikedByMe(page: Int, limit: Int, topic: String, userId: Long): List<Post> {
    return try {
        transaction {
            (Posts innerJoin PostLikes innerJoin PostDislikes innerJoin PostContents leftJoin PostEdits).slice(
                Posts.id,
                Posts.posterId,
                Posts.topic,
                Posts.timestamp,
                PostContents.title,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).select(PostDislikes.dislikedById eq userId).groupBy(Posts.id).orderBy(Posts.id, SortOrder.DESC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: return@transaction emptyList<Post>()
                    val lastEdited = checkLastPostEdit(postId)
                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        likedByMe = false,
                        dislikedByMe = true,
                        lastedEdited = lastEdited
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }
}

fun fetchPostsByOldest(page: Int, limit: Int, userId: Long?): List<Post> {
    return try {
        transaction {
            (Posts innerJoin PostLikes innerJoin PostDislikes innerJoin PostContents leftJoin PostEdits).slice(
                Posts.id,
                Posts.posterId,
                Posts.topic,
                Posts.timestamp,
                PostContents.title,
                PostContents.content,
                PostLikes.postId.count(),
                PostDislikes.postId.count()
            ).selectAll().groupBy(Posts.id).orderBy(Posts.id, SortOrder.ASC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: "Error getting username"
                    val isPostLikedByMe: Boolean = if (userId == null) {
                        false
                    } else {
                        isPostLikedByUser(postId, userId)
                    }

                    val isPostDislikedByMe: Boolean = if (userId == null) {
                        false
                    } else {
                        isPostDislikedByUser(postId, userId)
                    }
                    val lastEdited = checkLastPostEdit(postId)
                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        isPostLikedByMe,
                        isPostDislikedByMe,
                        lastedEdited = lastEdited
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }

}
fun searchPostByTitleOrContents(userId: Long?, queryParam: String, limit: Int, page: Int): List<Post>? {
    return try {

        transaction {
            val query = "%$queryParam%"
            val postsWithContents = (Posts innerJoin PostContents)
                .slice(
                    Posts.id,
                    Posts.posterId,
                    Posts.topic,
                    Posts.timestamp,
                    PostContents.title,
                    PostContents.content
                )
                .select { (PostContents.title like query) or (PostContents.content like query) }

            if (userId != null) {
                postsWithContents.andWhere { Posts.posterId eq userId }
            }

            val paginatedQuery = postsWithContents
                .limit(limit, (page - 1) * limit.toLong())
                .orderBy(Posts.id, SortOrder.DESC)

            val posts = paginatedQuery.map { row ->
                val postId = row[Posts.id]
                val posterUserId = row[Posts.posterId]
                val username = getUserName(posterUserId) ?: "Error occurred getting username"
                val isLikedByMe = userId != null && isPostLikedByUser(postId, userId)
                val isDislikedByMe = userId != null && isPostDislikedByUser(postId, userId)
                val lastEdited = checkLastPostEdit(postId)

                Post(
                    postId,
                    username,
                    row[Posts.topic],
                    row[Posts.timestamp].toString(),
                    row[PostContents.title],
                    row[PostContents.content],
                    getLikesForPost(postId),
                    getDislikesForPost(postId),
                    isLikedByMe,
                    isDislikedByMe,
                    lastEdited
                )
            }

            posts

        }


    } catch (e: Exception) {
        logger.error("Error occurred while searching posts: ${e.message}")
        null
    }

}