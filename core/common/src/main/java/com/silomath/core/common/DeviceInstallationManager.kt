package com.silomath.core.common

import android.content.Context
import android.content.SharedPreferences
import com.silomath.core.model.DeviceInstallation

class DeviceInstallationManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("silomath_device_installation", Context.MODE_PRIVATE)

    fun getOrCreateInstallation(): DeviceInstallation {
        var uuid = prefs.getString("device_installation_id", null)
        if (uuid == null) {
            val legacyUuid = prefs.getString("installation_uuid", null)
            if (legacyUuid != null && UuidUtils.isValidUuid(legacyUuid)) {
                uuid = legacyUuid
                prefs.edit()
                    .putString("device_installation_id", uuid)
                    .remove("installation_uuid")
                    .apply()
            } else {
                uuid = UuidUtils.generateUuid()
                prefs.edit()
                    .putString("device_installation_id", uuid)
                    .apply()
            }
        }
        return DeviceInstallation(deviceInstallationId = uuid)
    }
}
