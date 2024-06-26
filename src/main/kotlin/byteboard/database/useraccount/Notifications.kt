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
    val eventId: Column<Long> = long("event_id").references(Posts.id, onDelete = ReferenceOption.CASCADE)
    val userId: Column<Long> = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val type: Column<Long> = long("type")

    override val primaryKey = PrimaryKey(id)
}

data class Notification(
    val id: Long,
    val read: Boolean,
    val eventId: Long,
    val user: Long,
    val type: Long
)

fun insertNotification(id: Long, user: Long, notifType: Long): Boolean {
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
        transaction {
            Notifications.select(Notifications.userId eq userId).limit(limit.toInt(), (page - 1) * limit.toLong()).map {
                Notification(
                    it[Notifications.id],
                    it[Notifications.read],
                    it[Notifications.eventId],
                    userId,
                    it[Notifications.type]
                )
            }

        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun markNotifRead(notif: Long,user: Long): Boolean {

    return try {
        transaction {
            Notifications.update({ (Notifications.id eq notif) and (Notifications.userId eq user) }) {
                it[read] = true
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

fun markNotifUnread(notif: Long,user: Long): Boolean {

    return try {
        transaction {
            Notifications.update({ (Notifications.id eq notif) and (Notifications.userId eq user) }) {
                it[read] = false
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }
}

