package byteboard.database.admin

import byteboard.database.useraccount.Users
import byteboard.database.useraccount.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object SuspendLog : Table(name = "SuspendLog") {
    val id: Column<Long> = long("id").autoIncrement()
    val timestamp: Column<LocalDateTime> = datetime("suspendTime")
    val adminId: Column<Long> = long("message_id").references(Users.id)
    //true for suspend, false for unsuspend
    val suspend: Column<Boolean> = bool("suspend")
    val reason: Column<String> = text("reason")

    override val primaryKey = PrimaryKey(id)
}


data class SuspendLogEntry(
    val id: Long, val time: LocalDateTime, val adminId: Long, val suspend: Boolean, val reason: String
)


fun insertSuspendEntry(userId: Long, isSuspend: Boolean, reasonString: String): Boolean {
    return try {
        transaction {
            SuspendLog.insert {
                it[timestamp] = LocalDateTime.now()
                it[adminId] = userId
                it[suspend] = isSuspend
                it[reason] = reasonString
            }.insertedCount > 0

        }
    } catch (e: Exception) {
        logger.error { e.message }
        false

    }
}

fun getSuspendLogEntries(page: Int, limit: Int, userId: Long, order: String?): List<SuspendLogEntry>? {
    val orderBy: SortOrder = if (order == "oldest") SortOrder.ASC else SortOrder.DESC
    val offsetVal = ((page - 1) * limit).toLong()

    return try {
        transaction {
            SuspendLog.slice(
                    SuspendLog.id,
                    SuspendLog.timestamp,
                    SuspendLog.adminId,
                    SuspendLog.suspend,
                    SuspendLog.reason
                ).selectAll().orderBy(SuspendLog.id to orderBy)
                .limit(limit, offsetVal).map {
                    SuspendLogEntry(
                        it[SuspendLog.id],
                        it[SuspendLog.timestamp],
                        it[SuspendLog.adminId],
                        it[SuspendLog.suspend],
                        it[SuspendLog.reason]
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

fun getSuspendLogEntriesByAdmin(page: Int, limit: Int, userId: Long, order: String?,adminId: Long): List<SuspendLogEntry>? {
    val orderBy: SortOrder = if (order == "oldest") SortOrder.ASC else SortOrder.DESC
    val offsetVal = ((page - 1) * limit).toLong()

    return try {
        transaction {
            SuspendLog.slice(
                SuspendLog.id,
                SuspendLog.timestamp,
                SuspendLog.adminId,
                SuspendLog.suspend,
                SuspendLog.reason
            ).select { SuspendLog.adminId eq userId }.orderBy(SuspendLog.timestamp to orderBy)
                .limit(limit, offsetVal).map {
                    SuspendLogEntry(
                        it[SuspendLog.id],
                        it[SuspendLog.timestamp],
                        it[SuspendLog.adminId],
                        it[SuspendLog.suspend],
                        it[SuspendLog.reason]
                    )
                }
        }
    } catch (e: Exception) {
        logger.error { e.message }
        null
    }
}

