package byteboard.database

import org.jetbrains.exposed.sql.*
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.CurrentDateTime

object Posts : Table(name = "Posts") {
    val id: Column<Long> = long("id").autoIncrement()
    val poster: Column<Long> = long("poster").references(Users.id)
    val topic : Column<String> = varchar("topic",60)
    val timestamp : Column<LocalDateTime> = datetime("time_posted").defaultExpression(CurrentDateTime)



    override val primaryKey = PrimaryKey(id)
}


data class Post(
    val poster: Long,
    val topic: String,
    val timeStamp : LocalDateTime

)