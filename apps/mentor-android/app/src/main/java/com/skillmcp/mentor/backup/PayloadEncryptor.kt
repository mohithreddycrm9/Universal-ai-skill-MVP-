package com.skillmcp.mentor.backup

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class PayloadEncryptor(context: Context) {
    private val masterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val prefs =
        EncryptedSharedPreferences.create(
            context,
            "mentor_secure_backup",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    private fun dataKey(): ByteArray {
        val existing = prefs.getString(KEY_DATA_KEY, null)
        if (existing != null) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_DATA_KEY, Base64.encodeToString(key, Base64.NO_WRAP)).apply()
        return key
    }

    fun encrypt(plainText: String): String {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(dataKey(), "AES"), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String): String {
        val bytes = Base64.decode(cipherText, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, 12)
        val payload = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(dataKey(), "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(payload).toString(Charsets.UTF_8)
    }

    private companion object {
        const val KEY_DATA_KEY = "backup_data_key"
    }
}
