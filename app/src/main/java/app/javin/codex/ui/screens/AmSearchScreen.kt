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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.repository.ToolRepository
import coil.compose.AsyncImage
import com.google.gson.Gson
import kotlinx.coroutines.launch

data class AmVideo(
    val url: String = "",
    val handle: String = "",
    val snippet: String = "",
    val thumb: String = "",
    val found: Boolean = false,
    val presetLinks: List<String> = emptyList(),
    val message: String = ""
)

@Composable
fun AmSearchScreen(onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val repo = remember { ToolRepository(ApiClient.get()) }
    val clipboard = LocalClipboardManager.current

    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var videos by remember { mutableStateOf<List<AmVideo>>(emptyList()) }
    var selectedVideo by remember { mutableStateOf<AmVideo?>(null) }
    var presetResult by remember { mutableStateOf<String?>(null) }
    var presetLoading by remember { mutableStateOf(false) }

    val gson = remember { Gson() }

    fun doSearch() {
        errorMsg = null
        videos = emptyList()
        scope.launch {
            loading = true
            val r = repo.amSearch(query)
            loading = false
            r.onSuccess { raw ->
                try {
                    val map = gson.fromJson(raw, Map::class.java) as Map<*, *>
                    val arr = map["videos"] as? List<*> ?: emptyList<Any>()
                    videos = arr.mapNotNull { it ->
                        (it as? Map<*, *>)?.let { m ->
                            AmVideo(
                                url = m["url"]?.toString() ?: "",
                                handle = m["handle"]?.toString() ?: "",
                                snippet = m["snippet"]?.toString() ?: "",
                                thumb = m["thumb"]?.toString() ?: "",
                                found = (m["found"] as? Boolean) ?: false,
                                presetLinks = (m["presetLinks"] as? List<*>)?.mapNotNull { p -> p?.toString() } ?: emptyList(),
                                message = m["message"]?.toString() ?: ""
                            )
                        }
                    }
                } catch (e: Exception) {
                    errorMsg = "Response tidak valid"
                }
            }.onFailure { errorMsg = it.message }
        }
    }

    fun doFindPreset(url: String) {
        scope.launch {
            presetLoading = true
            presetResult = null
            val r = repo.amFinder(url)
            presetLoading = false
            r.onSuccess { presetResult = it }
                .onFailure { errorMsg = it.message }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 48.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x26FFFFFF), CircleShape)
                    .clickable(onClick = {
                        if (selectedVideo != null) { selectedVideo = null; presetResult = null }
                        else onBack()
                    }),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (selectedVideo == null) "AM Search" else "Cari Preset",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
        }

        if (selectedVideo == null) {
            // ==== SEARCH MODE ====
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; errorMsg = null },
                    placeholder = { Text("Cari nama lagu / keyword...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4A9EFF), unfocusedBorderColor = Color(0x1AFFFFFF),
                        cursorColor = Color(0xFF4A9EFF)
                    )
                )
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { doSearch() },
                    enabled = query.isNotBlank() && !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A9EFF), contentColor = Color.White,
                        disabledContainerColor = Color(0x334A9EFF), disabledContentColor = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cari Video", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (errorMsg != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Color(0x33FF6B6B))
                            .padding(12.dp)
                    ) {
                        Text(errorMsg!!, color = Color(0xFFFFB0B0), fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Video list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(videos) { v ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { selectedVideo = v }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (v.thumb.isNotBlank()) {
                            AsyncImage(
                                model = v.thumb,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(v.handle.ifBlank { "Video" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text(v.snippet.ifBlank { "-" }, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (v.found) {
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x337FFFA0))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Preset Tersedia", fontSize = 9.sp, color = Color(0xFF7FFFA0), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(60.dp)) }
            }
        } else {
            // ==== FIND PRESET MODE ====
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                val v = selectedVideo!!
                Text("Video dipilih:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                Text(v.handle.ifBlank { "Video" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(v.snippet, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 2)
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { doFindPreset(v.url) },
                    enabled = !presetLoading && v.url.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A9EFF), contentColor = Color.White)
                ) {
                    if (presetLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Cari Link Preset", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp))

                // Kalau video udah punya presetLinks
                if (v.presetLinks.isNotEmpty()) {
                    Text("PRESET LINK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(8.dp))
                    v.presetLinks.forEach { link ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(link, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 2, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x334A9EFF))
                                    .clickable { clipboard.setText(AnnotatedString(link)) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Copy", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (presetResult != null) {
                    Text("HASIL PENCARIAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color(0x14FFFFFF))
                            .padding(14.dp)
                    ) {
                        Text(presetResult!!, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
        }
    }
}
