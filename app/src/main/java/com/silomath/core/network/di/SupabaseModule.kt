package com.silomath.core.network.di

import com.silomath.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth) {
                scheme = "silomath"
                host = "auth"

                // SiloMath SecureCredentialStore is the only persistent
                // authority for access/refresh credentials.
                alwaysAutoRefresh = false
                autoLoadFromStorage = false
                autoSaveToStorage = false
                enableLifecycleCallbacks = false
            }

            install(Postgrest)
        }
    }
}
