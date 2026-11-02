package com.silomath.core.database.protection

import android.content.Context
import android.os.Looper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.DatabaseErrorHandler
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper as CipherOpenHelper

/** Application DI has no plaintext branch. Tests may explicitly own a different builder/secret source. */
class EncryptedRoomOpenHelperFactory(private val context: Context) : SupportSQLiteOpenHelper.Factory {
    internal var secretOverride: DatabaseSecretSource?=null
    internal var checkpoint: (ConversionCheckpoint)->Unit={}

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val name=requireNotNull(configuration.name)
        require(name.matches(Regex("[A-Za-z0-9_.-]+")))
        return object : SupportSQLiteOpenHelper {
            private var helper: CipherOpenHelper?=null
            private var secret: ByteArray?=null
            private var wal=false
            override val databaseName: String get()=name
            override val writableDatabase: SupportSQLiteDatabase get()=open()
            override val readableDatabase: SupportSQLiteDatabase get()=open()
            @Synchronized override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
                wal=enabled;helper?.setWriteAheadLoggingEnabled(enabled)
            }
            @Synchronized private fun open(): SupportSQLiteDatabase {
                helper?.let { return it.writableDatabase }
                if(Looper.myLooper()==Looper.getMainLooper()) throw DatabaseProtectionException(DatabaseProtectionException.Reason.BACKGROUND_OPEN_REQUIRED)
                return protect(DatabaseProtectionException.Reason.DATABASE_INVALID) {
                    NativeSqlCipher.load()
                    val preparation=EncryptedDatabasePreparation(context,name,checkpoint)
                    preparation.locked {
                        val key=(secretOverride ?: KeystoreDatabaseSecret(context,name)).loadOrCreate(preparation.mayCreateSecret())
                        try {
                            check(key.size==32)
                            preparation.prepare(key)
                            val callback=configuration.callback
                            val handler=DatabaseErrorHandler { _, _ -> throw DatabaseProtectionException(DatabaseProtectionException.Reason.DATABASE_INVALID) }
                            val encrypted=object : CipherOpenHelper(configuration.context,name,key,null,callback.version,0,handler,null,wal) {
                                override fun onConfigure(db: CipherDatabase)=callback.onConfigure(db)
                                override fun onCreate(db: CipherDatabase)=callback.onCreate(db)
                                override fun onUpgrade(db: CipherDatabase,oldVersion: Int,newVersion: Int)=callback.onUpgrade(db,oldVersion,newVersion)
                                override fun onDowngrade(db: CipherDatabase,oldVersion: Int,newVersion: Int)=callback.onDowngrade(db,oldVersion,newVersion)
                                override fun onOpen(db: CipherDatabase)=callback.onOpen(db)
                            }
                            try {
                                val db=encrypted.writableDatabase
                                check(!EncryptedDatabasePreparation.isPlaintext(preparation.source))
                                preparation.markInitialized(key)
                                helper=encrypted;secret=key
                                db
                            } catch(failure: Exception) { encrypted.close();throw failure }
                        } catch(failure: Exception) { key.fill(0);throw failure }
                    }
                }
            }
            @Synchronized override fun close() {
                try { helper?.close() } finally { helper=null;secret?.fill(0);secret=null }
            }
        }
    }
}

internal object NativeSqlCipher {
    private var loaded=false
    @Synchronized fun load() { if(!loaded) { System.loadLibrary("sqlcipher");loaded=true } }
}
