package com.silomath.core.database.protection

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import java.nio.ByteBuffer
import java.security.MessageDigest

/** Physical verification only: canonical TEXT and legacy REAL bits are never reinterpreted. */
internal object DatabaseContentsFingerprint {
    fun capture(db: SupportSQLiteDatabase): ByteArray {
        db.query("PRAGMA integrity_check").use { rows ->
            check(rows.moveToFirst() && rows.getString(0)=="ok" && !rows.moveToNext())
        }
        val digest=MessageDigest.getInstance("SHA-256")
        add(digest,ByteBuffer.allocate(4).putInt(db.version).array())
        val tables=mutableListOf<String>()
        db.query("SELECT type,name,tbl_name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' OR name='sqlite_sequence' ORDER BY type,name").use { rows ->
            while(rows.moveToNext()) {
                add(digest,rowHash(rows))
                if(rows.getString(0)=="table") tables+=rows.getString(1)
            }
        }
        for(table in tables.sorted()) {
            add(digest,table.toByteArray(Charsets.UTF_8))
            val hashes=mutableListOf<String>()
            db.query("SELECT * FROM "+quote(table)).use { rows ->
                while(rows.moveToNext()) hashes+=rowHash(rows).joinToString("") { "%02x".format(it) }
            }
            add(digest,ByteBuffer.allocate(8).putLong(hashes.size.toLong()).array())
            hashes.sorted().forEach { add(digest,it.toByteArray(Charsets.US_ASCII)) }
        }
        val fk=mutableListOf<String>()
        db.query("PRAGMA foreign_key_check").use { rows ->
            while(rows.moveToNext()) fk+=rowHash(rows).joinToString("") { "%02x".format(it) }
        }
        fk.sorted().forEach { add(digest,it.toByteArray(Charsets.US_ASCII)) }
        return digest.digest()
    }
    private fun rowHash(row: Cursor): ByteArray {
        val digest=MessageDigest.getInstance("SHA-256")
        for(column in 0 until row.columnCount) {
            val type=row.getType(column)
            digest.update(type.toByte())
            val bytes=when(type) {
                Cursor.FIELD_TYPE_NULL -> byteArrayOf()
                Cursor.FIELD_TYPE_INTEGER -> ByteBuffer.allocate(8).putLong(row.getLong(column)).array()
                Cursor.FIELD_TYPE_FLOAT -> ByteBuffer.allocate(8).putLong(java.lang.Double.doubleToRawLongBits(row.getDouble(column))).array()
                Cursor.FIELD_TYPE_BLOB -> row.getBlob(column)
                else -> row.getString(column).toByteArray(Charsets.UTF_8)
            }
            add(digest,bytes)
        }
        return digest.digest()
    }
    private fun add(digest: MessageDigest,bytes: ByteArray) {
        digest.update(ByteBuffer.allocate(8).putLong(bytes.size.toLong()).array());digest.update(bytes)
    }
    private fun quote(name: String)="\""+name.replace("\"","\"\"")+"\""
}
