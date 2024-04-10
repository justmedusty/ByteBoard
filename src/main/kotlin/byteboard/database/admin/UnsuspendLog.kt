package byteboard.database.admin

import byteboard.database.useraccount.Users
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UnsuspendLog : Table(name = "UnsuspendLog") {
    val id: Column<Long> = long("id").autoIncrement()
    val unsuspendTimeStamp: Column<LocalDateTime> = datetime("unsuspendTimeStamp")
    val adminId: Column<Long> = long("message_id").references(Users.id)
    val reason : Column<String> = text("reason")

    override val primaryKey = PrimaryKey(id)
}