package byteboard.database.admin

import byteboard.database.useraccount.Users
import byteboard.database.useraccount.isUserAdmin
import byteboard.database.useraccount.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object AdminLogs : Table(name = "AdminLogs") {
    val id: Column<Long> = long("id").autoIncrement()
    val timestamp: Column<LocalDateTime> = datetime("timestamp")
    val userId: Column<Long> = long("message_id").references(Users.id)
    val doneById: Column<Long> = long("done_by_id")
    //true for added, false for removed
    val added: Column<Boolean> = bool("added")
    val reason: Column<String> = text("reason")

    override val primaryKey = PrimaryKey(id)
}


data class AdminLog(
    val id: Long,
    val timestamp: LocalDateTime,
    val userId: Long,
    val doneById: Long,
    val added: Boolean,
    val reason: String
)

fun insertAdminLogEntry(user: Long,doneBy: Long,reasonString: String, addedBool: Boolean) : Boolean{

    if(!isUserAdmin(user)){
        return false
    }

    return try {
        transaction {
            AdminLogs.insert {
                it[timestamp] = LocalDateTime.now()
                it[userId] = user
                it[doneById] = doneBy
                it[added] = addedBool
                it[reason] = reasonString
            }.insertedCount > 0
        }

    }catch (e:Exception){
        logger.error { e.message }
        false
    }

}

fun getAdminLogEntries(page: Int, limit: Int, userId: Long, order: String?): List<AdminLog>? {

    if(!isUserAdmin(userId)){
        return null
    }

    val orderBy: SortOrder = if (order == "oldest") SortOrder.ASC else SortOrder.DESC
    val offsetVal = ((page - 1) * limit).toLong()

    return try {
        transaction {
            AdminLogs.slice(
                AdminLogs.id,
                AdminLogs.timestamp,
                AdminLogs.userId,
                AdminLogs.doneById,
                AdminLogs.added,
                AdminLogs.reason
            ).selectAll().orderBy(AdminLogs.id to orderBy)
                .limit(limit, offsetVal).map {
                    AdminLog(
                        it[AdminLogs.id],
                        it[AdminLogs.timestamp],
                        it[AdminLogs.userId],
                        it[AdminLogs.doneById],
                        it[AdminLogs.added],
                        it[AdminLogs.reason]
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun getAdminLogEntriesByAdmin(page: Int, limit: Int, userId: Long, order: String?,requestedId: Long): List<AdminLog>? {

    if(!isUserAdmin(userId)){
        return null
    }

    val orderBy: SortOrder = if (order == "oldest") SortOrder.ASC else SortOrder.DESC
    val offsetVal = ((page - 1) * limit).toLong()

    return try {
        transaction {
            AdminLogs.slice(
                AdminLogs.id,
                AdminLogs.timestamp,
                AdminLogs.userId,
                AdminLogs.doneById,
                AdminLogs.added,
                AdminLogs.reason
            ).select(AdminLogs.doneById eq requestedId).orderBy(AdminLogs.id to orderBy)
                .limit(limit, offsetVal).map {
                    AdminLog(
                        it[AdminLogs.id],
                        it[AdminLogs.timestamp],
                        it[AdminLogs.userId],
                        it[AdminLogs.doneById],
                        it[AdminLogs.added],
                        it[AdminLogs.reason]
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}
