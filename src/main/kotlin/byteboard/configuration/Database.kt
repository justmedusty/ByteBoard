package byteboard.configuration

import Messages
import byteboard.database.admin.AdminLogs
import byteboard.database.admin.SuspendLog
import byteboard.database.comments.*
import byteboard.database.posts.*
import byteboard.database.useraccount.MessageNotifications
import byteboard.database.useraccount.ProfileData
import byteboard.database.useraccount.Users
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Configure database
 *
 */
fun Application.configureDatabase() {
    val url = System.getenv("POSTGRES_URL")
    val user = System.getenv("POSTGRES_USER")
    val password = System.getenv("POSTGRES_PASSWORD")

    try {
        Database.connect(url, driver = "org.postgresql.Driver", user = user, password = password)
    } catch (e: Exception) {
        println(e)
    }

    transaction {
        SchemaUtils.create(
            Users,
            Comments,
            CommentNotifications
            ,CommentEdits
            ,CommentLikes
            ,CommentDislikes
            ,ProfileData
            ,Posts,
            PostLikes,
            PostDislikes,
            PostNotifications,
            PostContents,
            PostEdits,
            AdminLogs,
            SuspendLog,
            MessageNotifications,
            Messages)
    }
}