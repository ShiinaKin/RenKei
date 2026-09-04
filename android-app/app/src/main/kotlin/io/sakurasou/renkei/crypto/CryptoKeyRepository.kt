package io.sakurasou.renkei.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.sakurasou.renkei.module.IoDispatcher
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.interfaces.RSAKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class CryptoKeyInfo(
    val hasKeyPair: Boolean,
    val publicKey: String,
)

@Singleton
class CryptoKeyRepository
@Inject
constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val keyMutex = Mutex()

    suspend fun getKeyInfo(): CryptoKeyInfo =
        withContext(ioDispatcher) {
            keyMutex.withLock {
                val entry = loadEntry()
                CryptoKeyInfo(
                    hasKeyPair = entry != null,
                    publicKey = entry?.certificate?.publicKey?.encoded?.toBase64().orEmpty(),
                )
            }
        }

    suspend fun generateKeyPair(): CryptoKeyInfo =
        withContext(ioDispatcher) {
            keyMutex.withLock {
                if (loadEntry() == null) {
                    createKeyPair()
                }

                requireKeyInfo()
            }
        }

    suspend fun regenerateKeyPair(): CryptoKeyInfo =
        withContext(ioDispatcher) {
            keyMutex.withLock {
                loadKeyStore().deleteEntry(KEY_ALIAS)
                createKeyPair()
                requireKeyInfo()
            }
        }

    suspend fun encryptWithPrivateKey(plainText: String): String =
        withContext(ioDispatcher) {
            keyMutex.withLock {
                val privateKey = requireNotNull(loadEntry()?.privateKey) {
                    "请先生成密钥对"
                }
                val plainBytes = plainText.encodeToByteArray()
                plainBytes
                    .toList()
                    .chunked(MAX_PRIVATE_ENCRYPTION_BYTES)
                    .joinToString("\n") { chunk ->
                        createCipher(PRIVATE_KEY_CIPHER_TRANSFORMATION)
                            .apply { init(Cipher.ENCRYPT_MODE, privateKey) }
                            .doFinal(chunk.toByteArray())
                            .toBase64()
                    }
            }
        }

    suspend fun encrypt(plainText: String): String =
        withContext(ioDispatcher) {
            keyMutex.withLock {
                val publicKey = requireNotNull(loadEntry()?.certificate?.publicKey) {
                    "请先生成密钥对"
                }
                val plainBytes = plainText.encodeToByteArray()
                val rsaKey = publicKey as RSAKey
                val maxPlainTextBytes = rsaKey.modulus.bitLength() / Byte.SIZE_BITS - OAEP_OVERHEAD_BYTES
                require(plainBytes.size <= maxPlainTextBytes) {
                    "测试内容过长，最多支持 $maxPlainTextBytes 字节"
                }

                createCipher(CIPHER_TRANSFORMATION).apply {
                    init(Cipher.ENCRYPT_MODE, publicKey, oaepParameterSpec)
                }.doFinal(plainBytes).toBase64()
            }
        }

    suspend fun decrypt(cipherText: String): String =
        withContext(ioDispatcher) {
            keyMutex.withLock {
                val privateKey = requireNotNull(loadEntry()?.privateKey) {
                    "请先生成密钥对"
                }
                createCipher(CIPHER_TRANSFORMATION).apply {
                    init(Cipher.DECRYPT_MODE, privateKey, oaepParameterSpec)
                }.doFinal(Base64.decode(cipherText.trim(), Base64.NO_WRAP)).decodeToString()
            }
        }

    private fun loadEntry(): KeyStore.PrivateKeyEntry? =
        loadKeyStore().getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun createKeyPair() {
        KeyPairGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
            .apply {
                initialize(
                    KeyGenParameterSpec
                        .Builder(
                            KEY_ALIAS,
                            KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_SIGN,
                        ).setKeySize(KEY_SIZE_BITS)
                        .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .build(),
                )
            }.generateKeyPair()
    }

    private fun requireKeyInfo(): CryptoKeyInfo {
        val entry = checkNotNull(loadEntry())
        return CryptoKeyInfo(
            hasKeyPair = true,
            publicKey = entry.certificate.publicKey.encoded.toBase64(),
        )
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "renkei_transport_key_v2"
        const val KEY_SIZE_BITS = 2048
        const val PKCS1_PADDING_OVERHEAD_BYTES = 11
        const val MAX_PRIVATE_ENCRYPTION_BYTES = KEY_SIZE_BITS / Byte.SIZE_BITS - PKCS1_PADDING_OVERHEAD_BYTES
        const val SHA256_DIGEST_BYTES = 32
        const val OAEP_OVERHEAD_BYTES = 2 * SHA256_DIGEST_BYTES + 2
        const val CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        const val PRIVATE_KEY_CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding"

        val oaepParameterSpec =
            OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA1,
                PSource.PSpecified.DEFAULT,
            )
    }
}

private fun createCipher(transformation: String): Cipher = Cipher.getInstance(transformation)

private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
