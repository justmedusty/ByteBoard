package byteboard.database

import byteboard.database.PostContents.autoIncrement
import byteboard.database.PostContents.references
import byteboard.database.Posts.defaultExpression
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Comments: Table(name = "Comments") {
    val id: Column<Long> = long("id").autoIncrement()
    val content: Column<String> = text("postContent")
    val postId : Column<Long> = long("post").references(Posts.id,ReferenceOption.CASCADE)
    val commenterId: Column<Long> = long("commenterId").references(Users.id)
    val isReply : Column<Boolean> = bool("isReply").default(false)
    val parentCommentId : Column<Long?> = long("parentCommentId").references(Comments.id, onDelete = ReferenceOption.CASCADE).nullable().default(null)
    val timestamp : Column<LocalDateTime> = datetime("time_posted").defaultExpression(CurrentDateTime)




    override val primaryKey = PrimaryKey(id)
}


data class Comment(
    val content: String,
    val postId: Long,
    val commenterId: Long,
    val isReply: Boolean,
    val parentCommentId : Long?,
    val timeStamp : LocalDateTime
)