package byteboard.database.posts

import byteboard.database.Users
import byteboard.database.comments.Comments
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object PostEdits : Table(name = "PostEdits") {
    val id: Column<Long> = long("id").autoIncrement()
    val postId: Column<Long> = long("commentId").references(Comments.id)
    val posterId: Column<Long> = long("posterId").references(Users.id)
    val lastEdited: Column<LocalDateTime> = datetime("lastEdited")


    override val primaryKey = PrimaryKey(id)
}

data class PostEdit(
    val commentId : Long,
    val posterId : Long,
    val lastEdited : LocalDateTime
)


