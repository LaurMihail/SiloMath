package com.silomath.security

import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class ProtectedBackupPolicyTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val domains=setOf("root","file","database","sharedpref","external","device_root","device_file","device_database","device_sharedpref")

    @Test fun packagedApplicationDisallowsBackupAndLegacyRulesExcludeAllProtectedStores() {
        assertEquals(0,context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        assertEquals(domains,exclusions("backup_rules").getValue("full-backup-content"))
    }
    @Test fun cloudAndDeviceTransferExcludeProtectedStores() {
        val rules=exclusions("data_extraction_rules")
        assertEquals(domains,rules.getValue("cloud-backup"))
        assertEquals(domains,rules.getValue("device-transfer"))
    }
    private fun exclusions(resourceName: String): Map<String,Set<String>> {
        val id=context.resources.getIdentifier(resourceName,"xml",context.packageName)
        assertNotEquals(0,id)
        val parser=context.resources.getXml(id)
        val rules=mutableMapOf<String,MutableSet<String>>()
        var scope=""
        try {
            while(parser.eventType!=XmlPullParser.END_DOCUMENT) {
                if(parser.eventType==XmlPullParser.START_TAG) {
                    if(parser.name in setOf("full-backup-content","cloud-backup","device-transfer")) scope=parser.name
                    if(parser.name=="exclude") {
                        assertEquals(".",parser.getAttributeValue(null,"path"))
                        rules.getOrPut(scope) { mutableSetOf() }.add(parser.getAttributeValue(null,"domain"))
                    }
                }
                parser.next()
            }
        } finally { parser.close() }
        return rules
    }
}
