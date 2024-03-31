import byteboard.database.Comments
import byteboard.database.Posts
import byteboard.database.Users
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Dislikes: Table(name = "Dislikes") {
    private val id: Column<Long> = long("id").autoIncrement()
    val postId : Column<Long> = long("post").references(Posts.id, ReferenceOption.CASCADE)
    val dislikeCount: Column<Long> = long("dislikeCount")
    val isCommentDislike : Column<Boolean> = bool("isCommentDislike").default(false)
    val commentId: Column<Long?> = long("commentId").references(Comments.id).nullable().default(null)
    val dislikedBy : Column<Long> = long("dislikedBy").references(Users.id)



    override val primaryKey = PrimaryKey(id)
}


data class Dislike(
    val content: String,
    val postId: Long,
    val commenterId: Long,
    val isCommentDislike: Boolean,
    val commentId: Long?,
    val dislikedBy : Long
)