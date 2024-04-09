package byteboard.database.useraccount

/**
 * Configure database
 *
 */

import byteboard.security.hashPassword
import mu.KotlinLogging
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object Users : Table(name = "Users") {
    val id: Column<Long> = long("id").autoIncrement()
    val userName: Column<String> = varchar("user_name", 45).uniqueIndex()
    val passwordHash = text("password_hash")
    val isAdmin = bool("is_admin").default(false)
    val isModerator = bool("is_moderator").default(false)
    val isSuspended = bool("is_suspended").default(false)

    override val primaryKey = PrimaryKey(id)
}

val logger = KotlinLogging.logger { }

/**
 * User
 *
 * @property userName
 * @property publicKey
 * @property passwordHash
 * @constructor Create empty User
 */
data class User(
    val userName: String,
    val publicKey: String?,
    val passwordHash: String,
    val isAdmin: Boolean,
    val isModerator: Boolean,
    val isSuspended: Boolean
)


/**
 * Username already exists
 *
 * @param userName
 * @return
 */
fun userNameAlreadyExists(userName: String): Boolean {
    return try {
        transaction {
            Users.select { Users.userName eq userName }.count() > 0
        }
    } catch (e: Exception) {
        logger.error { "Error checking username $e" }
        return true
    }
}


/**
 * Verify credentials
 *
 * @param userName
 * @param password
 * @return
 */
fun verifyCredentials(userName: String, password: String): Boolean {
    return try {
        transaction {
            val user = Users.select { Users.userName eq userName }.singleOrNull()
            user != null && BCrypt.checkpw(password, user[Users.passwordHash])
        }
    } catch (e: Exception) {
        logger.error { "Error verifying credentials $e" }
        return false
    }
}

/**
 * User and password validation
 *
 * @param userName
 * @param password
 * @return
 */
fun userAndPasswordValidation(userName: String, password: String): Boolean {
    return try {
        when {
            password.isEmpty() && userName.isNotEmpty() -> {
                if (userNameAlreadyExists(userName)) {
                    false
                } else {
                    userName.length in 6..45
                }
            }

            password.isNotEmpty() && userName.isEmpty() -> {
                password.length >= 8
            }

            else -> {
                throw IllegalArgumentException("Unknown error")
            }
        }
    } catch (e: Exception) {
        logger.error { "Error with user/pass validation $e" }
        return false
    }
}

/**
 * Create user
 *
 * @param user
 */ // Functions to perform CRUD operations on Users table
fun createUser(user: User) {
    return try {
        transaction {
            if (userAndPasswordValidation(user.userName, "") && userAndPasswordValidation("", user.passwordHash)) {
                Users.insert {
                    it[userName] = user.userName
                    it[passwordHash] = hashPassword(user.passwordHash)
                } get Users.id
            }
        }
    } catch (e: Exception) {
        logger.error { "Error creating user $e" }
    }
}

/**
 * Get user id
 *
 * @param userName
 * @return
 */
fun getUserId(userName: String): Long {
    return try {
        transaction {
            Users.select { Users.userName eq userName }.singleOrNull()?.get(Users.id)!!
        }
    } catch (e: Exception) {
        logger.error { "Error getting userID $e" }
        -1
    }
}

/**
 * Get username
 *
 * @param id
 * @return
 */
fun getUserName(id: Long): String? {

    return try {
        val result = transaction {
            Users.select { Users.id eq id }.singleOrNull()?.get(Users.userName)
        }
        return result

    } catch (e: Exception) {
        logger.error { "Error grabbing username $e" }
        null
    }
}

/**
 * Update user credentials
 *
 * @param userName
 * @param password
 * @param newValue
 */
fun updateUserCredentials(userName: String, password: Boolean, newValue: String) {
    try {
        transaction {
            when {
                !password && newValue.isNotEmpty() -> {
                    Users.update({ Users.userName eq userName }) {
                        it[Users.userName] = newValue
                    }
                }

                password && newValue.isNotEmpty() -> {
                    Users.update({ Users.userName eq userName }) {
                        it[passwordHash] = hashPassword(newValue)
                    }
                }

                else -> {
                    throw IllegalArgumentException("An error occurred during the update")
                }
            }
        }
    } catch (e: Exception) {
        logger.error { "Error updating user credentials $e" }
    }
}

/**
 * Delete user
 *
 * @param id
 */
fun deleteUser(id: Long) {
    try {
        transaction {
            Users.deleteWhere { Users.id eq id }
        }
    } catch (e: Exception) {
        logger.error { "Error deleting user $e" }
    }
}

fun fetchAllUsers(page: Int, limit: Int): List<String> {
    val offset: Long = ((page - 1) * limit).toLong()
    return try {
        transaction {
            Users.slice(Users.userName).selectAll().limit(limit, offset).map { it[Users.userName] }
        }
    } catch (e: Exception) {
        logger.error { "Error occurred fetching users $e" }
        emptyList()
    }
}

fun searchAllUsers(query: String, page: Int, limit: Int): List<String> {
    return try {
        val offset = (page - 1) * limit
        transaction {
            Users.slice(Users.userName).select { Users.userName like "%$query%" }.limit(
                limit,
                offset.toLong(),
            ).map { it[Users.userName] }
        }
    } catch (e: Exception) {
        logger.error { "Error during search for users $e " }
        emptyList()
    }
}

fun isUserSuspended(userId: Long): Boolean {
    return try {
        transaction {
            userId.let { userId ->
                Users.select { Users.id eq userId }.singleOrNull()?.get(Users.isSuspended) ?: false
            }


        }
    } catch (e: ExposedSQLException) {
        logger.error { e.message }
        return false
    }
}

fun isUserAdmin(userId: Long): Boolean {
    return try {
        transaction {
            userId.let { userId ->
                Users.select { Users.id eq userId }.singleOrNull()?.get(Users.isAdmin) ?: false
            }


        }
    } catch (e: ExposedSQLException) {
        logger.error { e.message }
        return false
    }
}

fun isUserModerator(userId: Long): Boolean {
    return try {
        transaction {
            userId.let { userId ->
                Users.select { Users.id eq userId }.singleOrNull()?.get(Users.isModerator) ?: false
            }


        }
    } catch (e: ExposedSQLException) {
        logger.error { e.message }
        return false
    }
}

fun suspendUser(userId: Long, requesterId: Long): Boolean {

    if (isUserAdmin(requesterId)) {
        return try {
            transaction {
                Users.update({ Users.id eq userId }) {
                    it[isSuspended] = true
                }
                true
            }
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }
    }
    return false

}

fun unSuspendUser(userId: Long, requesterId: Long): Boolean {

    if (isUserAdmin(requesterId)) {
        return try {
            transaction {
                Users.update({ Users.id eq userId }) {
                    it[isSuspended] = false
                }
                true
            }
        } catch (e: Exception) {
            logger.error { e.message }
            false
        }
    }
    return false

}