package app.javin.codex

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
import app.javin.codex.ui.screens.HomeScreen
import app.javin.codex.ui.screens.LoginScreen
import app.javin.codex.ui.screens.RegisterScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { JavinAppRoot() }
    }
}

sealed class Screen {
    object Splash : Screen()
    object Login : Screen()
    object Register : Screen()
    object Home : Screen()
}

@Composable
fun JavinAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var userName by remember { mutableStateOf<String?>(null) }

    val session = remember { SessionManager.getInstance(context) }
    val authRepo = remember { AuthRepository(session) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        when (screen) {
            is Screen.Splash -> {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    VideoBackground(
                        videoResId = R.raw.splash,
                        loop = false,
                        onVideoEnd = {
                            scope.launch {
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
                    onNavigateResetPassword = { email ->
                        // TODO: bikin ResetPasswordScreen nanti
                        // Sementara panggil reset endpoint langsung
                        scope.launch {
                            authRepo.resetPassword(email)
                        }
                    }
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
