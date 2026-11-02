package com.silomath.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.silomath.core.data.auth.KeystoreSecureCredentialStore
import com.silomath.core.data.auth.SecureCredentialStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("silo_math_prefs") }
        )
    }

    @Provides
    @Singleton
    fun provideSecureCredentialStore(@ApplicationContext context: Context): SecureCredentialStore {
        return KeystoreSecureCredentialStore(context)
    }
}
