package com.silomath.core.sync.offline

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OfflinePackageStagingModule {
    @Provides @Singleton
    fun staging(@ApplicationContext context: Context): PackageStagingStore =
        EncryptedPackageStagingStore(File(context.noBackupFilesDir,"offline-packages")) { key() }

    @Synchronized private fun key(): SecretKey {
        val alias="silomath.offline.package.v1"
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias,null) as? SecretKey)?.let { return it }
        check(!store.containsAlias(alias)) { "Protected package key cannot be recovered" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }
}
