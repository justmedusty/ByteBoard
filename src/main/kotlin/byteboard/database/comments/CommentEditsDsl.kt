package byteboard.database.comments

import byteboard.database.useraccount.Users
import byteboard.database.useraccount.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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

fun getLastCommentEdit(commentId: Long) : LocalDateTime?{
    return try {
        transaction {
            val result = CommentEdits.select { CommentEdits.commentId eq commentId }
                .orderBy(CommentEdits.lastEdited, SortOrder.DESC)
                .limit(1)
                .firstOrNull()

            result?.get(CommentEdits.lastEdited)
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}