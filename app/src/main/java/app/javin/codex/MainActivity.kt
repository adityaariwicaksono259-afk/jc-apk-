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
import app.javin.codex.data.repository.AuthRepository
import app.javin.codex.data.session.SessionManager
import app.javin.codex.ui.components.VideoBackground
import app.javin.codex.ui.screens.LoginScreen
import app.javin.codex.ui.screens.RegisterScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JavinAppRoot()
        }
    }
}

// ============================================
// SCREEN STATE
// ============================================
sealed class Screen {
    object Splash : Screen()
    object Login : Screen()
    object Register : Screen()
    object Home : Screen()  // TODO: ganti nanti
}

@Composable
fun JavinAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var userCode by remember { mutableStateOf<String?>(null) }

    // Init dependencies
    val session = remember { SessionManager.getInstance(context) }
    val authRepo = remember { AuthRepository(session) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        when (screen) {
            is Screen.Splash -> {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    VideoBackground(
                        videoResId = R.raw.splash,
                        loop = false,
                        onVideoEnd = {
                            scope.launch {
                                val loggedIn = authRepo.isLoggedIn()
                                if (loggedIn) {
                                    // Cek session masih valid
                                    val result = authRepo.checkLogin()
                                    screen = if (result is app.javin.codex.data.repository.AuthResult.Success) {
                                        userCode = result.data.userCode
                                        Screen.Home
                                    } else {
                                        Screen.Login
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
                    onLoginSuccess = { user ->
                        userCode = user.userCode
                        screen = Screen.Home
                    },
                    onNavigateRegister = {
                        screen = Screen.Register
                    }
                )
            }

            is Screen.Register -> {
                RegisterScreen(
                    authRepo = authRepo,
                    onRegisterSuccess = { user ->
                        userCode = user.userCode
                        screen = Screen.Home
                    },
                    onNavigateLogin = {
                        screen = Screen.Login
                    }
                )
            }

            is Screen.Home -> {
                // TODO: Home screen nanti
                Box(Modifier.fillMaxSize().background(Color.Black))
            }
        }
    }
}
