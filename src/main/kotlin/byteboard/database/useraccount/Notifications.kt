import Notifications.eventId
import Notifications.read
import Notifications.type
import byteboard.database.posts.Posts
import byteboard.database.useraccount.Users
import byteboard.database.useraccount.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object Notifications : Table(name = "Notifications") {
    val id: Column<Long> = long("id").autoIncrement()
    val read: Column<Boolean> = bool("read").default(false)
    val eventId: Column<Long> = long("id").references(Posts.id, onDelete = ReferenceOption.CASCADE)
    val userId: Column<Long> = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val type: Column<Long> = long("type")

    override val primaryKey = PrimaryKey(id)
}

data class Notification(
    val id: Long, val read: Boolean, val eventId: Long, val user: Long, val type: Long
)

fun insertPostNotification(id: Long, user: Long, notifType: Long): Boolean {
    return try {
        transaction {
            Notifications.insert {
                it[eventId] = id
                it[userId] = user
                it[type] = notifType
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}

fun getAllNotifications(page: Long, limit: Long, userId: Long): List<Notification>? {
    return try {
        val offset = ((page - 1) * limit)
        transaction {
            Notifications.select(Notifications.userId eq userId).limit(limit.toInt(),offset = offset).map {
                Notification(
                    it[Notifications.id],
                    it[read],
                    it[eventId],
                    userId,
                    it[type]
                )
            }

        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun markNotifRead(notif: Long): Boolean {

    return try {
        transaction {
            Notifications.update({ Notifications.id eq notif }) {
                it[read] = true
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun markNotifUnread(notif: Long): Boolean {

    return try {
        transaction {
            Notifications.update({ Notifications.id eq notif }) {
                it[read] = false
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

