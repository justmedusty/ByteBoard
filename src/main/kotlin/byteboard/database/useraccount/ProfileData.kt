package byteboard.database.useraccount

import byteboard.database.posts.Posts.references
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.ByteArrayInputStream


object ProfileData : Table(name = "ProfileData") {
    val id: Column<Long> = long("id").autoIncrement()
    val userId: Column<Long> = long("user_id").references(Users.id,ReferenceOption.CASCADE)
    val bio = text("bio").nullable()
    val publicKey = text("public_key").nullable()
    val profilePhoto : Column<ExposedBlob?> = blob("profile_photo").nullable()

    override val primaryKey = PrimaryKey(id)
}

data class ProfileDataEntry(
    val userName: String,
    val bio : String?,
    val publicKey: String?,
    val profilePhoto : ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProfileDataEntry

        if (userName != other.userName) return false
        if (bio != other.bio) return false
        if (publicKey != other.publicKey) return false
        if (profilePhoto != null) {
            if (other.profilePhoto == null) return false
            if (!profilePhoto.contentEquals(other.profilePhoto)) return false
        } else if (other.profilePhoto != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userName.hashCode()
        result = 31 * result + (bio?.hashCode() ?: 0)
        result = 31 * result + (publicKey?.hashCode() ?: 0)
        result = 31 * result + (profilePhoto?.contentHashCode() ?: 0)
        return result
    }
}

fun updateBio(userId: Long, bioContents : String) : Boolean{
    return try{
        transaction {
            ProfileData.update({ ProfileData.userId eq userId }){
                it[bio] = bioContents
            }
            true
        }
    }catch (e:Exception){
        logger.error { e.message }
        false
    }
}

fun updatePublicKey(userId: Long, keyContents : String) : Boolean{
    return try{
        transaction {
            ProfileData.update({ ProfileData.userId eq userId }){
                it[publicKey] = keyContents
            }
            true
        }
    }catch (e:Exception){
        logger.error { e.message }
        false
    }
}


fun updateProfilePhoto(userId: Long, photo : ByteArray) : Boolean{
    return try{
        transaction {
            ProfileData.update({ ProfileData.userId eq userId }){
                it[profilePhoto] = ExposedBlob(photo)
            }
            true
        }
    }catch (e:Exception){
        logger.error { e.message }
        false
    }
}