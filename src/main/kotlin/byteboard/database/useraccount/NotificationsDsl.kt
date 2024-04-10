package byteboard.database.useraccount

import Messages.references
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table


object Notifications : Table(name = "Users") {
    val id: Column<Long> = long("id").autoIncrement()
    val read: Column<Boolean> = bool("read").default(false)
    val userId:Column<Long> = long("user_id").references(Users.id,ReferenceOption.CASCADE)



    override val primaryKey = PrimaryKey(id)
}
