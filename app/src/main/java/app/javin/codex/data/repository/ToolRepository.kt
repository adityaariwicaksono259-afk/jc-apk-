package app.javin.codex.data.repository

import app.javin.codex.data.api.ApiService
import app.javin.codex.data.model.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToolRepository(private val api: ApiService) {

    private var cachedTools: List<Tool>? = null
    private var cachedFolders: List<String>? = null

    // ============================================
    // FETCH ALL TOOLS
    // ============================================
    suspend fun getTools(forceRefresh: Boolean = false): Result<List<Tool>> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedTools != null) {
            return@withContext Result.success(cachedTools!!)
        }
        try {
            val resp = api.getEndpoints()
            if (resp.isSuccessful && resp.body() != null) {
                // Parse List<Map> → List<Tool> manual (biar robust)
                val tools = resp.body()!!.mapNotNull { m ->
                    try {
                        Tool(
                            catalogId = m["catalogId"]?.toString(),
                            name = m["name"]?.toString(),
                            desc = m["desc"]?.toString(),
                            folder = m["folder"]?.toString(),
                            subfolder = m["subfolder"]?.toString(),
                            m = m["m"]?.toString(),
                            needKey = (m["needKey"] as? Boolean) ?: false,
                            params = null,
                            outputType = m["outputType"]?.toString()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                cachedTools = tools
                Result.success(tools)
            } else {
                Result.failure(Exception("HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // GET FOLDERS (kategori unik)
    // ============================================
    suspend fun getFolders(): Result<List<String>> = withContext(Dispatchers.IO) {
        cachedFolders?.let { return@withContext Result.success(it) }
        getTools().map { tools ->
            val folders = tools.map { it.displayFolder }.distinct().sorted()
            cachedFolders = folders
            folders
        }
    }

    // ============================================
    // FILTER by folder
    // ============================================
    suspend fun getToolsByFolder(folder: String): Result<List<Tool>> = withContext(Dispatchers.IO) {
        getTools().map { tools ->
            if (folder.isBlank() || folder.equals("Semua", true)) {
                tools
            } else {
                tools.filter { it.displayFolder.equals(folder, ignoreCase = true) }
            }
        }
    }

    // ============================================
    // CALL TOOL — hit /api/proxy-ext
    // ============================================
    suspend fun callTool(catalogId: String, query: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Hardcode param per endpoint (karena tiap upstream beda-beda)
            val resp = when (catalogId) {
                "ep005" -> api.callToolDynamic(catalogId, mapOf("text" to query))
                "ep120", "ep124", "ep125", "ep126", "ep127" -> api.callToolDynamic(catalogId, mapOf(
                    "prompt" to query,
                    "system" to "You are a helpful assistant.",
                    "temperature" to "0.7"
                ))
                else -> api.callTool(catalogId, query)  // default: q
            }
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${resp.code()}"))
            }
            val body = resp.body() ?: return@withContext Result.failure(Exception("Response kosong"))
            val text = body.string()

            // Coba parse JSON
            try {
                val gson = com.google.gson.Gson()
                val map = gson.fromJson(text, Map::class.java) as Map<*, *>

                // Handle error: {ok:false} ATAU {status:false}
                val okFlag = map["ok"] as? Boolean ?: map["status"] as? Boolean ?: true
                if (!okFlag) {
                    val msg = map["message"] as? String
                        ?: (map["error"] as? String)
                        ?: "Gagal dari server"
                    return@withContext Result.failure(Exception(msg))
                }

                // Extract text dari berbagai format
                val extracted = extractText(map, gson) ?: text
                Result.success(extracted)
            } catch (e: Exception) {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // SEARCH by name
    // ============================================
    suspend fun searchTools(query: String): Result<List<Tool>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getTools()
        getTools().map { tools ->
            val q = query.lowercase()
            tools.filter {
                it.displayName.lowercase().contains(q) ||
                it.displayDesc.lowercase().contains(q)
            }
        }
    }
    // Helper: extract text dari berbagai format response AI
    private fun extractText(map: Map<*, *>, gson: com.google.gson.Gson): String? {
        // 1. Direct string fields
        (map["reply"] as? String)?.let { if (it.isNotBlank()) return it }
        (map["result"] as? String)?.let { if (it.isNotBlank()) return it }
        (map["text"] as? String)?.let { if (it.isNotBlank()) return it }
        (map["message"] as? String)?.let { if (it.isNotBlank()) return it }
        (map["response"] as? String)?.let { if (it.isNotBlank()) return it }

        // 2. Nested object: data.message / data.response / data.text / data.result
        val data = map["data"]
        when (data) {
            is String -> if (data.isNotBlank()) return data
            is Map<*, *> -> {
                (data["message"] as? String)?.let { if (it.isNotBlank()) return cleanText(it) }
                (data["response"] as? String)?.let { if (it.isNotBlank()) return cleanText(it) }
                (data["text"] as? String)?.let { if (it.isNotBlank()) return cleanText(it) }
                (data["result"] as? String)?.let { if (it.isNotBlank()) return cleanText(it) }
                (data["reply"] as? String)?.let { if (it.isNotBlank()) return cleanText(it) }
                (data["answer"] as? String)?.let { if (it.isNotBlank()) return cleanText(it) }
                // Nested 2 level
                val inner = data["data"]
                if (inner is Map<*, *>) {
                    (inner["message"] as? String)?.let { if (it.isNotBlank()) return cleanText(it) }
                    (inner["response"] as? String)?.let { if (it.isNotBlank()) return cleanText(it) }
                    (inner["text"] as? String)?.let { if (it.isNotBlank()) return cleanText(it) }
                    (inner["result"] as? String)?.let { if (it.isNotBlank()) return cleanText(it) }
                }
                if (inner is String && inner.isNotBlank()) return cleanText(inner)
            }
            is List<*> -> {
                if (data.size == 1) {
                    val first = data[0]
                    if (first is String) return first
                    if (first is Map<*, *>) {
                        (first["message"] as? String)?.let { return it }
                        (first["response"] as? String)?.let { return it }
                        (first["text"] as? String)?.let { return it }
                    }
                }
            }
        }
        return null
    }

    private fun cleanText(s: String): String {
        var t = s.trim()
        // Strip tag <think>...</think> dari reasoning model (DeepSeek R1)
        t = t.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "").trim()
        // Kalau HTML error dari upstream
        if (t.startsWith("<") || t.contains("<h1>")) {
            return "Server AI ini sedang error. Coba AI lain."
        }
        // Kalau isinya kosong banget
        if (t.length < 2) return "AI tidak memberi jawaban. Coba lagi."
        return t
    }

}
