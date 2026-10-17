package app.javin.codex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import app.javin.codex.ui.navigation.LiquidNavBar
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
    var playerUrl by remember { mutableStateOf<String?>(null) }
    var playerTitle by remember { mutableStateOf("Streaming") }
    var openDramaHub by remember { mutableStateOf(false) }
    var dramaTitle by remember { mutableStateOf<String?>(null) }
    var dramaPoster by remember { mutableStateOf<String?>(null) }
    var dramaUrl by remember { mutableStateOf<String?>(null) }
    var meloloTitle by remember { mutableStateOf<String?>(null) }
    var meloloEpisodes by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var meloloDetailUrl by remember { mutableStateOf<String?>(null) }
    var meloloProvider by remember { mutableStateOf("dramabox") }
    var musicTrack by remember { mutableStateOf<app.javin.codex.data.model.MusicTrack?>(null) }
    var musicPlaylist by remember { mutableStateOf<List<app.javin.codex.data.model.MusicTrack>>(emptyList()) }
    var musicIndex by remember { mutableStateOf(0) }
    var openChatRoomId by remember { mutableStateOf<Long?>(null) }
    var openChatOtherCode by remember { mutableStateOf("") }
    var openChatOtherName by remember { mutableStateOf("") }
    var openChatOtherAvatar by remember { mutableStateOf<String?>(null) }
    var openChatOtherBio by remember { mutableStateOf<String?>(null) }
    var openChatOtherLastSeen by remember { mutableStateOf(0L) }
    var showNewChatPicker by remember { mutableStateOf(false) }
    var dramaProvider by remember { mutableStateOf("dramabox") }
    var dramaPlayerUrl by remember { mutableStateOf<String?>(null) }
    var dramaPlayerTitle by remember { mutableStateOf("Episode") }
    var dramaEpisodeUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var dramaEpisodeTitles by remember { mutableStateOf<List<String>>(emptyList()) }
    var dramaEpisodeIndex by remember { mutableStateOf(-1) }
    var openAnimeHub by remember { mutableStateOf(false) }
    val t = LocalThemeTokens.current

    // ==== BackHandler Global — layered back navigation ====
    BackHandler(enabled = true) {
        when {
            // 0. Anime Hub kebuka → tutup Anime Hub
            openAnimeHub -> openAnimeHub = false
            // 0b. Streaming Hub kebuka → tutup Streaming Hub
            dramaPlayerUrl != null -> {
                dramaPlayerUrl = null
                dramaEpisodeIndex = -1
            }
            musicTrack != null -> musicTrack = null
            showNewChatPicker -> showNewChatPicker = false
            openChatRoomId != null -> openChatRoomId = null
            meloloTitle != null -> {
                meloloTitle = null
                meloloDetailUrl = null
            }
            dramaUrl != null -> dramaUrl = null
            openDramaHub -> openDramaHub = false
            // 1. Player kebuka → tutup player
            playerUrl != null -> playerUrl = null
            // 2. Chat AI kebuka → tutup chat
            openChatWithTool != null -> openChatWithTool = null
            // 3. AM screen kebuka → tutup AM
            openAmScreen != null -> openAmScreen = null
            // 4. Generic tool kebuka → tutup tool
            openGenericTool != null -> openGenericTool = null
            // 5. Sub-screen profil → balik ke profil main
            profileSub !is ProfileSubScreen.Main -> profileSub = ProfileSubScreen.Main
            // 6. Kategori anime kebuka → tutup kategori
            openCategory != null -> openCategory = null
            // 7. Bukan di Home tab → pindah ke Home
            selectedTab != NavTab.HOME -> selectedTab = NavTab.HOME
            // 8. Di Home → biarin default (keluar APK)
            else -> {
                // Do nothing → sistem handle (keluar APK)
                // Panggil activity.finish() kalau perlu
            }
        }
    }

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
                    bottomBar = {},
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
                                onCategoryClick = { cat ->
                                    when {
                                        cat.equals("Anime", ignoreCase = true) -> {
                                            openAnimeHub = true
                                        }
                                        cat.equals("Drama", ignoreCase = true) -> {
                                            openDramaHub = true
                                        }
                                        else -> {
                                            openCategory = cat
                                            openGenericTool = null
                                            openAmScreen = null
                                            openChatWithTool = null
                                        }
                                    }
                                }
                            )
                            NavTab.CHAT -> ChatListScreen(
                                onOpenRoom = { roomId, otherCode, otherName, otherAvatar, otherBio, otherLastSeen, onlineCount, totalUsers ->
                                    openChatRoomId = roomId
                                    openChatOtherCode = otherCode
                                    openChatOtherName = otherName
                                    openChatOtherAvatar = otherAvatar
                                    openChatOtherBio = otherBio
                                    openChatOtherLastSeen = otherLastSeen
                                    openChatOnlineCount = onlineCount
                                    openChatTotalUsers = totalUsers
                                },
                                onNewChat = { showNewChatPicker = true }
                            )
                            NavTab.PLAYLIST -> MusicHubScreen(
                                onTrackClick = { track, list, idx ->
                                    musicTrack = track
                                    musicPlaylist = list
                                    musicIndex = idx
                                }
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
            openChatWithTool?.let { tool ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(t.bg)
                ) {
                    AIChatScreen(
                        toolName = tool.displayName,
                        toolIconUrl = getToolIconUrl(tool.displayName, tool.catalogId),
                        catalogId = tool.catalogId,
                        onBack = { openChatWithTool = null }
                    )
                }
            }
            openAmScreen?.let { am ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(t.bg)
                ) {
                    when (am) {
                        "search" -> AmSearchScreen(onBack = { openAmScreen = null })
                        "finder" -> AmFinderScreen(onBack = { openAmScreen = null })
                        "prem" -> AmPremScreen(onBack = { openAmScreen = null })
                    }
                }
            }
            openGenericTool?.let { tool ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(t.bg)
                ) {
                    GenericToolScreen(
                        tool = tool,
                        onBack = { openGenericTool = null },
                        onPlayVideo = { url, title ->
                            playerUrl = url
                            playerTitle = title
                        }
                    )
                }
            }
            // Drama Hub
            if (openDramaHub) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(t.bg)
                ) {
                    DramaHubScreen(
                        onBack = { openDramaHub = false },
                        onDramaClick = { title, poster, url, provider ->
                            dramaTitle = title
                            dramaPoster = poster
                            dramaUrl = url
                            dramaProvider = provider
                        },
                        onPlayDrama = { title, url, provider ->
                            // Langsung ke Melolo mode (fetch episode di dalam DramaMeloloLoader)
                            meloloTitle = title
                            meloloEpisodes = emptyList()
                            meloloDetailUrl = url
                            meloloProvider = provider
                        }
                    )
                }
            }

            // Drama Melolo Mode
            meloloTitle?.let { title ->
                DramaMeloloScreen(
                    title = title,
                    episodes = meloloEpisodes,
                    detailUrl = meloloDetailUrl,
                    provider = meloloProvider,
                    onBack = {
                        meloloTitle = null
                        meloloDetailUrl = null
                    }
                )
            }

            // Drama Detail (full screen)
            dramaUrl?.let { url ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(t.bg)
                ) {
                    DramaDetailScreen(
                        title = dramaTitle ?: "",
                        poster = dramaPoster,
                        url = url,
                        provider = dramaProvider,
                        onBack = { dramaUrl = null },
                        onPlayEpisode = { epUrl, epTitle, idx ->
                            dramaPlayerUrl = epUrl
                            dramaPlayerTitle = epTitle
                            dramaEpisodeIndex = idx
                        },
                        onEpisodesLoaded = { urls, titles ->
                            dramaEpisodeUrls = urls
                            dramaEpisodeTitles = titles
                        },
                        onMeloloMode = { t, eps ->
                            meloloTitle = t
                            meloloEpisodes = eps
                        },
                        autoMelolo = true
                    )
                }
            }

            // Drama Player
            dramaPlayerUrl?.let { url ->
                AnimePlayerScreen(
                    videoUrl = url,
                    title = dramaPlayerTitle,
                    onBack = { dramaPlayerUrl = null },
                    onNextEpisode = {
                        val nextIdx = dramaEpisodeIndex + 1
                        if (nextIdx < dramaEpisodeUrls.size) {
                            dramaEpisodeIndex = nextIdx
                            dramaPlayerUrl = dramaEpisodeUrls[nextIdx]
                            dramaPlayerTitle = dramaEpisodeTitles.getOrNull(nextIdx) ?: "Episode ${nextIdx + 1}"
                        }
                    }
                )
            }

            // Anime Hub (full screen)
            if (openAnimeHub) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(t.bg)
                ) {
                    AnimeHubScreen(
                        onBack = { openAnimeHub = false },
                        onAnimeClick = { item ->
                            // TODO: navigate ke detail anime
                        }
                    )
                }
            }

            // Liquid nav bar (floating, di atas konten)
            if (!openAnimeHub && !openDramaHub &&
                openCategory == null && openGenericTool == null &&
                openChatWithTool == null && openAmScreen == null &&
                playerUrl == null && profileSub is ProfileSubScreen.Main) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    LiquidNavBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            }

            // Chat Room (full screen)
            openChatRoomId?.let { roomId ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(t.bg)
                ) {
                    ChatRoomScreen(
                        roomId = roomId,
                        otherCode = openChatOtherCode,
                        otherName = openChatOtherName,
                        otherAvatar = openChatOtherAvatar,
                        otherBio = openChatOtherBio,
                        otherLastSeen = openChatOtherLastSeen,
                        isGlobal = openChatOtherCode == "GLOBAL",
                        onlineCount = openChatOnlineCount,
                        totalUsers = openChatTotalUsers,
                        onBack = { openChatRoomId = null }
                    )
                }
            }

            // Chat Add Contact Dialog
            if (showNewChatPicker) {
                AddContactDialog(
                    onDismiss = { showNewChatPicker = false },
                    onSaved = { roomId, code, name ->
                        showNewChatPicker = false
                        openChatRoomId = roomId
                        openChatOtherCode = code
                        openChatOtherName = name
                        openChatOtherAvatar = null
                        openChatOtherBio = null
                        openChatOtherLastSeen = 0L
                    }
                )
            }

            // Music Player (paling atas)
            musicTrack?.let { track ->
                MusicPlayerScreen(
                    track = track,
                    playlist = musicPlaylist,
                    startIndex = musicIndex,
                    onBack = { musicTrack = null }
                )
            }

            // Anime Player (paling atas)
            playerUrl?.let { url ->
                AnimePlayerScreen(
                    videoUrl = url,
                    title = playerTitle,
                    onBack = { playerUrl = null }
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
