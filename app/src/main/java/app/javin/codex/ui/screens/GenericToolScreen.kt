package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Send
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
import app.javin.codex.ui.theme.LocalThemeTokens
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.Tool
import app.javin.codex.data.model.ToolParam
import app.javin.codex.data.repository.ToolRepository
import coil.compose.AsyncImage
import com.google.gson.Gson
import kotlinx.coroutines.launch

sealed class ToolOutput {
    data class Text(val text: String) : ToolOutput()
    data class Image(val url: String) : ToolOutput()
    data class Link(val url: String, val label: String = "") : ToolOutput()
    data class Video(val url: String, val thumb: String = "") : ToolOutput()
    data class Audio(val url: String) : ToolOutput()
    data class ListItems(val items: List<ToolOutput>) : ToolOutput()
    data class Error(val message: String) : ToolOutput()
    data class Raw(val json: String) : ToolOutput()
}

@Composable
fun GenericToolScreen(
    tool: Tool,
    onBack: () -> Unit = {},
    onPlayVideo: (url: String, title: String) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    val repo = remember { ToolRepository(ApiClient.get()) }
    val clipboard = LocalClipboardManager.current

    val params: List<ToolParam> = tool.params ?: emptyList()
    val values = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(tool.catalogId) {
        params.forEach { p ->
            val key = p.n ?: return@forEach
            values[key] = p.d ?: ""
        }
    }

    var loading by remember { mutableStateOf(false) }
    var outputs by remember { mutableStateOf<List<ToolOutput>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun callTool() {
        errorMsg = null
        outputs = emptyList()

        val catalogId = tool.catalogId ?: return

        // Kalau tool gak punya params → langsung call tanpa validasi
        val firstParam = params.firstOrNull()?.n
        val firstValue = if (firstParam != null) values[firstParam] ?: "" else ""

        if (firstParam != null && firstValue.isBlank()) {
            errorMsg = "Isi $firstParam dulu"
            return
        }

        scope.launch {
            loading = true
            val r = repo.callTool(catalogId, firstValue)
            loading = false
            r.onSuccess { raw ->
                outputs = parseOutput(raw)
            }.onFailure { errorMsg = it.message }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 48.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x26FFFFFF), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(tool.displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(tool.displayDesc, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // FORM PARAMS
            params.forEach { p ->
                val key = p.n ?: return@forEach
                val label = p.d ?: key
                val required = (p.r ?: 0) == 1

                Text(
                    (if (required) "$key *" else key).uppercase(),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = values[key] ?: "",
                    onValueChange = { values[key] = it; errorMsg = null },
                    placeholder = { Text(label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f)) },
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
            }

            if (params.isEmpty()) {
                Text("Tool tidak butuh input", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = { callTool() },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A9EFF), contentColor = Color.White)
            ) {
                if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Jalankan", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (errorMsg != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FF6B6B))
                        .border(1.dp, Color(0x66FF6B6B), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(errorMsg!!, color = Color(0xFFFFB0B0), fontSize = 13.sp)
                }
            }

            // RESULT
            if (outputs.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("HASIL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                outputs.forEach { output ->
                    RenderOutput(output, clipboard, onPlayVideo, tool.displayName)
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}

// ============================================
// PARSER — auto-detect tipe output dari raw JSON
// ============================================
private fun parseOutput(raw: String): List<ToolOutput> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return listOf(ToolOutput.Error("Response kosong"))

    val gson = Gson()

    // Coba parse JSON
    try {
        val parsed = gson.fromJson(trimmed, Any::class.java)
        val list = mutableListOf<ToolOutput>()

        when (parsed) {
            is Map<*, *> -> {
                // Cek status error
                val ok = parsed["ok"] as? Boolean ?: parsed["status"] as? Boolean ?: true
                if (!ok) {
                    val msg = (parsed["message"] as? String) ?: (parsed["error"] as? String) ?: "Gagal"
                    return listOf(ToolOutput.Error(msg))
                }
                // Khusus format stream (Animasu/Animekuindo/Dramabox/Reelshort):
                // cek field streamUrl / stream_url / videoUrl / playUrl / file
                val streamUrl = findStreamUrl(parsed, 0)
                if (streamUrl != null) {
                    list.add(ToolOutput.Video(streamUrl))
                }
                walkJson(parsed, list, 0)
            }
            is List<*> -> {
                parsed.forEach { item ->
                    if (item is Map<*, *>) walkJson(item, list, 0)
                    else if (item is String) {
                        val (kind, url) = detectUrlKind(item)
                        list.add(when (kind) {
                            "image" -> ToolOutput.Image(url)
                            "video" -> ToolOutput.Video(url)
                            "audio" -> ToolOutput.Audio(url)
                            "link" -> ToolOutput.Link(url)
                            else -> ToolOutput.Text(item)
                        })
                    }
                }
            }
            is String -> {
                // Plain string
                val (kind, url) = detectUrlKind(parsed)
                list.add(when (kind) {
                    "image" -> ToolOutput.Image(url)
                    "video" -> ToolOutput.Video(url)
                    "audio" -> ToolOutput.Audio(url)
                    "link" -> ToolOutput.Link(url)
                    else -> ToolOutput.Text(parsed)
                })
            }
            else -> list.add(ToolOutput.Text(parsed.toString()))
        }

        if (list.isEmpty()) return listOf(ToolOutput.Raw(trimmed))
        return list
    } catch (e: Exception) {
        // Bukan JSON — cek apakah URL image/video langsung
        val (kind, url) = detectUrlKind(trimmed)
        return when (kind) {
            "image" -> listOf(ToolOutput.Image(url))
            "video" -> listOf(ToolOutput.Video(url))
            "audio" -> listOf(ToolOutput.Audio(url))
            "link" -> listOf(ToolOutput.Link(url))
            else -> listOf(ToolOutput.Text(trimmed))
        }
    }
}

// Deteksi jenis URL
private fun detectUrlKind(s: String): Pair<String, String> {
    val t = s.trim()
    if (!t.startsWith("http")) return Pair("text", t)
    val lower = t.lowercase()
    // Blogger embed / iframe / player / embed
    if (lower.contains("blogger.com/video.g") || lower.contains("blogger.com/video")) return Pair("video", t)
    if (lower.contains("/embed") || lower.contains("/player") || lower.contains("iframe")) return Pair("video", t)
    return when {
        lower.matches(Regex(".*\\.(jpg|jpeg|png|webp|gif|bmp|svg)(\\?.*)?$")) -> Pair("image", t)
        lower.matches(Regex(".*\\.(mp4|webm|mov|mkv)(\\?.*)?$")) -> Pair("video", t)
        lower.matches(Regex(".*\\.(mp3|m4a|wav|ogg|opus|aac)(\\?.*)?$")) -> Pair("audio", t)
        lower.contains("/image") || lower.contains("/img") || lower.contains("image") -> Pair("image", t)
        lower.contains("/video") || lower.contains("/vid") -> Pair("video", t)
        else -> Pair("link", t)
    }
}

// Walk JSON cari value menarik (image/url/text)
private fun walkJson(obj: Map<*, *>, out: MutableList<ToolOutput>, depth: Int) {
    if (depth > 3) return

    obj.forEach { (k, v) ->
        when (v) {
            is String -> {
                val key = k?.toString()?.lowercase() ?: ""
                val (kind, url) = detectUrlKind(v)
                val isImageKey = key.contains("image") || key.contains("avatar") ||
                                key.contains("thumb") || key.contains("cover") ||
                                key.contains("photo") || key.contains("artwork") ||
                                key.contains("picture")
                val isVideoKey = key.contains("video") || key.contains("mp4") || key.contains("stream")
                val isAudioKey = key.contains("audio") || key.contains("mp3") || key.contains("sound")
                val isTextKey = key.contains("message") || key.contains("text") ||
                                key.contains("response") || key.contains("result") ||
                                key.contains("reply") || key.contains("answer") ||
                                key.contains("caption") || key.contains("desc")

                when {
                    kind == "image" || (isImageKey && v.startsWith("http")) ->
                        out.add(ToolOutput.Image(v))
                    kind == "video" || (isVideoKey && v.startsWith("http")) ->
                        out.add(ToolOutput.Video(v))
                    kind == "audio" || (isAudioKey && v.startsWith("http")) ->
                        out.add(ToolOutput.Audio(v))
                    isTextKey && v.length > 3 && !v.startsWith("http") ->
                        out.add(ToolOutput.Text(v))
                    kind == "link" -> out.add(ToolOutput.Link(v, k?.toString() ?: ""))
                }
            }
            is Map<*, *> -> walkJson(v, out, depth + 1)
            is List<*> -> {
                v.forEach { item ->
                    if (item is Map<*, *>) walkJson(item, out, depth + 1)
                    else if (item is String) {
                        val (kind, url) = detectUrlKind(item)
                        if (kind == "image") out.add(ToolOutput.Image(url))
                        else if (kind == "video") out.add(ToolOutput.Video(url))
                        else if (kind == "audio") out.add(ToolOutput.Audio(url))
                        else if (url.startsWith("http")) out.add(ToolOutput.Link(url))
                    }
                }
            }
        }
    }
}

// ============================================
// RENDER OUTPUT
// ============================================
@Composable
private fun RenderOutput(
    output: ToolOutput,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    onPlayVideo: (url: String, title: String) -> Unit = { _, _ -> },
    title: String = ""
) {
    when (output) {
        is ToolOutput.Text -> {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0x14FFFFFF))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(output.text, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 20.sp)
            }
        }
        is ToolOutput.Image -> {
            Column {
                AsyncImage(
                    model = output.url,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(8.dp))
                ActionChip(icon = Icons.Default.ContentCopy, label = "Copy Image URL") {
                    clipboard.setText(AnnotatedString(output.url))
                }
            }
        }
        is ToolOutput.Video -> {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF000000))
                        .clickable { onPlayVideo(output.url, title) }
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PlayCircle, null, tint = Color(0xFF4A9EFF), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("▶ Tonton Sekarang", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Tap buat streaming di APK", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip(icon = Icons.Default.ContentCopy, label = "Copy URL") {
                        clipboard.setText(AnnotatedString(output.url))
                    }
                }
            }
        }
        is ToolOutput.Audio -> {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1AFFFFFF))
                        .padding(16.dp)
                ) {
                    Text("🎵 Audio tersedia", fontSize = 13.sp, color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                ActionChip(icon = Icons.Default.ContentCopy, label = "Copy Audio URL") {
                    clipboard.setText(AnnotatedString(output.url))
                }
            }
        }
        is ToolOutput.Link -> {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Color(0x14FFFFFF))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    output.label.ifBlank { output.url },
                    fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(Color(0x334A9EFF))
                        .clickable { clipboard.setText(AnnotatedString(output.url)) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Copy", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        is ToolOutput.ListItems -> {
            output.items.forEach { RenderOutput(it, clipboard, onPlayVideo, title) }
        }
        is ToolOutput.Error -> {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Color(0x33FF6B6B))
                    .padding(12.dp)
            ) {
                Text(output.message, color = Color(0xFFFFB0B0), fontSize = 13.sp)
            }
        }
        is ToolOutput.Raw -> {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0x0FFFFFFF))
                    .padding(12.dp)
            ) {
                Text(output.json, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun ActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(Color(0x334A9EFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Cari field yang isinya URL streaming video (blogger/embed/mp4).
 */
private fun findStreamUrl(obj: Map<*, *>, depth: Int): String? {
    if (depth > 3) return null
    val streamKeys = setOf(
        "streamurl", "stream_url", "videourl", "video_url",
        "playurl", "play_url", "url_stream", "stream",
        "file", "playurl"
    )
    for ((k, v) in obj) {
        val key = k?.toString()?.lowercase() ?: continue
        if (v is String && key in streamKeys && v.startsWith("http")) {
            return v
        }
        if (v is Map<*, *>) {
            val found = findStreamUrl(v, depth + 1)
            if (found != null) return found
        }
    }
    return null
}
