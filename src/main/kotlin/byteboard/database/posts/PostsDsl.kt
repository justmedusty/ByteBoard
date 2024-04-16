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
    val lastedEdited: String?
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

fun fetchPostsByTopic(postTopic: String, page: Int, limit: Int, userId: Long, order: String?): List<Post>? {
    try {
        var orderByCount: Expression<Long>? = null
        var sortOrder: SortOrder = SortOrder.DESC

        when (order) {
            "old" -> sortOrder = SortOrder.ASC
            "new" -> sortOrder = SortOrder.DESC
            "liked" -> orderByCount = PostLikes.postId.count()
            "disliked" -> orderByCount = PostDislikes.postId.count()
        }

        return transaction {
            val queryParam = "%$postTopic%"
            val relevantPostIds = Posts.select { Posts.topic like queryParam }.map { it[Posts.id] }

            val query = Posts.innerJoin(PostContents, { Posts.id }, { PostContents.postId }).leftJoin(PostDislikes).leftJoin(PostLikes)
                .slice(
                    Posts.id,
                    Posts.posterId,
                    Posts.topic,
                    Posts.timestamp,
                    PostContents.title,
                    PostContents.content
                )
                .select { Posts.id inList relevantPostIds }

            if (orderByCount != null) {
                query
                    .groupBy(Posts.id, PostContents.title, PostContents.content)
                    .orderBy(orderByCount, sortOrder)
            } else {
                query
                    .groupBy(Posts.id, PostContents.title, PostContents.content)
                    .orderBy(Posts.id, sortOrder)
            }

            query.limit(limit, offset = ((page - 1) * limit).toLong())

            query.map {
                val postId = it[Posts.id]
                val posterUsername = it[Posts.posterId]
                val username = getUserName(posterUsername) ?: "Could not get username"
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
                    getLikesForPost(postId),
                    getDislikesForPost(postId),
                    isPostLikedByMe,
                    isPostDislikedByMe,
                    lastEdited.toString()
                )
            }
        }
    } catch (e: Exception) {
        logger.error { "Error fetching posts: ${e.message}" }
        return null
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
                        lastEdited.toString()

                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }
}

fun fetchPostsInteractedByMe(page: Int, limit: Int, topic: String, userId: Long, interactionType: String): List<Post> {
    val isLiked = interactionType == "like"
    val interactionColumn = if (isLiked) PostLikes.likedById else PostDislikes.dislikedById

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
            ).select(interactionColumn eq userId).groupBy(Posts.id).orderBy(Posts.id, SortOrder.DESC)
                .limit(limit, offset = ((page - 1) * limit).toLong()).map {
                    val postId = it[Posts.id]
                    val posterUsername = it[Posts.posterId]
                    val username = getUserName(posterUsername) ?: "Could not get username"
                    val lastEdited = checkLastPostEdit(postId)
                    val likedByMe = isLiked
                    val dislikedByMe = !isLiked
                    Post(
                        postId,
                        username,
                        it[Posts.topic],
                        it[Posts.timestamp].toString(),
                        it[PostContents.title],
                        it[PostContents.content],
                        it[PostLikes.postId.count()],
                        it[PostDislikes.postId.count()],
                        likedByMe,
                        dislikedByMe,
                        lastEdited.toString()
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        emptyList<Post>()
    }
}
fun fetchPosts(page: Int, limit: Int, userId: Long, order: String?): List<Post>? {
    try {
        var orderByCount: Expression<Long>? = null
        var sortOrder: SortOrder = SortOrder.DESC

        when (order) {
            "old" -> sortOrder = SortOrder.ASC
            "new" -> sortOrder = SortOrder.DESC
            "liked" -> orderByCount = PostLikes.postId.count()
            "disliked" -> orderByCount = PostDislikes.postId.count()
        }

        return transaction {
            val relevantPostIds = Posts.selectAll().map { it[Posts.id] }

            val query = Posts.innerJoin(PostContents, { Posts.id }, { PostContents.postId }).leftJoin(PostDislikes).leftJoin(PostLikes)
                .slice(
                    Posts.id,
                    Posts.posterId,
                    Posts.topic,
                    Posts.timestamp,
                    PostContents.title,
                    PostContents.content
                )
                .select { Posts.id inList relevantPostIds }

            if (orderByCount != null) {
                query
                    .groupBy(Posts.id, PostContents.title, PostContents.content)
                    .orderBy(orderByCount, sortOrder)
            } else {
                query
                    .groupBy(Posts.id, PostContents.title, PostContents.content)
                    .orderBy(Posts.id, sortOrder)
            }

            query.limit(limit, offset = ((page - 1) * limit).toLong())
            query.map {
                val postId = it[Posts.id]
                val posterUsername = it[Posts.posterId]
                val username = getUserName(posterUsername) ?: "Could not get username"
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
                    getLikesForPost(postId),
                    getDislikesForPost(postId),
                    isPostLikedByMe,
                    isPostDislikedByMe,
                    lastEdited.toString()
                )
            }
        }
    } catch (e: Exception) {
        logger.error { "Error fetching posts: ${e.message}" }
        return null
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
                    lastEdited.toString()
                )
            }

            posts

        }


    } catch (e: Exception) {
        logger.error("Error occurred while searching posts: ${e.message}")
        null
    }

}