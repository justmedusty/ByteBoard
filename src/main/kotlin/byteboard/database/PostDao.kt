package byteboard.database

import byteboard.database.Posts.references
import mu.KotlinLogging
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object Posts : Table(name = "Posts") {
    val id: Column<Long> = long("id").autoIncrement()
    val poster: Column<Long> = long("poster").references(Users.id)
    val topic : Column<String> = varchar("topic",60)


    override val primaryKey = PrimaryKey(id)
}


data class Post(
    val poster: Long,
    val topic: String,

)