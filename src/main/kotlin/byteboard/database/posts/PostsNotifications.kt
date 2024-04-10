package byteboard.database.posts

import Messages.autoIncrement
import Messages.bool
import Messages.default
import Messages.long
import Messages.references
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object PostNotifications : Table(name = "PostNotifications") {
    val id: Column<Long> = long("id").autoIncrement()
    val read: Column<Boolean> = bool("read").default(false)
    val postId: Column<Long> = long("post_id").references(Posts.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(id)
}