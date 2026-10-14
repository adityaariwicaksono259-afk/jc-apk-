package app.javin.codex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.repository.AuthRepository
import app.javin.codex.data.session.SessionManager
import app.javin.codex.ui.components.GlassBackground
import app.javin.codex.ui.navigation.JavinBottomNav
import app.javin.codex.ui.navigation.NavTab

@Composable
fun MainScreen(
    userName: String?,
    authRepo: AuthRepository,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val session = remember { SessionManager.getInstance(context) }

    var selectedTab by remember { mutableStateOf(NavTab.HOME) }
    var openCategory by remember { mutableStateOf<String?>(null) }

    GlassBackground {
        if (openCategory != null) {
            // Kategori terbuka — full screen
            CategoryScreen(
                categoryName = openCategory!!,
                onBack = { openCategory = null },
                onToolClick = { /* TODO: ToolScreen */ }
            )
        } else {
            Scaffold(
                bottomBar = {
                    JavinBottomNav(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    when (selectedTab) {
                        NavTab.HOME -> HomeScreen(
                            userName = userName,
                            onCategoryClick = { category -> openCategory = category }
                        )

                        NavTab.CHAT -> PlaceholderTab(
                            emoji = "💬",
                            title = "Chat",
                            subtitle = "Fitur chat bakal segera hadir"
                        )

                        NavTab.PLAYLIST -> PlaceholderTab(
                            emoji = "🎵",
                            title = "Playlist",
                            subtitle = "Fitur playlist bakal segera hadir"
                        )

                        NavTab.STATUS -> PlaceholderTab(
                            emoji = "📸",
                            title = "Status",
                            subtitle = "Fitur status bakal segera hadir"
                        )

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
    }
}

@Composable
private fun PlaceholderTab(
    emoji: String,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                fontSize = 64.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
