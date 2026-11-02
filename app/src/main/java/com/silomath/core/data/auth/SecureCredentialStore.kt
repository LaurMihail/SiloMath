package com.silomath.core.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SecureCredentialStore {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveCredentials(accessToken: String?, refreshToken: String?)
    fun clearCredentials()
}

class KeystoreSecureCredentialStore(
    context: Context
) : SecureCredentialStore {

    private val keyAlias = "SiloMathKeystoreKey"
    private val prefsName = "silomath_secure_credentials"
    private val transformation = "AES/GCM/NoPadding"
    private val gcmTagLength = 128

    private val prefs: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
        return (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun encrypt(plainText: String?): String? {
        if (plainText.isNullOrBlank()) return null
        return try {
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun decrypt(encryptedBase64: String?): String? {
        if (encryptedBase64.isNullOrBlank()) return null
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size <= 12) return null
            val iv = ByteArray(12)
            System.arraycopy(combined, 0, iv, 0, 12)
            val encryptedBytes = ByteArray(combined.size - 12)
            System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.size)

            val cipher = Cipher.getInstance(transformation)
            val spec = GCMParameterSpec(gcmTagLength, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val plainBytes = cipher.doFinal(encryptedBytes)
            String(plainBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    override fun getAccessToken(): String? = decrypt(prefs.getString("encrypted_access_token", null))

    override fun getRefreshToken(): String? = decrypt(prefs.getString("encrypted_refresh_token", null))

    override fun saveCredentials(accessToken: String?, refreshToken: String?) {
        val encAccess = encrypt(accessToken)
        val encRefresh = encrypt(refreshToken)
        prefs.edit()
            .putString("encrypted_access_token", encAccess)
            .putString("encrypted_refresh_token", encRefresh)
            .apply()
    }

    override fun clearCredentials() {
        prefs.edit().clear().apply()
    }
}

class InMemorySecureCredentialStore : SecureCredentialStore {
    private var accessToken: String? = null
    private var refreshToken: String? = null

    override fun getAccessToken(): String? = accessToken
    override fun getRefreshToken(): String? = refreshToken

    override fun saveCredentials(accessToken: String?, refreshToken: String?) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    override fun clearCredentials() {
        this.accessToken = null
        this.refreshToken = null
    }
}
