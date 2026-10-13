package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.repository.AuthRepository
import app.javin.codex.data.repository.ToolRepository
import app.javin.codex.data.session.SessionManager
import app.javin.codex.ui.navigation.JavinBottomNav
import app.javin.codex.ui.navigation.NavTab
import androidx.compose.ui.platform.LocalContext

@Composable
fun MainScreen(
    userName: String?,
    authRepo: AuthRepository,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val session = remember { SessionManager.getInstance(context) }

    var selectedTab by remember { mutableStateOf(NavTab.HOME) }

    Scaffold(
        bottomBar = {
            JavinBottomNav(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                NavTab.HOME -> HomeTab(userName = userName)
                NavTab.CHAT -> PlaceholderTab("Chat", "Fitur chat bakal segera hadir")
                NavTab.PLAYLIST -> PlaceholderTab("Playlist", "Fitur playlist bakal segera hadir")
                NavTab.STATUS -> PlaceholderTab("Status", "Fitur status bakal segera hadir")
                NavTab.PROFIL -> ProfileScreen(
                    onLogout = onLogout,
                    onEditProfile = { /* TODO */ },
                    onPrivacySettings = { /* TODO */ },
                    onNotificationSettings = { /* TODO */ },
                    onAbout = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderTab(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HomeTab(userName: String?) {
    HomeScreen(
        userName = userName,
        onLogout = {},
        onToolClick = {}
    )
}

