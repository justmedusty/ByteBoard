package byteboard.database.comments

import Messages.long
import byteboard.database.Users
import byteboard.database.Users.references
import byteboard.database.logger
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
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

fun insertNewCommentEdit(commentsId: Long,userId: Long): Boolean{
    return try {
        transaction {
            CommentEdits.insert {
                it[commentId] = commentsId
                it[posterId]= userId
                it[lastEdited] = LocalDateTime.now()
            }
            true
        }
    }catch (e:Exception){
        logger.error { e.message }
        false
    }
}
