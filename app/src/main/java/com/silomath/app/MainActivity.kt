package com.silomath.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.silomath.app.navigation.ProductionDestinationAuthorizer
import com.silomath.app.navigation.navigationScope
import com.silomath.core.designsystem.SiloMathTheme
import com.silomath.core.designsystem.components.*
import com.silomath.core.model.OrganizationId
import com.silomath.core.navigation.*
import com.silomath.feature.authentication.*
import com.silomath.feature.dashboard.DashboardScreen
import com.silomath.feature.masterdata.organization.SelectOrganizationScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var destinationAuthorizer: ProductionDestinationAuthorizer
    private val requestedDestination = MutableStateFlow<Destination?>(null)
    private val recoveryContext = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureNavigationWindow(lightSurface = true)
        acceptIntent(intent)
        setContent {
            val target by requestedDestination.collectAsStateWithLifecycle()
            val recovery by recoveryContext.collectAsStateWithLifecycle()
            SiloMathTheme { SiloMathApp(recovery, target, destinationAuthorizer) }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); acceptIntent(intent)
    }
    private fun acceptIntent(intent: Intent?) {
        requestedDestination.value = DestinationCodec.decode(intent?.getStringExtra(EXTRA_NAVIGATION_TARGET))
        recoveryContext.value = intent?.getStringExtra(EXTRA_RECOVERY_CONTEXT)?.takeIf { it.isNotBlank() }
    }
    companion object {
        const val EXTRA_RECOVERY_CONTEXT = "com.silomath.app.extra.RECOVERY_CONTEXT"
        const val EXTRA_NAVIGATION_TARGET = "com.silomath.app.extra.NAVIGATION_TARGET"
    }
}

@Composable
fun SiloMathApp(recoveryContext: String? = null, requestedDestination: Destination? = null,
    destinationAuthorizer: DestinationAuthorizer,
    authViewModel: AuthViewModel = hiltViewModel()) {
    val auth by authViewModel.uiState.collectAsStateWithLifecycle()
    val session by authViewModel.sessionState.collectAsStateWithLifecycle()
    val scope = navigationScope(session,auth.isRestoredSessionReady)
    var recoveryHandled by rememberSaveable(recoveryContext?.hashCode()) { mutableStateOf(false) }
    var signInAfterRecovery by rememberSaveable { mutableStateOf(false) }
    val isRecovery = !recoveryContext.isNullOrBlank() && !recoveryHandled
    if (!isRecovery && !auth.isSessionRestoreComplete) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(),contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }; return
    }
    var choosingOrganization by rememberSaveable { mutableStateOf(false) }
    val authorizedOrg = scope.activeOrganizationId?.takeIf {
        scope.userId != null && it in scope.authorizedOrganizations
    }
    if (!isRecovery && !signInAfterRecovery && scope.userId != null && authorizedOrg != null && !choosingOrganization) {
        key(scope.userId,authorizedOrg) {
            OperationalShell(authorizedOrg,scope,{ navigationScope(session,auth.isRestoredSessionReady) },
                requestedDestination,destinationAuthorizer)
        }
    } else {
        val initial = when { isRecovery -> ResetPassword
            signInAfterRecovery -> SignIn
            scope.userId != null -> SelectOrganization
            else -> SignIn }
        // A change of trusted authentication phase destroys inaccessible entry state.
        key(initial) {
            val stack = rememberNavBackStack(*(if (initial == ResetPassword)
                arrayOf(SignIn,ResetPassword) else arrayOf(initial)))
            var forgotPassword by rememberSaveable { mutableStateOf(false) }
            Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                NavDisplay(backStack = stack,
                    onBack = {
                        if (stack.lastOrNull() == ResetPassword) { recoveryHandled = true; signInAfterRecovery = true }
                        if (stack.size > 1) stack.removeLastOrNull()
                    },
                    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
                    entryProvider = { key -> NavEntry(key) {
                        when(key) {
                            SignIn -> SignInScreen(onSignInSuccess = { signInAfterRecovery = false; choosingOrganization = true },
                                onForgotPasswordClick = { forgotPassword = true }, viewModel = authViewModel)
                            ResetPassword -> ResetPasswordScreen(recoveryContext.orEmpty(),
                                onResetSuccess = { recoveryHandled = true; signInAfterRecovery = true },
                                onBackToSignIn = { recoveryHandled = true; signInAfterRecovery = true }, viewModel = authViewModel)
                            SelectOrganization -> if (scopeAllows(SelectOrganization,scope)) {
                                SelectOrganizationScreen(onOrganizationSelected = { choosingOrganization = false })
                            }
                            else -> Unit
                        }
                    } }
                )
                if (forgotPassword) ForgotPasswordSheet(onDismiss = { forgotPassword = false },
                    onSubmit = { email -> authViewModel.resetPassword(email); forgotPassword = false })
            }
        }
    }
}

@Composable
private fun OperationalShell(organizationId: OrganizationId, currentScope: NavigationScope,
    scopeProvider: () -> NavigationScope, request: Destination?, authorizer: DestinationAuthorizer) {
    val state = rememberOperationalStacks(organizationId)
    val currentScopeProvider by rememberUpdatedState(scopeProvider)
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(request,currentScope) {
        if (request is OrganizationDestination) state.open(request,{ currentScopeProvider() },authorizer,true)
    }
    val items = ShellSection.entries.map { section ->
        val icon = when(section) {
            ShellSection.Dashboard -> Icons.Default.Dashboard
            ShellSection.Campaigns -> Icons.AutoMirrored.Filled.Assignment
            ShellSection.Stocktaking -> Icons.Default.Inventory
            ShellSection.Locations -> Icons.Default.LocationOn
            ShellSection.More -> Icons.Default.MoreHoriz
        }
        SiloMathNavigationItem(section,section.title,icon)
    }
    SiloMathAdaptiveNavigation(items,state.activeSection,onSelect = { section ->
        coroutineScope.launch { state.open(ShellDestination(organizationId,section),
            { currentScopeProvider() },authorizer) }
    }) {
        Column(Modifier.fillMaxSize()) {
            AppHeader(state.activeSection.title,windowInsets = WindowInsets(0,0,0,0))
            val entries = state.decoratedEntries { key -> NavEntry(key) {
                val target = key as? OrganizationDestination
                // Scope validation occurs before protected content on every recompose/restore.
                if (target != null) ReauthorizedEntry(target,currentScope,{ currentScopeProvider() },authorizer) {
                    when(target) {
                        is ShellDestination -> when(target.section) {
                            ShellSection.Dashboard -> DashboardScreen()
                            else -> Box(Modifier.fillMaxSize()) // existing unimplemented feature boundary
                        }
                        else -> Box(Modifier.fillMaxSize()) // domain/feature owner not implemented yet
                    }
                }
            } }
            NavDisplay(entries = entries,onBack = state::back,
                modifier = Modifier.fillMaxSize())
        }
    }
}


/** Async domain revalidation before any restored/deep-linked protected content. */
@Composable
private fun ReauthorizedEntry(target: OrganizationDestination, scope: NavigationScope,
    scopeProvider: () -> NavigationScope, authorizer: DestinationAuthorizer,
    content: @Composable () -> Unit) {
    var allowed by remember(target,scope) { mutableStateOf(false) }
    var checking by remember(target,scope) { mutableStateOf(true) }
    LaunchedEffect(target,scope) {
        allowed = authorizeNavigation(listOf(target),scopeProvider,authorizer)
        checking = false
    }
    if (allowed && scopeAllows(target,scope)) content()
    else if (checking) Box(Modifier.fillMaxSize(),contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
