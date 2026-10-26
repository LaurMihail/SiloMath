package com.silomath.feature.authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.silomath.core.designsystem.EnterpriseNavy

@Composable
fun ResetPasswordScreen(
    recoveryContext: String,
    onResetSuccess: () -> Unit,
    onBackToSignIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val resetState by viewModel.passwordResetUiState.collectAsState()

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val hasRecoveryContext = recoveryContext.isNotBlank()
    val passwordsMatch =
        newPassword.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            newPassword == confirmPassword

    LaunchedEffect(resetState.isSuccess) {
        if (resetState.isSuccess) {
            newPassword = ""
            confirmPassword = ""
            viewModel.consumePasswordResetSuccess()
            onResetSuccess()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.headlineSmall,
                color = EnterpriseNavy
            )

            Text(
                text =
                    if (hasRecoveryContext) {
                        "Recovery context received."
                    } else {
                        "Recovery context is missing or invalid."
                    },
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (hasRecoveryContext) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasRecoveryContext && !resetState.isSubmitting,
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                visualTransformation =
                    if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {
                    IconButton(
                        onClick = { showPassword = !showPassword }
                    ) {
                        Icon(
                            imageVector =
                                if (showPassword) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                            contentDescription =
                                if (showPassword) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                }
                        )
                    }
                }
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm New Password") },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasRecoveryContext && !resetState.isSubmitting,
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                visualTransformation =
                    if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {
                    IconButton(
                        onClick = { showPassword = !showPassword }
                    ) {
                        Icon(
                            imageVector =
                                if (showPassword) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                            contentDescription =
                                if (showPassword) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                }
                        )
                    }
                },
                isError =
                    confirmPassword.isNotBlank() &&
                        newPassword != confirmPassword
            )

            if (
                confirmPassword.isNotBlank() &&
                newPassword != confirmPassword
            ) {
                Text(
                    text = "Passwords do not match.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            resetState.error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    viewModel.updatePassword(
                        recoveryContext = recoveryContext,
                        newPassword = newPassword
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled =
                    hasRecoveryContext &&
                        passwordsMatch &&
                        !resetState.isSubmitting
            ) {
                if (resetState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("RESET PASSWORD")
                }
            }

            TextButton(
                onClick = onBackToSignIn,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Back to Sign In")
            }
        }
    }
}