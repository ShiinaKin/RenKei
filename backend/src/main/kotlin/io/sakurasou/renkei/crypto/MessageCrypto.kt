package io.sakurasou.renkei.crypto

import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import kotlin.io.encoding.Base64

object MessageCrypto {
    fun decryptWithPublicKey(
        cipherText: String,
        publicKey: String,
    ): String {
        val key =
            KeyFactory
                .getInstance(KEY_ALGORITHM)
                .generatePublic(X509EncodedKeySpec(Base64.decode(publicKey)))
        val plainText = ByteArrayOutputStream()
        cipherText
            .lineSequence()
            .filter(String::isNotBlank)
            .forEach { block ->
                val bytes =
                    Cipher
                        .getInstance(CIPHER_TRANSFORMATION)
                        .apply { init(Cipher.DECRYPT_MODE, key) }
                        .doFinal(Base64.decode(block))
                plainText.write(bytes)
            }
        return plainText.toByteArray().decodeToString()
    }

    private const val KEY_ALGORITHM = "RSA"
    private const val CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
}
