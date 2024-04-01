package byteboard.database.comments

import Messages.long
import byteboard.database.Users
import byteboard.database.Users.references
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object CommentEdits : Table(name = "CommentEdits") {
    val id: Column<Long> = long("id").autoIncrement()
    val commentId: Column<Long> = long("commentId").references(Comments.id)
    val posterId: Column<Long> = long("posterId").references(Users.id)
    val lastEdited : Column<LocalDateTime> = datetime("lastEdited")


    override val primaryKey = PrimaryKey(id)
}

data class CommentEdit(
    val commentId : Long,
    val posterId : Long,
    val lastEdited : LocalDateTime
)