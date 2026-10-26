package com.silomath.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.silomath.core.designsystem.SiloMathTheme
import com.silomath.core.designsystem.components.AppHeader
import com.silomath.core.designsystem.components.SiloMathBottomNav
import com.silomath.feature.authentication.AuthViewModel
import com.silomath.feature.authentication.ForgotPasswordSheet
import com.silomath.feature.authentication.ResetPasswordScreen
import com.silomath.feature.authentication.SignInScreen
import com.silomath.feature.dashboard.DashboardScreen
import com.silomath.feature.masterdata.organization.SelectOrganizationScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val recoveryContext =
            intent
                ?.getStringExtra(
                    EXTRA_RECOVERY_CONTEXT
                )
                ?.takeIf { it.isNotBlank() }

        setContent {
            SiloMathTheme {
                SiloMathApp(
                    recoveryContext =
                        recoveryContext
                )
            }
        }
    }

    companion object {
        const val EXTRA_RECOVERY_CONTEXT =
            "com.silomath.app.extra.RECOVERY_CONTEXT"
    }
}

@Composable
fun SiloMathApp(
    recoveryContext: String? = null,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    val authUiState by
        authViewModel.uiState.collectAsState()

    val sessionState by
        authViewModel
            .sessionState
            .collectAsState()

    val navBackStackEntry by
        navController
            .currentBackStackEntryAsState()

    val currentRoute =
        navBackStackEntry
            ?.destination
            ?.route

    var showForgotPasswordSheet by
        remember {
            mutableStateOf(false)
        }

    val isRecoveryFlow =
        !recoveryContext.isNullOrBlank()

    if (
        !isRecoveryFlow &&
        !authUiState.isSessionRestoreComplete
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val isLoggedIn =
        authUiState.isRestoredSessionReady &&
            sessionState?.isLoggedIn == true

    val hasAuthorizedActiveOrganization =
        sessionState
            ?.hasAuthorizedActiveOrganization ==
            true

    val showShell =
        !isRecoveryFlow &&
            isLoggedIn &&
            currentRoute != "login" &&
            currentRoute !=
                "select_organization" &&
            currentRoute !=
                "reset_password"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (showShell) {
                AppHeader(
                    title =
                        when (currentRoute) {
                            "dashboard" ->
                                "Dashboard"
                            else ->
                                "SiloMath"
                        }
                )
            }
        },
        bottomBar = {
            if (showShell) {
                SiloMathBottomNav(
                    currentRoute =
                        currentRoute,
                    onNavigate = { route ->
                        navController
                            .navigate(route) {
                                popUpTo(
                                    navController
                                        .graph
                                        .startDestinationId
                                ) {
                                    saveState =
                                        true
                                }
                                launchSingleTop =
                                    true
                                restoreState =
                                    true
                            }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController =
                navController,
            startDestination =
                when {
                    isRecoveryFlow ->
                        "reset_password"
                    isLoggedIn &&
                        hasAuthorizedActiveOrganization ->
                        "dashboard"
                    isLoggedIn ->
                        "select_organization"
                    else ->
                        "login"
                },
            modifier =
                Modifier.padding(
                    innerPadding
                )
        ) {
            composable("login") {
                SignInScreen(
                    onSignInSuccess = {
                        navController
                            .navigate(
                                "select_organization"
                            ) {
                                popUpTo(
                                    "login"
                                ) {
                                    inclusive =
                                        true
                                }
                            }
                    },
                    onForgotPasswordClick = {
                        showForgotPasswordSheet =
                            true
                    },
                    viewModel =
                        authViewModel
                )
            }

            composable(
                "reset_password"
            ) {
                ResetPasswordScreen(
                    recoveryContext =
                        recoveryContext
                            .orEmpty(),
                    onResetSuccess = {
                        navController
                            .navigate("login") {
                                popUpTo(
                                    "reset_password"
                                ) {
                                    inclusive =
                                        true
                                }
                            }
                    },
                    onBackToSignIn = {
                        navController
                            .navigate("login") {
                                popUpTo(
                                    "reset_password"
                                ) {
                                    inclusive =
                                        true
                                }
                            }
                    },
                    viewModel =
                        authViewModel
                )
            }

            composable(
                "select_organization"
            ) {
                SelectOrganizationScreen(
                    onOrganizationSelected = {
                        navController
                            .navigate(
                                "dashboard"
                            ) {
                                popUpTo(
                                    "select_organization"
                                ) {
                                    inclusive =
                                        true
                                }
                            }
                    }
                )
            }

            composable("dashboard") {
                DashboardScreen()
            }

            composable("campaigns") {
                /* Technical boundary */
            }
            composable("stocktaking") {
                /* Technical boundary */
            }
            composable("locations") {
                /* Technical boundary */
            }
            composable("more") {
                /* Technical boundary */
            }
        }

        if (showForgotPasswordSheet) {
            ForgotPasswordSheet(
                onDismiss = {
                    showForgotPasswordSheet =
                        false
                },
                onSubmit = { email ->
                    authViewModel
                        .resetPassword(email)
                    showForgotPasswordSheet =
                        false
                }
            )
        }
    }
}