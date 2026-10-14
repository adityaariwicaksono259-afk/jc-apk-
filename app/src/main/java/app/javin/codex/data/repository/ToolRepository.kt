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
            val resp = api.callTool(catalogId, query)
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${resp.code()}"))
            }
            val body = resp.body() ?: return@withContext Result.failure(Exception("Response kosong"))
            val text = body.string()

            // Coba parse JSON
            try {
                val gson = com.google.gson.Gson()
                val map = gson.fromJson(text, Map::class.java) as Map<*, *>
                val ok = map["ok"] as? Boolean ?: true
                if (!ok) {
                    val msg = map["message"] as? String ?: "Gagal dari server"
                    return@withContext Result.failure(Exception(msg))
                }
                // Cari field text/reply/result/message/data
                val extracted = map["reply"] as? String
                    ?: map["result"] as? String
                    ?: map["text"] as? String
                    ?: map["message"] as? String
                    ?: (map["data"]?.let { gson.toJson(it) })
                    ?: text
                Result.success(extracted)
            } catch (e: Exception) {
                // Bukan JSON — return raw
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
}
