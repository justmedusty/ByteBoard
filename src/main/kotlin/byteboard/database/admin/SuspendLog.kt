package byteboard.database.admin

import byteboard.database.useraccount.Users
import byteboard.database.useraccount.logger
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
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
    val id : Long,
    val time: LocalDateTime,
    val adminId : Long,
    val suspend: Boolean,
    val reason : String
)


fun insertSuspendEntry(userId: Long,isSuspend : Boolean,reasonString: String) : Boolean{
    return try {
        transaction {
            SuspendLog.insert {
                it[timestamp] = LocalDateTime.now()
                it[adminId] = userId
                it[suspend] = isSuspend
                it[reason] = reasonString
            }.insertedCount > 0

        }
    }catch (e:Exception){
        logger.error { e.message }
        false

    }
}

