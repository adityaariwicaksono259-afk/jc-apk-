package app.javin.codex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Icon
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
import app.javin.codex.ui.components.AppBackground
import app.javin.codex.ui.components.getToolIconUrl
import app.javin.codex.ui.theme.LocalThemeTokens
import app.javin.codex.ui.navigation.JavinBottomNav
import app.javin.codex.ui.navigation.NavTab

sealed class ProfileSubScreen {
    object Main : ProfileSubScreen()
    object Edit : ProfileSubScreen()
    object Privacy : ProfileSubScreen()
    object Notification : ProfileSubScreen()
    object Security : ProfileSubScreen()
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
    var openAmScreen by remember { mutableStateOf<String?>(null) }
    var openGenericTool by remember { mutableStateOf<Tool?>(null) }
    var profileSub by remember { mutableStateOf<ProfileSubScreen>(ProfileSubScreen.Main) }

    val anyOverlayOpen = openChatWithTool != null || openAmScreen != null || openGenericTool != null

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            // ============ BASE LAYER ============
            when {
                // Sub-screen profil
                profileSub is ProfileSubScreen.Edit -> EditProfileScreen(
                    authRepo = authRepo,
                    onBack = { profileSub = ProfileSubScreen.Main },
                    onSaved = { profileSub = ProfileSubScreen.Main }
                )
                profileSub is ProfileSubScreen.Privacy -> PrivacyScreen(
                    authRepo = authRepo,
                    onBack = { profileSub = ProfileSubScreen.Main }
                )
                profileSub is ProfileSubScreen.Notification -> NotificationScreen(
                    onBack = { profileSub = ProfileSubScreen.Main }
                )
                profileSub is ProfileSubScreen.Security -> PinSettingsScreen(
                    onBack = { profileSub = ProfileSubScreen.Main }
                )
                profileSub is ProfileSubScreen.About -> AboutScreen(
                    onBack = { profileSub = ProfileSubScreen.Main }
                )

                // Kategori (base) — tetap ke-render kalau tool overlay kebuka
                openCategory != null -> CategoryScreen(
                    categoryName = openCategory!!,
                    onBack = { openCategory = null },
                    onToolClick = { tool ->
                        when {
                            tool.displayFolder.equals("AI", ignoreCase = true) -> openChatWithTool = tool
                            tool.catalogId == "am-preset-search" -> openAmScreen = "search"
                            tool.catalogId == "premium-amfinder" -> openAmScreen = "finder"
                            tool.catalogId == "premium-amprem" -> openAmScreen = "prem"
                            else -> openGenericTool = tool
                        }
                    }
                )

                // Main tabs
                else -> Scaffold(
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
                                onCategoryClick = {
                                    openCategory = it
                                    openGenericTool = null
                                    openAmScreen = null
                                    openChatWithTool = null
                                }
                            )
                            NavTab.CHAT -> PlaceholderTab(
                                icon = Icons.Filled.Chat,
                                title = "Chat",
                                subtitle = "Chat antar user segera hadir"
                            )
                            NavTab.PLAYLIST -> PlaceholderTab(
                                icon = Icons.Filled.PlayCircle,
                                title = "Playlist",
                                subtitle = "Playlist segera hadir"
                            )
                            NavTab.STATUS -> PlaceholderTab(
                                icon = Icons.Filled.Timelapse,
                                title = "Status",
                                subtitle = "Status segera hadir"
                            )
                            NavTab.PROFIL -> ProfileScreen(
                                onLogout = onLogout,
                                onEditProfile = { profileSub = ProfileSubScreen.Edit },
                                onPrivacySettings = { profileSub = ProfileSubScreen.Privacy },
                                onNotificationSettings = { profileSub = ProfileSubScreen.Notification },
                                onSecuritySettings = { profileSub = ProfileSubScreen.Security },
                                onAbout = { profileSub = ProfileSubScreen.About }
                            )
                        }
                    }
                }
            }

            // ============ OVERLAY LAYER ============
            // AI Chat
            openChatWithTool?.let { tool ->
                AIChatScreen(
                    toolName = tool.displayName,
                    toolIconUrl = getToolIconUrl(tool.displayName, tool.catalogId),
                    catalogId = tool.catalogId,
                    onBack = { openChatWithTool = null }
                )
            }
            // AM Screens
            openAmScreen?.let { am ->
                when (am) {
                    "search" -> AmSearchScreen(onBack = { openAmScreen = null })
                    "finder" -> AmFinderScreen(onBack = { openAmScreen = null })
                    "prem" -> AmPremScreen(onBack = { openAmScreen = null })
                }
            }
            // Generic tool
            openGenericTool?.let { tool ->
                GenericToolScreen(
                    tool = tool,
                    onBack = { openGenericTool = null }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val t = LocalThemeTokens.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = t.accent,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = if (t.isNeo) FontWeight.Black else FontWeight.Bold,
                color = t.textPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = t.textMuted
            )
        }
    }
}
