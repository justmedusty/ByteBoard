package byteboard.database.admin

import byteboard.database.useraccount.Users
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object SuspendLog : Table(name = "SuspendLog") {
    val id: Column<Long> = long("id").autoIncrement()
    val suspendTimeStamp: Column<LocalDateTime> = datetime("suspendTime")
    val adminId: Column<Long> = long("message_id").references(Users.id)
    val reason : Column<String> = text("reason")

    override val primaryKey = PrimaryKey(id)
}


