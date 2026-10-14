
package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.repository.ToolRepository
import app.javin.codex.data.model.AppVersion
import app.javin.codex.data.update.NotificationHelper
import app.javin.codex.data.update.UpdateManager
import app.javin.codex.ui.components.DeviceStatsHeader
import app.javin.codex.ui.components.UpdateBanner
import androidx.compose.ui.platform.LocalContext
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
// Icon mapping per kategori (Material Icons)
private fun categoryIcon(name: String): ImageVector = when (name.lowercase()) {
    "ai" -> Icons.Default.SmartToy
    "anime" -> Icons.Default.Movie
    "canvas" -> Icons.Default.Palette
    "downloader" -> Icons.Default.Download
    "search" -> Icons.Default.Search
    "games" -> Icons.Default.SportsEsports
    "tools" -> Icons.Default.Build
    "random" -> Icons.Default.Casino
    "berita" -> Icons.Default.Article
    "primbon" -> Icons.Default.AutoAwesome
    "info" -> Icons.Default.Info
    "stalker" -> Icons.Default.PersonSearch
    "islami" -> Icons.Default.AutoAwesome
    "smm" -> Icons.Default.Share
    "uploader" -> Icons.Default.CloudUpload
    "sticker" -> Icons.Default.EmojiEmotions
    else -> Icons.Default.Widgets
}

// Gradient color per kategori
private fun categoryColors(name: String): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> = when (name.lowercase()) {
    "ai" -> androidx.compose.ui.graphics.Color(0xFF4A9EFF) to androidx.compose.ui.graphics.Color(0xFF7F5AF0)
    "anime" -> androidx.compose.ui.graphics.Color(0xFFFF7FE5) to androidx.compose.ui.graphics.Color(0xFF7F5AF0)
    "canvas" -> androidx.compose.ui.graphics.Color(0xFFFFB84D) to androidx.compose.ui.graphics.Color(0xFFFF7FE5)
    "downloader" -> androidx.compose.ui.graphics.Color(0xFF4A9EFF) to androidx.compose.ui.graphics.Color(0xFF7FFFA0)
    "search" -> androidx.compose.ui.graphics.Color(0xFF7FFFA0) to androidx.compose.ui.graphics.Color(0xFF4A9EFF)
    "games" -> androidx.compose.ui.graphics.Color(0xFFFF6B6B) to androidx.compose.ui.graphics.Color(0xFFFF7FE5)
    "tools" -> androidx.compose.ui.graphics.Color(0xFF7F5AF0) to androidx.compose.ui.graphics.Color(0xFF4A9EFF)
    else -> androidx.compose.ui.graphics.Color(0xFF4A9EFF) to androidx.compose.ui.graphics.Color(0xFF7F5AF0)
}

@Composable
fun HomeScreen(
    userName: String? = null,
    onCategoryClick: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val repo = remember { ToolRepository(ApiClient.get()) }

    val context = LocalContext.current

    var categories by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // ==== Update state ====
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
                .map { (folder, list) -> folder to list.size }
                .sortedByDescending { it.second }
        }.onFailure { e ->
            errorMsg = e.message ?: "Gagal load kategori"
        }
    }

    // ==== CEK UPDATE OTOMATIS ====
    LaunchedEffect(Unit) {
        val update = UpdateManager.checkForUpdate()
        if (update != null) {
            updateInfo = update
            // Kirim notif
            NotificationHelper.showUpdateNotification(context, update.version, update.notes)
        }
    }

    // ==== LISTENER DOWNLOAD SELESAI ====
    DisposableEffect(downloadId) {
        if (downloadId == -1L) return@DisposableEffect onDispose {}

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id == downloadId) {
                    downloading = false
                    downloadProgress = 100
                    UpdateManager.installApk(context)
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED)
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
        }
    }

    val visibleCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter { it.first.lowercase().contains(searchQuery.lowercase()) }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ============================================
        // HEADER
        // ============================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 16.dp)
        ) {
            // ==== UPDATE BANNER (kalau ada update) ====
            if (updateInfo != null && !updateDismissed) {
                UpdateBanner(
                    version = updateInfo!!.version,
                    notes = updateInfo!!.notes,
                    downloading = downloading,
                    progress = downloadProgress,
                    onUpdateClick = {
                        downloading = true
                        downloadProgress = 0
                        downloadId = UpdateManager.downloadApk(context, updateInfo!!.url)
                    },
                    onDismiss = { updateDismissed = true }
                )
                Spacer(Modifier.height(12.dp))
            }

            // ==== DEVICE STATS HEADER ====
            DeviceStatsHeader()

            Spacer(Modifier.height(16.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Cari kategori...", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x1AFFFFFF),
                    unfocusedContainerColor = Color(0x1AFFFFFF),
                    focusedBorderColor = Color(0x26FFFFFF),
                    unfocusedBorderColor = Color(0x26FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF4A9EFF)
                )
            )
        }

        // ============================================
        // CONTENT
        // ============================================
        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF4A9EFF), strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("Memuat kategori...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }
            }

            errorMsg != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            errorMsg!!,
                            color = Color(0xFFFF6B6B),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    loading = true
                                    errorMsg = null
                                    repo.getTools(true).onSuccess { allTools ->
                                        categories = allTools
                                            .groupBy { it.displayFolder }
                                            .map { (folder, list) -> folder to list.size }
                                            .sortedByDescending { it.second }
                                    }.onFailure { e -> errorMsg = e.message }
                                    loading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x334A9EFF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Coba Lagi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            visibleCategories.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isBlank()) "Belum ada kategori"
                            else "Nggak ketemu: \"$searchQuery\"",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleCategories, key = { it.first }) { (name, count) ->
                        CategoryCard(
                            name = name,
                            count = count,
                            onClick = { onCategoryClick(name) }
                        )
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    name: String,
    count: Int,
    onClick: () -> Unit
) {
    val (color1, color2) = categoryColors(name)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0x1FFFFFFF), Color(0x0DFFFFFF))
                )
            )
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .height(130.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Icon dengan gradient background
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(colors = listOf(color1, color2))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon(name),
                contentDescription = name,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        Column {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$count tools",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
