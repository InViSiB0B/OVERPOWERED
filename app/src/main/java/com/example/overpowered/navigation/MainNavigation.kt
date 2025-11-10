package com.example.overpowered.navigation

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.overpowered.auth.PhoneAuthScreen
import com.example.overpowered.auth.VerificationCodeScreen
import com.example.overpowered.navigation.components.ErrorScreen
import com.example.overpowered.navigation.components.LoadingScreen
import com.example.overpowered.navigation.components.MainAppScaffold
import com.example.overpowered.onboarding.OnboardingScreen
import com.example.overpowered.navigation.components.ErrorScreen
import com.example.overpowered.navigation.components.LoadingScreen
import com.example.overpowered.viewmodel.AppViewModel
import com.example.overpowered.viewmodel.PhoneAuthState



@Composable
fun MainNavigation(
    viewModel: AppViewModel = viewModel()
) {
    val phoneAuthState by viewModel.phoneAuthState.collectAsState()
    val isOnboarded by viewModel.isOnboarded.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val activity = context as? Activity

    var savedPhoneNumber by remember { mutableStateOf("") }

    when (val state = phoneAuthState) {
        is PhoneAuthState.Initial,
        is PhoneAuthState.SendingCode -> {
            if (isLoading) {
                LoadingScreen()
            } else {
                PhoneAuthScreen(
                    onVerificationCodeSent = { _, phoneNumber ->
                        savedPhoneNumber = phoneNumber
                        activity?.let { viewModel.startPhoneAuth(phoneNumber, it) }
                    },
                    onError = {
                        // hook a snackbar/toast if you want
                    }
                )
            }
        }

        is PhoneAuthState.CodeSent -> {
            savedPhoneNumber = state.phoneNumber
            VerificationCodeScreen(
                phoneNumber = state.phoneNumber,
                verificationId = state.verificationId,
                onVerificationComplete = { code ->
                    viewModel.verifyPhoneCode(
                        verificationId = state.verificationId,
                        code = code,
                        phoneNumber = state.phoneNumber
                    )
                },
                onResendCode = {
                    activity?.let { viewModel.startPhoneAuth(state.phoneNumber, it) }
                },
                onError = {
                    // hook error UI if needed
                }
            )
        }

        is PhoneAuthState.VerifyingCode -> {
            LoadingScreen(message = "Verifying code...")
        }

        is PhoneAuthState.Success -> {
            when {
                !isOnboarded && !isLoading -> {
                    OnboardingScreen(
                        onComplete = { username ->
                            viewModel.completeOnboarding(
                                username = username,
                                phoneNumber = savedPhoneNumber
                            )
                        }
                    )
                }

                isOnboarded -> {
                    MainAppScaffold(viewModel = viewModel)
                }

                else -> {
                    LoadingScreen()
                }
            }
        }

        is PhoneAuthState.Error -> {
            ErrorScreen(
                message = state.message,
                onRetry = { viewModel.resetPhoneAuthState() }
            )
        }
    }
}
