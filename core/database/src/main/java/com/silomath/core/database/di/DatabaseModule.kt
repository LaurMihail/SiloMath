package com.silomath.core.database.di

import android.content.Context
import com.silomath.core.database.protection.EncryptedRoomOpenHelperFactory
import com.silomath.core.database.offline.CanonicalOfflineGuards
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.silomath.core.database.SiloMathDatabase
import com.silomath.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SiloMathDatabase {
        return Room.databaseBuilder(
            context,
            SiloMathDatabase::class.java,
            "silomath_db"
        )
        .openHelperFactory(EncryptedRoomOpenHelperFactory(context)).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                CanonicalOfflineGuards.install(db)

    @Provides
    @Singleton
    fun provideLocationCanonicalDao(
        database: SiloMathDatabase
    ): LocationCanonicalDao = database.locationCanonicalDao()
}
            override fun onOpen(db: SupportSQLiteDatabase) {

    @Provides
    @Singleton
    fun provideLocationCanonicalDao(
        database: SiloMathDatabase
    ): LocationCanonicalDao = database.locationCanonicalDao()
}
        })
        .build()

    @Provides
    @Singleton
    fun provideLocationCanonicalDao(
        database: SiloMathDatabase
    ): LocationCanonicalDao = database.locationCanonicalDao()
}

    @Provides
    @Singleton
    fun provideLocationCanonicalDao(
        database: SiloMathDatabase
    ): LocationCanonicalDao = database.locationCanonicalDao()
}
