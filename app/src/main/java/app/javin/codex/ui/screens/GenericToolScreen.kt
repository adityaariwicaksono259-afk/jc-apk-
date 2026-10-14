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
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.Tool
import app.javin.codex.data.model.ToolParam
import app.javin.codex.data.repository.ToolRepository
import coil.compose.AsyncImage
import com.google.gson.Gson
import kotlinx.coroutines.launch

@Composable
fun GenericToolScreen(
    tool: Tool,
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val repo = remember { ToolRepository(ApiClient.get()) }
    val clipboard = LocalClipboardManager.current

    val params: List<ToolParam> = tool.params ?: emptyList()
    val values = remember { mutableStateMapOf<String, String>() }

    // Init default values dari example_values atau defaults
    LaunchedEffect(tool.catalogId) {
        params.forEach { p ->
            val key = p.n ?: return@forEach
            values[key] = p.d ?: ""
        }
    }

    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var resultRaw by remember { mutableStateOf<String?>(null) }
    var resultImageUrl by remember { mutableStateOf<String?>(null) }
    var resultMap by remember { mutableStateOf<Map<String, Any?>?>(null) }

    fun callTool() {
        errorMsg = null
        resultRaw = null
        resultImageUrl = null
        resultMap = null

        val catalogId = tool.catalogId ?: return
        val firstParam = params.firstOrNull()?.n ?: "q"
        val firstValue = values[firstParam] ?: ""

        if (firstValue.isBlank()) {
            errorMsg = "Isi ${firstParam} dulu"
            return
        }

        scope.launch {
            loading = true
            val r = repo.callTool(catalogId, firstValue)
            loading = false
            r.onSuccess { raw ->
                // Try parse as JSON
                try {
                    val gson = Gson()
                    val map = gson.fromJson(raw, Map::class.java) as Map<*, *>
                    resultMap = map as Map<String, Any?>

                    // Cari image URL
                    val imgFields = listOf("image", "img", "url", "avatar", "photo", "picture", "thumbnail", "result")
                    for (k in imgFields) {
                        val v = map[k]?.toString() ?: ""
                        if (v.startsWith("http") && (v.contains(".jpg") || v.contains(".png") || v.contains(".jpeg") || v.contains(".webp"))) {
                            resultImageUrl = v
                            break
                        }
                    }

                    // Cek nested data
                    val data = map["data"]
                    if (data is Map<*, *>) {
                        for (k in imgFields) {
                            val v = data[k]?.toString() ?: ""
                            if (v.startsWith("http") && (v.contains(".jpg") || v.contains(".png") || v.contains(".jpeg") || v.contains(".webp"))) {
                                resultImageUrl = v
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    resultRaw = raw
                }
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
                        .padding(12.dp)
                ) {
                    Text(errorMsg!!, color = Color(0xFFFFB0B0), fontSize = 13.sp)
                }
            }

            // RESULT
            if (resultImageUrl != null || resultRaw != null || resultMap != null) {
                Spacer(Modifier.height(20.dp))
                Text("HASIL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))

                if (resultImageUrl != null) {
                    AsyncImage(
                        model = resultImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color(0x334A9EFF))
                            .clickable { clipboard.setText(AnnotatedString(resultImageUrl!!)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Copy Image URL", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (resultRaw != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color(0x14FFFFFF))
                            .padding(14.dp)
                    ) {
                        Text(resultRaw!!, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                } else if (resultMap != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(Gson().toJson(resultMap), fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}
