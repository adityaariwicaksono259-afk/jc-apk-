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
import app.javin.codex.data.update.NotificationHelper
import app.javin.codex.data.repository.AuthResult
import app.javin.codex.data.session.SessionManager
import app.javin.codex.data.lock.LockManager
import app.javin.codex.ui.screens.LockScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.javin.codex.ui.components.GlassBackground
import app.javin.codex.ui.components.VideoBackground
import app.javin.codex.ui.screens.ForgotPasswordScreen
import app.javin.codex.ui.screens.LoginScreen
import app.javin.codex.ui.screens.MainScreen
import app.javin.codex.ui.screens.RegisterScreen
import app.javin.codex.ui.screens.ResetPasswordScreen
import app.javin.codex.ui.theme.JavinCodexTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        var pendingResetToken by mutableStateOf<String?>(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannels(this)
        handleIntent(intent)
        setContent { JavinAppRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.host == "jvin.pages.dev" && data.path?.startsWith("/reset-password") == true) {
            val token = data.getQueryParameter("token")
            if (!token.isNullOrBlank()) pendingResetToken = token
        } else if (data.scheme == "javincodex" && data.host == "reset") {
            val token = data.getQueryParameter("token")
            if (!token.isNullOrBlank()) pendingResetToken = token
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
    var isLocked by remember { mutableStateOf(false) }

    val session = remember { SessionManager.getInstance(context) }
    val authRepo = remember { AuthRepository(session) }
    val lockManager = remember { LockManager.getInstance(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Cek lock state saat app dibuka / resume
    LaunchedEffect(Unit) {
        if (lockManager.isLockEnabled()) {
            isLocked = lockManager.shouldLock()
        }
    }

    // Auto-lock saat app ke background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    scope.launch { lockManager.updateLastActive() }
                }
                Lifecycle.Event.ON_RESUME -> {
                    scope.launch {
                        if (lockManager.isLockEnabled()) {
                            val shouldLock = lockManager.shouldLock()
                            if (shouldLock) isLocked = true
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(MainActivity.pendingResetToken) {
        val token = MainActivity.pendingResetToken
        if (!token.isNullOrBlank()) {
            screen = Screen.ResetPassword(token)
        }
    }

    JavinCodexTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0F0F1E)) {
            if (isLocked) {
                LockScreen(onUnlock = {
                    isLocked = false
                    scope.launch { lockManager.updateLastActive() }
                })
                return@Surface
            }

            when (val current = screen) {
                is Screen.Splash -> {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        VideoBackground(
                            videoResId = R.raw.splash,
                            loop = false,
                            onVideoEnd = {
                                scope.launch {
                                    if (!MainActivity.pendingResetToken.isNullOrBlank()) return@launch
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

                is Screen.Forgot -> {
                    ForgotPasswordScreen(
                        authRepo = authRepo,
                        onResetSuccess = { screen = Screen.Login },
                        onBackToLogin = { screen = Screen.Login }
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

                is Screen.Home -> {
                    MainScreen(
                        userName = userName,
                        authRepo = authRepo,
                        onLogout = {
                            scope.launch {
                                authRepo.logout()
                                userName = null
                                screen = Screen.Login
                            }
                        }
                    )
                }
            }
        }
    }
}
