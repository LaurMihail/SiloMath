package com.silomath.core.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*
import com.silomath.core.model.OrganizationId

/** Call inside a principal+organization composition key. All five stacks/decorators
 * remain owned while switching tabs; popping clears only the removed entry. */
@Composable
fun rememberOperationalStacks(organizationId: OrganizationId): OperationalStacks {
    val active = rememberSaveable(organizationId.value) { mutableStateOf(ShellSection.Dashboard) }
    val stacks = ShellSection.entries.associateWith { section ->
        key(organizationId,section) { rememberNavBackStack(ShellDestination(organizationId,section)) }
    }
    return remember(organizationId) { OperationalStacks(organizationId,active,stacks) }
}

class OperationalStacks(val organizationId: OrganizationId,
    active: MutableState<ShellSection>,
    val stacks: Map<ShellSection, NavBackStack<NavKey>>) {
    var activeSection by active
        private set
    val currentStack get() = stacks.getValue(activeSection)
    val current get() = currentStack.last() as OrganizationDestination

    suspend fun open(target: OrganizationDestination, scopeProvider: () -> NavigationScope,
        authorizer: DestinationAuthorizer, deepLink: Boolean = false): Boolean {
        if (target.organizationId != organizationId) return false
        val chain = if (deepLink) target.parentChain() else listOf(target)
        if (!authorizeNavigation(chain,scopeProvider,authorizer)) return false
        val section = target.shellSection()
        val stack = stacks.getValue(section)
        if (deepLink) { stack.clear(); stack.addAll(chain) }
        else if (target !is ShellDestination && stack.lastOrNull() != target) stack.add(target)
        activeSection = section
        return true
    }

    /** Invoked on committed Back only, never during predictive progress/cancellation. */
    fun back() {
        if (currentStack.size > 1) currentStack.removeLastOrNull()
        else if (activeSection != ShellSection.Dashboard) activeSection = ShellSection.Dashboard
    }

    @Composable
    fun decoratedEntries(provider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
        val entries = stacks.mapValues { (section,stack) ->
            key(organizationId,section) {
                rememberDecoratedNavEntries(backStack = stack,
                    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()), entryProvider = provider)
            }
        }
        return if (activeSection == ShellSection.Dashboard) entries.getValue(activeSection)
            else entries.getValue(ShellSection.Dashboard) + entries.getValue(activeSection)
    }
}
