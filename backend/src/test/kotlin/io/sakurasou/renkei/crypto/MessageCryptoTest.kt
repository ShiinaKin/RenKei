package io.sakurasou.renkei.crypto

import java.security.KeyPairGenerator
import java.util.Base64
import javax.crypto.Cipher
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageCryptoTest {
    @Test
    fun `decrypts private-key encrypted content with public key`() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val content = "RenKei Android 长消息连接测试".repeat(30)
        val cipherText =
            content
                .encodeToByteArray()
                .toList()
                .chunked(245)
                .joinToString("\n") { chunk ->
                    Cipher
                        .getInstance("RSA/ECB/PKCS1Padding")
                        .apply { init(Cipher.ENCRYPT_MODE, keyPair.private) }
                        .doFinal(chunk.toByteArray())
                        .let(Base64.getEncoder()::encodeToString)
                }
        val publicKey = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        assertEquals(content, MessageCrypto.decryptWithPublicKey(cipherText, publicKey))
    }
}
