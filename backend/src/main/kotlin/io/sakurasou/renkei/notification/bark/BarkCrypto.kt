package io.sakurasou.renkei.notification.bark

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class BarkCrypto(
    encryptionKey: String,
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    private val key = SecretKeySpec(encryptionKey.toByteArray(Charsets.UTF_8), "AES")

    fun encrypt(plainText: String): BarkCiphertext {
        val iv = randomASCII(IV_LENGTH)
        val cipher =
            Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    GCMParameterSpec(TAG_LENGTH_BITS, iv.toByteArray(Charsets.US_ASCII)),
                )
            }
        return BarkCiphertext(
            value = Base64.getEncoder().encodeToString(cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))),
            iv = iv,
        )
    }

    private fun randomASCII(length: Int): String =
        buildString(length) {
            repeat(length) {
                append(IV_ALPHABET[secureRandom.nextInt(IV_ALPHABET.length)])
            }
        }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
        const val IV_LENGTH = 12
        const val IV_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    }
}

internal data class BarkCiphertext(
    val value: String,
    val iv: String,
)
