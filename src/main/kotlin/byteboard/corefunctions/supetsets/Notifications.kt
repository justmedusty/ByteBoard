package byteboard.corefunctions.supetsets

sealed class Notification {
    abstract val id: Long
    abstract val read: Boolean
    abstract val userId: Long
}

data class CommentNotification(
    override val id: Long,
    override val read: Boolean,
    override val userId: Long,
    val commentId: Long
) : Notification()

data class PostNotification(
    override val id: Long,
    override val read: Boolean,
    override val userId: Long,
    val postId: Long
) : Notification()

