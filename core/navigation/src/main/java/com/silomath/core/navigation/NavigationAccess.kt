package com.silomath.core.navigation

import com.silomath.core.model.*
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Presentation of already-authorized session scope supplied by the composition root.
 * Not an authentication credential or independent business-authorization source. */
data class NavigationScope(val userId: UserId?, val activeOrganizationId: OrganizationId?,
    val authorizedOrganizations: Set<OrganizationId>, val adminOrganizations: Set<OrganizationId>)

fun scopeAllows(target: Destination, scope: NavigationScope): Boolean = when(target) {
    SignIn, ResetPassword -> true
    SelectOrganization -> scope.userId != null
    is OrganizationDestination -> scope.userId != null &&
        target.organizationId == scope.activeOrganizationId &&
        target.organizationId in scope.authorizedOrganizations &&
        (target !is LocationEditor && target !is StorageUnitEditor || target.organizationId in scope.adminOrganizations)
}

/** Domain owners must re-read authoritative context, hierarchy, role, campaign access
 * and entity-state permissions. Unknown/unimplemented context must return false. */
fun interface DestinationAuthorizer {
    suspend fun isAuthorized(target: OrganizationDestination): Boolean
}

/** Scope is rechecked after async authorization to prevent stale-principal navigation. */
suspend fun authorizeNavigation(targets: List<OrganizationDestination>,
    scopeProvider: () -> NavigationScope, authorizer: DestinationAuthorizer): Boolean {
    val before = scopeProvider()
    if (targets.isEmpty() || targets.any { !scopeAllows(it,before) }) return false
    return try {
        targets.all { authorizer.isAuthorized(it) } && before == scopeProvider()
    } catch (e: CancellationException) { throw e }
    catch (_: Exception) { false }
}

/** Technical contextual-reference transport; no URI route authority or secret payload.
 * Unknown keys/fields, malformed IDs and oversized requests fail closed. */
object DestinationCodec {
    private val json = Json { ignoreUnknownKeys = false }
    fun encode(target: Destination): String = json.encodeToString(target)
    fun decode(value: String?): Destination? {
        if (value == null || value.length > 8192) return null
        return runCatching { json.decodeFromString<Destination>(value) }.getOrNull()
    }
}
