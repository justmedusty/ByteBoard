package byteboard.database.comments

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object CommentNotifications : Table(name = "CommentNotifications") {
    val id: Column<Long> = long("id").autoIncrement()
    val read: Column<Boolean> = bool("read").default(false)
    val commentId: Column<Long> = long("comment_id").references(Comments.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}

