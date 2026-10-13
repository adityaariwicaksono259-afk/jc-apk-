package app.javin.codex

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.javin.codex.data.model.User
import app.javin.codex.data.repository.AuthRepository
import app.javin.codex.data.repository.AuthResult
import app.javin.codex.data.session.SessionManager
import app.javin.codex.ui.components.VideoBackground
import app.javin.codex.ui.screens.ForgotPasswordScreen
import app.javin.codex.ui.screens.HomeScreen
import app.javin.codex.ui.screens.LoginScreen
import app.javin.codex.ui.screens.RegisterScreen
import app.javin.codex.ui.screens.ResetPasswordScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Global state buat deep link reset token
    companion object {
        var pendingResetToken by mutableStateOf<String?>(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent { JavinAppRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        // Cek deep link reset password
        // Format: https://jvin.pages.dev/reset-password?token=xxx
        //     atau javincodex://reset?token=xxx
        if (data.host == "jvin.pages.dev" && data.path?.startsWith("/reset-password") == true) {
            val token = data.getQueryParameter("token")
            if (!token.isNullOrBlank()) {
                pendingResetToken = token
            }
        } else if (data.scheme == "javincodex" && data.host == "reset") {
            val token = data.getQueryParameter("token")
            if (!token.isNullOrBlank()) {
                pendingResetToken = token
            }
        }
    }
}

sealed class Screen {
    object Splash : Screen()
    object Login : Screen()
    object Register : Screen()
    object Home : Screen()
    object Forgot : Screen()
    data class ResetPassword(val token: String) : Screen()
}

@Composable
fun JavinAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var userName by remember { mutableStateOf<String?>(null) }

    val session = remember { SessionManager.getInstance(context) }
    val authRepo = remember { AuthRepository(session) }

    // Watch deep link token — kalau ada, langsung ke ResetPasswordScreen
    LaunchedEffect(MainActivity.pendingResetToken) {
        val token = MainActivity.pendingResetToken
        if (!token.isNullOrBlank()) {
            screen = Screen.ResetPassword(token)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        when (val current = screen) {
            is Screen.Splash -> {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    VideoBackground(
                        videoResId = R.raw.splash,
                        loop = false,
                        onVideoEnd = {
                            scope.launch {
                                // Kalau ada pending reset token, jangan ke Login
                                if (!MainActivity.pendingResetToken.isNullOrBlank()) {
                                    return@launch
                                }
                                if (authRepo.isLoggedIn()) {
                                    val result = authRepo.checkLogin()
                                    if (result is AuthResult.Success) {
                                        userName = result.data.name
                                        screen = Screen.Home
                                    } else {
                                        screen = Screen.Login
                                    }
                                } else {
                                    screen = Screen.Login
                                }
                            }
                        }
                    )
                }
            }

            is Screen.Login -> {
                LoginScreen(
                    authRepo = authRepo,
                    onLoginSuccess = { user: User ->
                        userName = user.name
                        screen = Screen.Home
                    },
                    onNavigateRegister = { screen = Screen.Register },
                    onNavigateResetPassword = { screen = Screen.Forgot }
                )
            }

            is Screen.Register -> {
                RegisterScreen(
                    authRepo = authRepo,
                    onRegisterSuccess = { user: User ->
                        userName = user.name
                        screen = Screen.Home
                    },
                    onNavigateLogin = { screen = Screen.Login }
                )
            }

            is Screen.ResetPassword -> {
                ResetPasswordScreen(
                    token = current.token,
                    authRepo = authRepo,
                    onBackToLogin = {
                        MainActivity.pendingResetToken = null
                        screen = Screen.Login
                    }
                )
            }

            is Screen.Forgot -> {
                ForgotPasswordScreen(
                    authRepo = authRepo,
                    onResetSuccess = { screen = Screen.Login },
                    onBackToLogin = { screen = Screen.Login }
                )
            }

            is Screen.Home -> {
                HomeScreen(
                    userName = userName,
                    onLogout = {
                        scope.launch {
                            authRepo.logout()
                            userName = null
                            screen = Screen.Login
                        }
                    },
                    onToolClick = { /* TODO: buka ToolScreen */ }
                )
            }
        }
    }
}
