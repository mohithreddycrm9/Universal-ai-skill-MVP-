package com.skillmcp.mentor.backup

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Encrypt backup payloads with a user-chosen passphrase (never stored on device). */
object PassphraseEncryptor {
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val ITERATIONS = 120_000

    fun encrypt(plainText: String, passphrase: CharArray): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt)
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = salt + iv + encrypted
        return "v2:" + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String, passphrase: CharArray): String {
        require(cipherText.startsWith("v2:")) { "Unsupported backup format" }
        val bytes = Base64.decode(cipherText.removePrefix("v2:"), Base64.NO_WRAP)
        val salt = bytes.copyOfRange(0, SALT_BYTES)
        val iv = bytes.copyOfRange(SALT_BYTES, SALT_BYTES + IV_BYTES)
        val payload = bytes.copyOfRange(SALT_BYTES + IV_BYTES, bytes.size)
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(payload).toString(Charsets.UTF_8)
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
