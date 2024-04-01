package byteboard.database.posts

import byteboard.database.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


object PostContents : Table(name = "PostContents") {
    val id: Column<Long> = long("id").autoIncrement()
    var title: Column<String> = text("title")
    val content: Column<String> = text("postContent")
    val postId: Column<Long> = long("post").references(Posts.id, onDelete = ReferenceOption.CASCADE)


    override val primaryKey = PrimaryKey(id)
}

data class PostContent(
    val postContent: String, val postId: Long, val title: String
)


fun addPostContents(postContent: String, id: Long, postTitle: String): Boolean {
    return try {
        transaction {
            PostContents.insert {
                it[content] = postContent
                it[postId] = id
                it[title] = postTitle
            }
            true
        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}


fun updatePostContents(newTitle: String?, newContent: String?, postId: Long): Boolean {
    if (newTitle == null && newContent == null) {
        return false
    }
    if (newTitle != null && newContent != null) {
        try {
            transaction {
                PostContents.update({ PostContents.id eq postId }) {
                    it[title] = newTitle
                    it[content] = newContent
                }
            }
            return true

        } catch (e: Exception) {
            logger.error { e.message }
            return false
        }
    } else if (newTitle != null) {
        try {
            transaction {
                PostContents.update({ PostContents.id eq postId }) {
                    it[title] = newTitle
                }
            }
            return true

        } catch (e: Exception) {
            logger.error { e.message }
            return false
        }

    } else if (newContent != null) {
        try {
            transaction {
                PostContents.update({ PostContents.id eq postId }) {
                    it[content] = newContent
                }
            }
            return true

        } catch (e: Exception) {
            logger.error { e.message }
            return false
        }


    }
    return false
}