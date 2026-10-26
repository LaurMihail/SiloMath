package com.silomath.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.silomath.feature.authentication.SignInScreen
import com.silomath.feature.authentication.ForgotPasswordSheet
import com.silomath.feature.dashboard.DashboardScreen
import com.silomath.feature.organization.SelectOrganizationScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SiloMathTheme {
                SiloMathApp()
            }
        }
    }
}

@Composable
fun SiloMathApp(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authUiState by authViewModel.uiState.collectAsState()
    val sessionState by authViewModel.sessionState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showForgotPasswordSheet by remember { mutableStateOf(false) }

    if (!authUiState.isSessionRestoreComplete) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val isLoggedIn =
        authUiState.isRestoredSessionReady && sessionState?.isLoggedIn == true
    val showShell = isLoggedIn && currentRoute != "login"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (showShell) {
                AppHeader(
                    title = when (currentRoute) {
                        "dashboard" -> "Dashboard"
                        "select_organization" -> "Select Organization"
                        else -> "SiloMath"
                    }
                )
            }
        },
        bottomBar = {
            if (showShell) {
                SiloMathBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) "select_organization" else "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                SignInScreen(
                    onSignInSuccess = {
                        navController.navigate("select_organization") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onForgotPasswordClick = { showForgotPasswordSheet = true },
                    viewModel = authViewModel
                )
            }
            composable("select_organization") {
                SelectOrganizationScreen(
                    onOrganizationSelected = {
                        navController.navigate("dashboard") {
                            popUpTo("select_organization") { inclusive = true }
                        }
                    }
                )
            }
            composable("dashboard") {
                DashboardScreen()
            }
            composable("campaigns") { /* Technical boundary */ }
            composable("stocktaking") { /* Technical boundary */ }
            composable("locations") { /* Technical boundary */ }
            composable("more") { /* Technical boundary */ }
        }

        if (showForgotPasswordSheet) {
            ForgotPasswordSheet(
                onDismiss = { showForgotPasswordSheet = false },
                onSubmit = { email ->
                    authViewModel.resetPassword(email)
                    showForgotPasswordSheet = false
                }
            )
        }
    }
}
