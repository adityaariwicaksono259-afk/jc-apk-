package app.javin.codex.ui.screens

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.AppVersion
import app.javin.codex.data.repository.ToolRepository
import app.javin.codex.data.update.NotificationHelper
import app.javin.codex.data.update.UpdateManager
import app.javin.codex.ui.components.DeviceStatsHeader
import app.javin.codex.ui.components.UpdateBanner
import app.javin.codex.ui.theme.LocalThemeTokens
import app.javin.codex.ui.theme.pickAccent
import kotlinx.coroutines.launch

data class CategoryInfo(
    val name: String,
    val icon: ImageVector,
    val desc: String,
    val tag: String = "",
    val comingSoon: Boolean = false
)

private val COMING_SOON = setOf<String>()  // semua kategori dibuka

private fun getCategoryInfo(name: String): CategoryInfo {
    val base = getCategoryInfoBase(name)
    return base.copy(comingSoon = name.lowercase() in COMING_SOON)
}

private fun getCategoryInfoBase(name: String): CategoryInfo = when (name.lowercase()) {
    "ai" -> CategoryInfo("AI", Icons.Default.SmartToy, "ChatGPT, Claude, Gemini, DeepSeek, dll", "Chat")
    "anime" -> CategoryInfo("Anime", Icons.Default.Movie, "Anime populer, detail, jadwal rilis")
    "canvas" -> CategoryInfo("Canvas", Icons.Default.Palette, "AI image generator, canvas editor", "AI")
    "downloader" -> CategoryInfo("Downloader", Icons.Default.Download, "TikTok, YouTube, IG tanpa watermark", "No Watermark")
    "search" -> CategoryInfo("Search", Icons.Default.Search, "Search engine multi-source")
    "games" -> CategoryInfo("Games", Icons.Default.SportsEsports, "Game API, top up, info game")
    "tools" -> CategoryInfo("Tools", Icons.Default.Build, "AM Finder, utilities, converter", "Utility")
    "random" -> CategoryInfo("Random", Icons.Default.Casino, "Random generator, fun tools")
    "berita" -> CategoryInfo("Berita", Icons.Default.Article, "Berita terkini dari berbagai sumber", "Live")
    "primbon" -> CategoryInfo("Primbon", Icons.Default.AutoAwesome, "Ramalan, primbon Jawa, zodiak")
    "info" -> CategoryInfo("Info", Icons.Default.Info, "Info seputar teknologi & gadget")
    "stalker" -> CategoryInfo("Stalker", Icons.Default.PersonSearch, "IG, FF, Roblox, TikTok stalker", "Profile HD")
    "islami" -> CategoryInfo("Islami", Icons.Default.AutoAwesome, "Doa, jadwal sholat, kiblat")
    "smm" -> CategoryInfo("SMM", Icons.Default.Share, "Social media marketing tools")
    "uploader" -> CategoryInfo("Uploader", Icons.Default.CloudUpload, "Upload file ke CDN")
    "sticker" -> CategoryInfo("Sticker", Icons.Default.EmojiEmotions, "Sticker maker & download")
    else -> CategoryInfo(name, Icons.Default.Widgets, "Tools kategori $name")
}

@Composable
fun HomeScreen(
    userName: String? = null,
    onCategoryClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val t = LocalThemeTokens.current
    val repo = remember { ToolRepository(ApiClient.get()) }

    var categories by remember { mutableStateOf<List<CategoryInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    var updateInfo by remember { mutableStateOf<AppVersion?>(null) }
    var updateDismissed by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadId by remember { mutableStateOf(-1L) }

    LaunchedEffect(Unit) {
        loading = true
        errorMsg = null
        val result = repo.getTools()
        loading = false
        result.onSuccess { allTools ->
            categories = allTools
                .groupBy { it.displayFolder }
                .map { (folder, _) -> getCategoryInfo(folder) }
                .sortedBy { it.name }
        }.onFailure { e ->
            errorMsg = e.message ?: "Gagal load"
        }
    }

    val visibleCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter {
            it.name.lowercase().contains(searchQuery.lowercase()) ||
            it.desc.lowercase().contains(searchQuery.lowercase())
        }
    }

    LaunchedEffect(Unit) {
        val update = UpdateManager.checkForUpdate()
        if (update != null) {
            updateInfo = update
            NotificationHelper.showUpdateNotification(context, update.version, update.notes)
        }
    }

    DisposableEffect(downloadId) {
        if (downloadId == -1L) return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id == downloadId) {
                    downloading = false
                    UpdateManager.installApk(context)
                }
            }
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }
        } catch (e: Exception) {}
        onDispose { try { context.unregisterReceiver(receiver) } catch (e: Exception) {} }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 12.dp)
        ) {
            DeviceStatsHeader()
            Spacer(Modifier.height(12.dp))

            if (updateInfo != null && !updateDismissed) {
                UpdateBanner(
                    version = updateInfo!!.version,
                    notes = updateInfo!!.notes,
                    downloading = downloading,
                    progress = downloadProgress,
                    onUpdateClick = {
                        downloading = true
                        downloadId = UpdateManager.downloadApk(context, updateInfo!!.url)
                    },
                    onDismiss = { updateDismissed = true }
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari kategori...", fontSize = 14.sp, color = t.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = t.searchPlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = t.searchBg,
                    unfocusedContainerColor = t.searchBg,
                    focusedBorderColor = t.searchBorder,
                    unfocusedBorderColor = t.searchBorder,
                    focusedTextColor = t.searchText,
                    unfocusedTextColor = t.searchText,
                    cursorColor = t.accent
                )
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "SEMUA ALAT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = t.textMuted,
                letterSpacing = 1.5.sp
            )
        }

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = t.accent, strokeWidth = 3.dp)
                }
            }

            errorMsg != null -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMsg!!, color = t.danger, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    loading = true
                                    errorMsg = null
                                    repo.getTools(true).onSuccess { allTools ->
                                        categories = allTools.groupBy { it.displayFolder }
                                            .map { (folder, _) -> getCategoryInfo(folder) }
                                            .sortedBy { it.name }
                                    }.onFailure { e -> errorMsg = e.message }
                                    loading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = t.accentSoft)
                        ) { Text("Coba Lagi", color = Color.White) }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    items(visibleCategories, key = { it.name }) { cat ->
                        CategoryRow(info = cat, onClick = { onCategoryClick(cat.name) })
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 66.dp)
                                .height(1.dp)
                                .background(t.divider)
                        )
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(info: CategoryInfo, onClick: () -> Unit) {
    val t = LocalThemeTokens.current
    val accent = pickAccent(info.name, t.neoPalette, t.iconBg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(if (t.isNeo) 10.dp else 13.dp))
                .background(if (t.isNeo) accent else Color(0x14FFFFFF))
                .then(
                    if (t.isNeo) Modifier.border(2.dp, t.cardBorder, RoundedCornerShape(10.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = info.icon,
                contentDescription = info.name,
                tint = if (t.isNeo) t.iconTint else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = info.name,
                    fontSize = 15.sp,
                    fontWeight = if (t.isNeo) FontWeight.Black else FontWeight.Bold,
                    color = t.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (info.tag.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (t.isNeo) accent else Color(0x1AFFFFFF))
                            .then(if (t.isNeo) Modifier.border(1.5.dp, t.cardBorder, RoundedCornerShape(6.dp)) else Modifier)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = info.tag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (t.isNeo) t.accentOn else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = info.desc,
                fontSize = 12.sp,
                color = t.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = t.textMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}
