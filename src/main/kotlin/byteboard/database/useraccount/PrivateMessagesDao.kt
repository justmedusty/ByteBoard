import byteboard.database.useraccount.Users
import byteboard.database.useraccount.getUserName
import byteboard.database.useraccount.logger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object Messages : Table(name = "Messages") {
    private val id: Column<Int> = integer("id").autoIncrement()
    val senderId: Column<Long> = long("sender_id").references(Users.id)
    val receiverId: Column<Long> = long("receiver_id").references(Users.id)
    val message: Column<String> = text("message")
    val timeSent: Column<LocalDateTime> = datetime("time_sent").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

data class Message(
    val senderUserName: String, val receiverUserName: String, val message: String, val timeSent: LocalDateTime
)


fun insertMessage(sender: Long, receiver: Long, messageString: String): Boolean {
    return try {

        transaction {
            Messages.insert {
                it[senderId] = sender
                it[receiverId] = receiver
                it[message] = messageString
                it[timeSent] = LocalDateTime.now()
            }
            true

        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}

fun getMessagesFromUser(requestorId: Long, requestedId: Long, page: Int, limit: Int): List<Message> {
    try {
        val offsetVal = ((page - 1) * limit).toLong()

        // Get sender and receiver usernames
        val senderUserNameString = getUserName(requestedId)
        val receiverUserNameString = getUserName(requestorId)

        if(senderUserNameString != null && receiverUserNameString != null) {


            return transaction {
                Messages.select { (Messages.receiverId eq requestorId) and (Messages.senderId eq requestedId) }
                    .limit(limit, offsetVal)
                    .map {
                        Message(
                            senderUserName = senderUserNameString,
                            receiverUserName = receiverUserNameString,
                            message = it[Messages.message],
                            timeSent = it[Messages.timeSent]
                        )
                    }
            }
        }else{
            return emptyList()
        }
    } catch (e: Exception) {
        logger.error { e.message }
        return emptyList()  // Return an empty list if there's an error
    }
}

fun getAllMessages(userId : Long,page: Int,limit: Int) : List<Message>{
    val offsetVal = ((page - 1) * limit).toLong()
    val receiverUserNameString = getUserName(userId)
    if(receiverUserNameString != null) return try {
        transaction {

            Messages.select { (Messages.receiverId eq userId) }
                .limit(limit, offsetVal)
                .map {
                    val senderId = it[Messages.senderId]
                    val senderUserNameString = getUserName(senderId) ?: "Unknown"

                    Message(
                        senderUserName = senderUserNameString,
                        receiverUserName = receiverUserNameString,
                        message = it[Messages.message],
                        timeSent = it[Messages.timeSent]
                    )
                }
        }
    }catch (e:Exception){
        logger.error { e.message }
        emptyList()
    } else return emptyList()
}