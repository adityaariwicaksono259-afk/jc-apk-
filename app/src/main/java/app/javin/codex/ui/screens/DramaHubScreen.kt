package app.javin.codex.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.repository.AnimeRepository
import app.javin.codex.ui.theme.LocalThemeTokens
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun DramaHubScreen(
    onBack: () -> Unit = {}
) {
    val t = LocalThemeTokens.current
    val scope = rememberCoroutineScope()
    val repo = remember { AnimeRepository(ApiClient.get()) }

    var searchQuery by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = true) { onBack() }

    // Load home Dramabox
    LaunchedEffect(Unit) {
        loading = true
        try {
            val r = repo.callRawEndpoint("ep062", emptyMap())
            items = parseDramaItems(r)
        } catch (e: Exception) {
            errorMsg = e.message
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
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
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = t.textPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Drama",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = t.textPrimary
            )
        }

        Text(
            text = "Drama pendek, Reelshort, Dramabox",
            fontSize = 12.sp,
            color = t.textMuted,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(12.dp))

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = t.accent, strokeWidth = 3.dp)
            }
            errorMsg != null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Mohon maaf, server drama belum siap\n${errorMsg}",
                    color = t.danger,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada drama", color = t.textMuted, fontSize = 13.sp)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it["url"] ?: it["title"] ?: it.hashCode().toString() }) { item ->
                    DramaPosterCard(
                        title = item["title"] ?: "",
                        poster = item["cover"] ?: item["image"] ?: item["poster"],
                        onClick = { /* TODO: detail */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun DramaPosterCard(
    title: String,
    poster: String?,
    onClick: () -> Unit
) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        if (!poster.isNullOrBlank()) {
            AsyncImage(
                model = poster,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(t.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(title.take(2), color = t.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )
    }
}

/**
 * Parse response drama (map/list JSON) -> list of {title, cover, url}
 */
private fun parseDramaItems(body: String): List<Map<String, String>> {
    val out = mutableListOf<Map<String, String>>()
    try {
        val gson = com.google.gson.Gson()
        val root = gson.fromJson(body, Any::class.java)
        fun walk(obj: Any?, depth: Int = 0) {
            if (depth > 6) return
            when (obj) {
                is Map<*, *> -> {
                    val title = (obj["title"] ?: obj["name"] ?: obj["bookName"]) as? String
                    val cover = (obj["cover"] ?: obj["image"] ?: obj["poster"] ?: obj["bookCover"]) as? String
                    val url = (obj["url"] ?: obj["link"] ?: obj["bookId"]) as? String
                    if (title != null && cover != null) {
                        out.add(mapOf(
                            "title" to title,
                            "cover" to cover,
                            "url" to (url ?: "")
                        ))
                    }
                    obj.values.forEach { walk(it, depth + 1) }
                }
                is List<*> -> obj.forEach { walk(it, depth + 1) }
            }
        }
        walk(root)
    } catch (e: Exception) { /* ignore */ }
    return out.distinctBy { it["url"] }
}
