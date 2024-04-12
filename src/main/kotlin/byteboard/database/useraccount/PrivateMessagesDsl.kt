import byteboard.database.useraccount.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object Messages : Table(name = "Messages") {
    val id: Column<Long> = long("id").autoIncrement()
    val senderId: Column<Long> = long("sender_id").references(Users.id)
    val receiverId: Column<Long> = long("receiver_id").references(Users.id)
    val message: Column<String> = text("message")
    val timeSent: Column<LocalDateTime> = datetime("time_sent").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

data class Message(
    val id: Long,
    val senderUserName: String,
    val receiverUserName: String,
    val message: String,
    val timeSent: LocalDateTime
)

fun sendMessage(sender: Long, receiver: Long, messageString: String): Boolean {

    val publicKey: String?
    var encryptedMessage: ByteArray? = null
    if (hasAutoEncryptionEnabled(receiver)) {
        publicKey = getPublicKey(receiver)
        if (publicKey != null) {
            encryptedMessage = encryptMessage(publicKey, messageString)
        } else {
            encryptedMessage = null
        }

    }

    return try {

        transaction {

            Messages.insert {
                it[senderId] = sender
                it[receiverId] = receiver

                if (encryptedMessage != null) {
                    //TODO remember this is here and make sure this conversion works properly
                    it[message] = encryptedMessage.contentToString()
                } else {
                    it[message] = messageString
                }

                it[timeSent] = LocalDateTime.now()
            }
            true

        }
    } catch (e: Exception) {
        logger.error { e.message }
        false
    }

}

fun getMessagesFromUser(requesterId: Long, requestedId: Long, page: Int, limit: Int): List<Message>? {
    try {
        val offsetVal = ((page - 1) * limit).toLong()

        // Get sender and receiver usernames
        val senderUserNameString = getUserName(requestedId)
        val receiverUserNameString = getUserName(requesterId)

        return if (senderUserNameString != null && receiverUserNameString != null) {


            transaction {
                Messages.select { (Messages.receiverId eq requesterId) and (Messages.senderId eq requestedId) }
                    .limit(limit, offsetVal).map {
                        Message(
                            id = it[Messages.id],
                            senderUserName = senderUserNameString,
                            receiverUserName = receiverUserNameString,
                            message = it[Messages.message],
                            timeSent = it[Messages.timeSent]
                        )
                    }
            }
        } else {
            null
        }
    } catch (e: Exception) {
        logger.error { e.message }
        return null //return null on error , not empty list, because empty list can mean more than just error, null can only mean error
    }
}

fun getAllMessages(userId: Long, page: Int, limit: Int): List<Message>? {
    val offsetVal = ((page - 1) * limit).toLong()
    val receiverUserNameString = getUserName(userId)
    return if (receiverUserNameString != null) {
        try {
            transaction {

                Messages.select { (Messages.receiverId eq userId) }.limit(limit, offsetVal).map {
                    val senderId = it[Messages.senderId]
                    val senderUserNameString = getUserName(senderId) ?: "Unknown"

                    Message(
                        id = it[Messages.id],
                        senderUserName = senderUserNameString,
                        receiverUserName = receiverUserNameString,
                        message = it[Messages.message],
                        timeSent = it[Messages.timeSent]
                    )
                }
            }
        } catch (e: Exception) {
            logger.error { e.message }
            null
        }
    } else return null
}