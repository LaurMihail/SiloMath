package com.silomath.core.database.protection

/** Sanitized actionable failure; provider SQL, bind arguments and key material are never attached. */
class DatabaseProtectionException(val reason: Reason) : IllegalStateException("Protected database recovery required: ${reason.name}") {
    enum class Reason { KEY_UNAVAILABLE, KEY_ENVELOPE_INVALID, DATABASE_MISSING, DATABASE_INVALID,
        CONVERSION_FAILED, VERIFICATION_FAILED, RECOVERY_REQUIRED, BACKGROUND_OPEN_REQUIRED }
}

internal inline fun <T> protect(reason: DatabaseProtectionException.Reason, action: () -> T): T =
    try { action() } catch(failure: DatabaseProtectionException) { throw failure }
    catch(_: Exception) { throw DatabaseProtectionException(reason) }
