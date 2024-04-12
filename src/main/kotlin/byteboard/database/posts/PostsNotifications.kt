package byteboard.database.posts

import byteboard.database.useraccount.Users
import byteboard.database.useraccount.logger
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction


object PostNotifications : Table(name = "PostNotifications") {
    val id: Column<Long> = long("id").autoIncrement()
    val read: Column<Boolean> = bool("read").default(false)
    val postId: Column<Long> = long("post_id").references(Posts.id, onDelete = ReferenceOption.CASCADE)
    val userId: Column<Long> = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}

fun insertPostNotification(post: Long, user: Long): Boolean {
    return try {
        transaction {
            PostNotifications.insert {
                it[postId] = post
                it[userId] = user
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}

