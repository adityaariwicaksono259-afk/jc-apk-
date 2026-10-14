package app.javin.codex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import app.javin.codex.data.model.Tool
import app.javin.codex.data.repository.AuthRepository
import app.javin.codex.data.session.SessionManager
import app.javin.codex.ui.components.GlassBackground
import app.javin.codex.ui.components.getToolIconUrl
import app.javin.codex.ui.navigation.JavinBottomNav
import app.javin.codex.ui.navigation.NavTab

// Sub-screen dalam Profil (di atas bottom nav)
sealed class ProfileSubScreen {
    object Main : ProfileSubScreen()
    object Edit : ProfileSubScreen()
    object Privacy : ProfileSubScreen()
    object Notification : ProfileSubScreen()
    object About : ProfileSubScreen()
}

@Composable
fun MainScreen(
    userName: String?,
    authRepo: AuthRepository,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val session = remember { SessionManager.getInstance(context) }

    var selectedTab by remember { mutableStateOf(NavTab.HOME) }
    var openChatWithTool by remember { mutableStateOf<Tool?>(null) }
    var openCategory by remember { mutableStateOf<String?>(null) }
    var profileSub by remember { mutableStateOf<ProfileSubScreen>(ProfileSubScreen.Main) }

    GlassBackground {
        val chatTool = openChatWithTool
        val category = openCategory

        when {
            // ==== AI CHAT ====
            chatTool != null -> {
                AIChatScreen(
                    toolName = chatTool.displayName,
                    toolIconUrl = getToolIconUrl(chatTool.displayName, chatTool.catalogId),
                    catalogId = chatTool.catalogId,
                    onBack = { openChatWithTool = null }
                )
            }

            // ==== KATEGORI ====
            category != null -> {
                CategoryScreen(
                    categoryName = category,
                    onBack = { openCategory = null },
                    onToolClick = { tool ->
                        // AI → chat, lainnya → belum diimplementasi
                        if (tool.displayFolder.equals("AI", ignoreCase = true)) {
                            openChatWithTool = tool
                        }
                        // TODO: kategori lain bikin ToolScreen masing-masing
                    }
                )
            }

            // ==== SUB-SCREEN PROFIL ====
            profileSub is ProfileSubScreen.Edit -> {
                EditProfileScreen(
                    authRepo = authRepo,
                    onBack = { profileSub = ProfileSubScreen.Main },
                    onSaved = { profileSub = ProfileSubScreen.Main }
                )
            }

            profileSub is ProfileSubScreen.Privacy -> {
                PrivacyScreen(
                    authRepo = authRepo,
                    onBack = { profileSub = ProfileSubScreen.Main }
                )
            }

            profileSub is ProfileSubScreen.Notification -> {
                NotificationScreen(
                    onBack = { profileSub = ProfileSubScreen.Main }
                )
            }

            profileSub is ProfileSubScreen.About -> {
                AboutScreen(
                    onBack = { profileSub = ProfileSubScreen.Main }
                )
            }

            // ==== MAIN TABS ====
            else -> {
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
                                onCategoryClick = { openCategory = it }
                            )

                            NavTab.CHAT -> PlaceholderTab(
                                emoji = "💬",
                                title = "Chat",
                                subtitle = "Chat antar user segera hadir"
                            )

                            NavTab.PLAYLIST -> PlaceholderTab(
                                emoji = "🎵",
                                title = "Playlist",
                                subtitle = "Playlist segera hadir"
                            )

                            NavTab.STATUS -> PlaceholderTab(
                                emoji = "📸",
                                title = "Status",
                                subtitle = "Status segera hadir"
                            )

                            NavTab.PROFIL -> ProfileScreen(
                                onLogout = onLogout,
                                onEditProfile = { profileSub = ProfileSubScreen.Edit },
                                onPrivacySettings = { profileSub = ProfileSubScreen.Privacy },
                                onNotificationSettings = { profileSub = ProfileSubScreen.Notification },
                                onAbout = { profileSub = ProfileSubScreen.About }
                            )
                        }
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
            Text(text = emoji, fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
