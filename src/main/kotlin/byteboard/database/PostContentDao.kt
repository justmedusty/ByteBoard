package byteboard.database

import byteboard.database.Posts.autoIncrement
import byteboard.database.Posts.references
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table


object PostContents: Table(name = "PostContents") {
    private val id: Column<Long> = long("id").autoIncrement()
    val content: Column<String> = text("postContent")
    val postId : Column<Long> = long("post").references(Posts.id, onDelete = ReferenceOption.CASCADE)



    override val primaryKey = PrimaryKey(id)
}


data class PostContent(
    val postContent: String,
    val postId: Long
    )