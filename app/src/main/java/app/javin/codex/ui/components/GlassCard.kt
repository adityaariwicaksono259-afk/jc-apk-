package app.javin.codex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight

@Composable
fun GlassCard(
    name: String,
    iconUrl: String?,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x1FFFFFFF),
                        Color(0x0DFFFFFF)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color(0x26FFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ==== ICON (favicon) ====
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x1AFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (!iconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                // Fallback: initial letter
                Text(
                    text = name.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // ==== NAME ====
        Text(
            text = name,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF0F0F5),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // ==== CHEVRON ====
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0x99FFFFFF),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Mapping nama tool → domain favicon
 * Pakai Google Favicon API: https://www.google.com/s2/favicons?domain=X&sz=128
 */
fun getToolIconUrl(toolName: String, catalogId: String?): String? {
    val n = toolName.lowercase().replace(" ", "")
    val domain = when {
        n.contains("c.ai") || n.contains("cai") || n.contains("character") -> "character.ai"
        n.contains("gpt4") || n.contains("gpt-4") || n.contains("gpt4o") -> "openai.com"
        n.contains("gpt3") || n.contains("gpt-3") || n.contains("chatgpt") -> "openai.com"
        n.contains("claude") -> "claude.ai"
        n.contains("deepseek") || n.contains("deepsek") -> "deepseek.com"
        n.contains("felo") -> "felo.ai"
        n.contains("gemini") -> "gemini.google.com"
        n.contains("llama") || n.contains("meta") -> "meta.ai"
        n.contains("grok") -> "grok.com"
        n.contains("perplexity") -> "perplexity.ai"
        n.contains("mistral") -> "mistral.ai"
        n.contains("copilot") -> "copilot.microsoft.com"
        n.contains("bing") -> "bing.com"
        n.contains("bard") -> "google.com"
        n.contains("you.com") || n.contains("youai") -> "you.com"
        n.contains("phind") -> "phind.com"
        n.contains("blackbox") -> "blackbox.ai"
        n.contains("poe") -> "poe.com"
        n.contains("hugging") -> "huggingface.co"
        n.contains("replicate") -> "replicate.com"
        n.contains("midjourney") -> "midjourney.com"
        n.contains("stablediffusion") || n.contains("stability") -> "stability.ai"
        n.contains("dalle") -> "openai.com"
        n.contains("suno") -> "suno.com"
        n.contains("udio") -> "udio.com"
        n.contains("runway") -> "runwayml.com"
        n.contains("pika") -> "pika.art"
        n.contains("leonardo") -> "leonardo.ai"
        n.contains("ideogram") -> "ideogram.ai"
        n.contains("flux") -> "blackforestlabs.ai"
        else -> null
    }
    return if (domain != null) {
        "https://www.google.com/s2/favicons?domain=$domain&sz=128"
    } else null
}
