import byteboard.database.Comments
import byteboard.database.Comments.autoIncrement
import byteboard.database.Comments.references
import byteboard.database.Posts
import byteboard.database.Users
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Likes: Table(name = "Likes") {
    private val id: Column<Long> = long("id").autoIncrement()
    val postId : Column<Long> = long("post").references(Posts.id, ReferenceOption.CASCADE)
    val likeCount: Column<Long> = long("likeCount")
    val isCommentLike : Column<Boolean> = bool("isCommentLike").default(false)
    val commentId: Column<Long?> = long("commentId").references(Comments.id).nullable().default(null)
    val likedBy : Column<Long> = long("likedBy").references(Users.id)



    override val primaryKey = PrimaryKey(id)
}


data class Like(
    val content: String,
    val postId: Long,
    val commenterId: Long,
    val isCommentLike: Boolean,
    val commentId: Long?,
    val likedBy : Long
)