package byteboard.database.useraccount

import Messages
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object MessageNotifications : Table(name = "MessageNotifications") {
    val id: Column<Long> = long("id").autoIncrement()
    val read: Column<Boolean> = bool("read").default(false)
    val messageId: Column<Long> = long("message_id").references(Messages.id, onDelete = ReferenceOption.CASCADE)
    val userId: Column<Long> = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}

data class MessagesNotification(
    val message: Long,
    val read: Boolean,

    )

fun insertMessageNotification(message: Long, user: Long): Boolean {
    return try {
        transaction {
            MessageNotifications.insert {
                it[messageId] = message
                it[userId] = user
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}
