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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.repository.ToolRepository
import com.google.gson.Gson
import kotlinx.coroutines.launch

@Composable
fun AmFinderScreen(onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val repo = remember { ToolRepository(ApiClient.get()) }
    val clipboard = LocalClipboardManager.current

    var url by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var resultMap by remember { mutableStateOf<Map<String, Any?>?>(null) }

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
            Text("AM Finder", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                "Masukkan link TikTok berisi preset Alight Motion",
                fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it.trim(); errorMsg = null },
                placeholder = { Text("https://www.tiktok.com/...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f)) },
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
                onClick = {
                    errorMsg = null
                    resultMap = null
                    scope.launch {
                        loading = true
                        val r = repo.amFinder(url)
                        loading = false
                        r.onSuccess { raw ->
                            try {
                                val gson = Gson()
                                val map = gson.fromJson(raw, Map::class.java) as Map<*, *>
                                resultMap = map as Map<String, Any?>
                            } catch (e: Exception) {
                                errorMsg = "Response tidak valid"
                            }
                        }.onFailure { e ->
                            errorMsg = e.message ?: "Gagal"
                        }
                    }
                },
                enabled = url.isNotBlank() && !loading,
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
                    Text("Cari Preset", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

            if (resultMap != null) {
                Spacer(Modifier.height(20.dp))

                // Status
                val status = resultMap?.get("status")?.toString() ?: ""
                Text("HASIL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))

                // Parse results — cari array "presets" / "result" / "links"
                val candidates = listOf("presets", "result", "links", "data", "items")
                var foundList: List<*>? = null
                for (k in candidates) {
                    val v = resultMap?.get(k)
                    if (v is List<*> && v.isNotEmpty()) { foundList = v; break }
                }

                if (foundList != null) {
                    foundList.forEach { item ->
                        if (item is Map<*, *>) {
                            val presetUrl = (item["url"] ?: item["link"] ?: item["preset"] ?: "").toString()
                            val name = (item["name"] ?: item["title"] ?: item["author"] ?: "Preset").toString()
                            ResultCard(name = name, url = presetUrl, onCopy = {
                                clipboard.setText(AnnotatedString(presetUrl))
                            })
                            Spacer(Modifier.height(8.dp))
                        } else if (item is String) {
                            ResultCard(name = "Preset", url = item, onCopy = {
                                clipboard.setText(AnnotatedString(item))
                            })
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                } else {
                    // Fallback: tampil raw
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(Gson().toJson(resultMap), fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun ResultCard(name: String, url: String, onCopy: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(url, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(Color(0x334A9EFF))
                    .clickable(onClick = onCopy)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Copy Link", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
