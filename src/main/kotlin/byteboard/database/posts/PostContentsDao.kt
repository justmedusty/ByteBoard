package byteboard.database.posts

import byteboard.database.users.logger
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction


object PostContents : Table(name = "PostContents") {
    val id: Column<Long> = long("id").autoIncrement()
    val content: Column<String> = text("postContent")
    val postId: Column<Long> = long("post").references(Posts.id, onDelete = ReferenceOption.CASCADE)


    override val primaryKey = PrimaryKey(id)
}

data class PostContent(
    val postContent: String, val postId: Long
)


fun addPostContents(postContent: String, id: Long): Boolean {
    return try {
        transaction {
            PostContents.insert {
                it[content] = postContent
                it[postId] = id
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}