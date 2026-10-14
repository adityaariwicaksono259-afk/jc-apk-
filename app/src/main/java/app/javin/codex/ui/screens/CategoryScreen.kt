package app.javin.codex.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.javin.codex.data.api.ApiClient
import app.javin.codex.data.model.Tool
import app.javin.codex.data.repository.ToolRepository
import app.javin.codex.ui.components.GlassCard
import app.javin.codex.ui.components.getToolIconUrl
import kotlinx.coroutines.launch

@Composable
fun CategoryScreen(
    categoryName: String,
    onBack: () -> Unit = {},
    onToolClick: (Tool) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
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
        result.onSuccess { tools = it }
            .onFailure { errorMsg = it.message ?: "Gagal load" }
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
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
                    .background(Color(0x1AFFFFFF))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = categoryName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${tools.size} tools",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // ============================================
        // SEARCH BAR
        // ============================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Cari di ${categoryName}...", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
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
                    CircularProgressIndicator(color = Color(0xFF4A9EFF), strokeWidth = 3.dp)
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
                                    repo.getTools(true).onSuccess {
                                        tools = it.filter { t ->
                                            t.displayFolder.equals(categoryName, true)
                                        }
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
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isBlank()) "Belum ada tool di $categoryName"
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
                    items(visibleTools, key = { it.catalogId ?: it.displayName }) { tool ->
                        GlassCard(
                            name = tool.displayName,
                            iconUrl = getToolIconUrl(tool.displayName, tool.catalogId),
                            onClick = { onToolClick(tool) }
                        )
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }
}
