
import byteboard.database.admin.SuspendLog
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
object AdminNotifications : Table(name = "AdminNotifications") {
    val id: Column<Long> = long("id").autoIncrement()
    val read: Column<Boolean> = bool("read").default(false)
    val suspendId: Column<Long?> = long("suspendId").references(SuspendLog.id).nullable()

    override val primaryKey = PrimaryKey(id)
}
