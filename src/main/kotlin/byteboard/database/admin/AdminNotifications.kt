
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
object AdminNotifications : Table(name = "AdminNotifications") {
    val id: Column<Long> = long("id").autoIncrement()
    val read: Column<Boolean> = bool("read").default(false)
    val messageId: Column<Long> = long("message_id").references(Messages.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}