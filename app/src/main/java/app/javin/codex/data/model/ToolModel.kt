package app.javin.codex.data.model

import com.google.gson.annotations.SerializedName

data class Tool(
    @SerializedName("catalogId")
    val catalogId: String? = null,
    val name: String? = null,
    val desc: String? = null,
    val folder: String? = null,
    val subfolder: String? = null,
    val m: String? = null,
    @SerializedName("needKey")
    val needKey: Boolean = false,
    val params: List<ToolParam>? = null,
    @SerializedName("outputType")
    val outputType: String? = null
) {
    val displayName: String get() = name ?: catalogId ?: "Unknown"
    val displayDesc: String get() = desc ?: ""
    val displayFolder: String get() = folder ?: "Other"
}

data class ToolParam(
    val n: String? = null,      // nama param
    val t: String? = null,      // tipe (string, number)
    val r: Int? = null,         // required? 1=yes
    val d: String? = null       // default / deskripsi
)
