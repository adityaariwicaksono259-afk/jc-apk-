package app.javin.codex.ui.components

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceInfo(
    val time: String,
    val battery: Int,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val deviceModel: String
)

@Composable
fun rememberDeviceInfo(): State<DeviceInfo> {
    val context = LocalContext.current
    val info = remember { mutableStateOf(readDeviceInfo(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            info.value = readDeviceInfo(context)
            delay(5000)  // update tiap 5 detik
        }
    }
    return info
}

fun readDeviceInfo(context: Context): DeviceInfo {
    // ==== TIME ====
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val time = timeFmt.format(Date())

    // ==== BATTERY ====
    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

    // ==== RAM ====
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    am.getMemoryInfo(memInfo)
    val totalMb = memInfo.totalMem / (1024 * 1024)
    val availMb = memInfo.availMem / (1024 * 1024)
    val usedMb = totalMb - availMb

    // ==== DEVICE MODEL ====
    val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    val model = Build.MODEL
    val deviceModel = if (model.startsWith(manufacturer, ignoreCase = true)) {
        model
    } else {
        "$manufacturer $model"
    }

    return DeviceInfo(
        time = time,
        battery = batteryPct,
        ramUsedMb = usedMb,
        ramTotalMb = totalMb,
        deviceModel = deviceModel
    )
}

@Composable
fun DeviceStatsHeader() {
    val info by rememberDeviceInfo()

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
            .padding(16.dp)
    ) {
        // ==== Row 1: Time + Device ====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = info.time,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = info.deviceModel,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1
            )
        }

        Spacer(Modifier.height(12.dp))

        // ==== Row 2: Battery + RAM ====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Battery
            StatChip(
                icon = "🔋",
                label = "Baterai",
                value = "${info.battery}%",
                modifier = Modifier.weight(1f)
            )
            // RAM
            StatChip(
                icon = "💾",
                label = "RAM",
                value = "${info.ramUsedMb} / ${info.ramTotalMb} MB",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatChip(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
