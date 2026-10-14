package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.Tool
import app.javin.codex.data.repository.ToolRepository
import app.javin.codex.ui.components.getToolIconUrl
import app.javin.codex.ui.theme.LocalThemeTokens
import app.javin.codex.ui.theme.pickAccent
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun CategoryScreen(
    categoryName: String,
    onBack: () -> Unit = {},
    onToolClick: (Tool) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val t = LocalThemeTokens.current
    val repo = remember { ToolRepository(ApiClient.get()) }

    var tools by remember { mutableStateOf<List<Tool>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(categoryName) {
        loading = true
        errorMsg = null
        val result = repo.getToolsByFolder(categoryName)
        loading = false
        result.onSuccess { allTools ->
            tools = allTools
        }.onFailure { errorMsg = it.message ?: "Gagal load" }
    }

    val visibleTools = remember(tools, searchQuery) {
        if (searchQuery.isBlank()) tools
        else {
            val q = searchQuery.lowercase()
            tools.filter {
                it.displayName.lowercase().contains(q) ||
                it.displayDesc.lowercase().contains(q)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ============================================
        // HEADER
        // ============================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(t.iconBg)
                    .then(if (t.isNeo) Modifier.border(2.dp, t.cardBorder, CircleShape) else Modifier)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = t.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = categoryName,
                    fontSize = 20.sp,
                    fontWeight = if (t.isNeo) FontWeight.Black else FontWeight.Bold,
                    color = t.textPrimary
                )
                Text(
                    text = "${tools.size} tools",
                    fontSize = 11.sp,
                    color = t.textMuted
                )
            }
        }

        // ============================================
        // SEARCH BAR
        // ============================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Cari di $categoryName...", fontSize = 14.sp, color = t.searchPlaceholder)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = t.searchPlaceholder)
                },
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
        }

        Spacer(Modifier.height(8.dp))

        // ============================================
        // CONTENT
        // ============================================
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
                                    repo.getTools(true).onSuccess {
                                        tools = it.filter { t -> t.displayFolder.equals(categoryName, true) }
                                    }.onFailure { e -> errorMsg = e.message }
                                    loading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = t.accentSoft)
                        ) { Text("Coba Lagi", color = Color.White) }
                    }
                }
            }

            visibleTools.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isBlank()) "Belum ada tool di $categoryName"
                        else "Nggak ketemu: \"$searchQuery\"",
                        color = t.textMuted,
                        fontSize = 13.sp
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    items(visibleTools, key = { it.catalogId ?: it.displayName }) { tool ->
                        ToolRow(
                            name = tool.displayName,
                            desc = tool.displayDesc,
                            tag = "",
                            iconUrl = getToolIconUrl(tool.displayName, tool.catalogId),
                            onClick = { onToolClick(tool) }
                        )
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
private fun ToolRow(
    name: String,
    desc: String,
    tag: String,
    iconUrl: String?,
    onClick: () -> Unit
) {
    val t = LocalThemeTokens.current
    val accent = pickAccent(name, t.neoPalette, t.iconBg)
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
                .background(if (t.isNeo) accent else t.iconBg)
                .then(
                    if (t.isNeo) Modifier.border(2.dp, t.cardBorder, RoundedCornerShape(10.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!iconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Icon(
                    imageVector = getToolIconVector(name),
                    contentDescription = name,
                    tint = if (t.isNeo) t.iconTint else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = if (t.isNeo) FontWeight.Black else FontWeight.Bold,
                    color = t.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (tag.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (t.isNeo) t.accent else Color(0x1AFFFFFF))
                            .then(if (t.isNeo) Modifier.border(1.5.dp, t.cardBorder, RoundedCornerShape(6.dp)) else Modifier)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (t.isNeo) t.accentOn else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            if (desc.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = t.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
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


/**
 * Return Material Icon vector untuk tool, fallback kalau favicon gak ada.
 */
private fun getToolIconVector(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    val n = name.lowercase()
    return when {
        n.contains("animasu") || n.contains("animekuindo") || n.contains("anime") -> Icons.Default.Movie
        n.contains("stream") || n.contains("tv") -> Icons.Default.Tv
        n.contains("search") -> Icons.Default.Search
        n.contains("download") -> Icons.Default.Download
        n.contains("canvas") || n.contains("image") || n.contains("ai image") -> Icons.Default.Palette
        n.contains("game") -> Icons.Default.SportsEsports
        n.contains("am") || n.contains("finder") -> Icons.Default.Search
        n.contains("primbon") || n.contains("zodiak") -> Icons.Default.AutoAwesome
        n.contains("berita") || n.contains("news") -> Icons.Default.Article
        n.contains("stalk") -> Icons.Default.PersonSearch
        n.contains("smm") || n.contains("social") -> Icons.Default.Share
        n.contains("upload") -> Icons.Default.CloudUpload
        n.contains("sticker") || n.contains("emoji") -> Icons.Default.EmojiEmotions
        n.contains("chat") || n.contains("gpt") || n.contains("claude") || n.contains("gemini") -> Icons.Default.SmartToy
        n.contains("tool") || n.contains("utility") -> Icons.Default.Build
        n.contains("random") -> Icons.Default.Casino
        n.contains("info") -> Icons.Default.Info
        else -> Icons.Default.Widgets
    }
}
