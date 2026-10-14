package app.javin.codex.data.model

import com.google.gson.annotations.SerializedName

data class AppVersion(
    val ok: Boolean = false,
    val version: String = "1.0.0",
    @SerializedName("version_code")
    val versionCode: Int = 1,
    val url: String = "",
    val notes: String = "",
    @SerializedName("released_at")
    val releasedAt: Long = 0,
    @SerializedName("size_mb")
    val sizeMb: Float = 0f,
    val mandatory: Boolean = false
)
