import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.util.io.Streams
import org.pgpainless.PGPainless
import org.pgpainless.algorithm.SymmetricKeyAlgorithm
import org.pgpainless.encryption_signing.EncryptionOptions
import org.pgpainless.encryption_signing.EncryptionStream
import org.pgpainless.encryption_signing.ProducerOptions
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

/**
 * Encrypt message
 *
 * @param publicKey
 * @param message
 * @return encrypted message
 */
fun encryptMessage(publicKey: String, message: String): ByteArray? {
    return try {
        val outputStream = ByteArrayOutputStream()

        val publicKeyObj: PGPPublicKeyRing = PGPainless.readKeyRing().publicKeyRing(publicKey)!!
        val plaintextInputStream = ByteArrayInputStream(message.toByteArray())

        val encryptionStream: EncryptionStream = PGPainless.encryptAndOrSign().onOutputStream(outputStream).withOptions(
            ProducerOptions.encrypt(
                EncryptionOptions().addRecipient(publicKeyObj)
                    .overrideEncryptionAlgorithm(SymmetricKeyAlgorithm.AES_192),

                ).setAsciiArmor(true),
        )

        Streams.pipeAll(plaintextInputStream, encryptionStream)
        encryptionStream.close()

        val encryptedMessage = Base64.getEncoder().encodeToString(outputStream.toByteArray())

        encryptedMessage.toByteArray()
    }catch (e: Exception){
        null
    }

}