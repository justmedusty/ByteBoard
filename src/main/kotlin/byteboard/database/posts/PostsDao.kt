package byteboard.database.posts

import byteboard.database.users.Users
import byteboard.database.users.logger
import org.jetbrains.exposed.sql.*
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.transactions.transaction

object Posts : Table(name = "Posts") {
    val id: Column<Long> = long("id").autoIncrement()
    val posterId: Column<Long> = long("posterId").references(Users.id)
    val topic: Column<String> = varchar("topic", 60)
    val timestamp: Column<LocalDateTime> = datetime("time_posted").defaultExpression(CurrentDateTime)


    override val primaryKey = PrimaryKey(id)
}


data class Post(
    val poster: Long, val topic: String, val timeStamp: LocalDateTime

)


fun createPost(userId: Long, content: String, topic: String): Boolean {
    return try {
        transaction {
            // Insert post into Posts table and get the generated postId
            val postId = insertAndGetId(userId, topic)


            if (postId != (-1).toLong() && addPostContents(content, postId)) {
                true
            } else {
                false
            }


        }
    }catch (e:Exception){
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