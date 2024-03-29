import byteboard.database.Posts.references
import byteboard.database.Users
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.*
object Messages : Table(name = "Messages") {
    private val id: Column<Int> = integer("id").autoIncrement()
    val senderId: Column<Long> = long("sender_id").references(Users.id)
    val receiverId: Column<Long> = long("receiver_id").references(Users.id)
    val message: Column<String> = text("message")
    val timeSent: Column<LocalDateTime> = datetime("time_sent").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}
