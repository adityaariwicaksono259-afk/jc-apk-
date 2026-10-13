package app.javin.codex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.Tool
import app.javin.codex.data.repository.ToolRepository
import app.javin.codex.ui.components.GlassBackground
import app.javin.codex.ui.components.GlassCard
import app.javin.codex.ui.components.getToolIconUrl
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    userName: String? = null,
    onLogout: () -> Unit = {},
    onToolClick: (Tool) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val repo = remember { ToolRepository(ApiClient.get()) }

    var tools by remember { mutableStateOf<List<Tool>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true
        errorMsg = null
        val result = repo.getTools()
        loading = false
        result.onSuccess { allTools ->
            tools = allTools.filter { it.displayFolder.equals("AI", ignoreCase = true) }
        }.onFailure { e ->
            errorMsg = e.message ?: "Gagal load tools"
        }
    }

    val visibleTools = remember(tools, searchQuery) {
        if (searchQuery.isBlank()) tools
        else {
            val q = searchQuery.lowercase()
            tools.filter { it.displayName.lowercase().contains(q) }
        }
    }

    GlassBackground {
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
                Text(
                    text = "Hai, ${userName ?: "Sobat"} 👋",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Mau ngapain hari ini?",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(20.dp))

                // ==== GLASS SEARCH BAR ====
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Cari tool AI...", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = Color(0x26FFFFFF),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x1AFFFFFF),
                        unfocusedContainerColor = Color(0x1AFFFFFF),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
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
                            CircularProgressIndicator(
                                color = Color(0xFF4A9EFF),
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Memuat tools...",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                errorMsg != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
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
                                        repo.getTools(true).onSuccess {
                                            tools = it.filter { t -> t.displayFolder.equals("AI", true) }
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

                visibleTools.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (searchQuery.isBlank()) "Belum ada tool AI"
                                else "Nggak ketemu: \"$searchQuery\"",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Section title
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color(0xFF4A9EFF), Color(0xFF7F5AF0))
                                            )
                                        )
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "FITUR AI",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f),
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "(${visibleTools.size})",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // List glass cards
                        items(visibleTools, key = { it.catalogId ?: it.displayName }) { tool ->
                            GlassCard(
                                name = tool.displayName,
                                iconUrl = getToolIconUrl(tool.displayName, tool.catalogId),
                                onClick = { onToolClick(tool) }
                            )
                        }

                        // Logout button (bottom)
                        item {
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = 1.dp,
                                        color = Color(0x33FF6B6B),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF6B6B)
                                )
                            ) {
                                Text(
                                    "Logout",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}
