package app.javin.codex.data.repository

import app.javin.codex.data.api.ApiService
import app.javin.codex.data.model.ChatContact
import app.javin.codex.data.model.ChatMessage
import app.javin.codex.data.model.ChatRoom
import app.javin.codex.data.model.ChatUser
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class ChatRepository(private val api: ApiService) {

    private val gson = Gson()

    // ============================================
    // ROOMS — list + buka
    // ============================================
    suspend fun getRooms(): Result<List<ChatRoom>> = withContext(Dispatchers.IO) {
        try {
            val r = api.getChatRooms()
            if (!r.isSuccessful) return@withContext Result.failure(Exception("HTTP ${r.code()}"))
            val body = r.body()?.string() ?: return@withContext Result.failure(Exception("Kosong"))
            val root = gson.fromJson(body, Map::class.java) as Map<*, *>
            val rooms = root["rooms"] as? List<*> ?: return@withContext Result.success(emptyList())
            val out = rooms.mapNotNull { item ->
                if (item !is Map<*, *>) return@mapNotNull null
                val other = item["other"] as? Map<*, *>
                val last = item["lastMessage"] as? Map<*, *>
                ChatRoom(
                    id = (item["id"] as? Number)?.toLong() ?: 0,
                    other = ChatUser(
                        code = other?.get("code")?.toString() ?: "",
                        name = other?.get("name")?.toString() ?: "User",
                        avatar = other?.get("avatar")?.toString()?.ifBlank { null },
                        bio = other?.get("bio")?.toString()?.ifBlank { null },
                        lastSeen = (other?.get("lastSeen") as? Number)?.toLong() ?: 0,
                        isOfficial = other?.get("isOfficial") as? Boolean ?: false
                    ),
                    lastMessage = last?.let {
                        ChatMessage(
                            text = it["text"]?.toString() ?: "",
                            fromMe = it["fromMe"] as? Boolean ?: false,
                            createdAt = (it["time"] as? Number)?.toLong() ?: 0
                        )
                    },
                    unread = (item["unread"] as? Number)?.toInt() ?: 0,
                    lastMessageAt = (item["lastMessageAt"] as? Number)?.toLong() ?: 0,
                    isOfficial = item["isOfficial"] as? Boolean ?: false,
                    onlineCount = (item["onlineCount"] as? Number)?.toInt() ?: 0,
                    totalUsers = (item["totalUsers"] as? Number)?.toInt() ?: 0
                )
            }
            Result.success(out)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun openRoomWithUser(userCode: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val bodyJson = """{"target":"$userCode"}"""
            val reqBody = bodyJson.toRequestBody("application/json".toMediaType())
            val r = api.postChatRoom(reqBody)
            if (!r.isSuccessful) return@withContext Result.failure(Exception("HTTP ${r.code()}"))
            val body = r.body()?.string() ?: return@withContext Result.failure(Exception("Kosong"))
            val root = gson.fromJson(body, Map::class.java) as Map<*, *>
            val room = root["room"] as? Map<*, *>
            val id = (room?.get("id") as? Number)?.toLong()
                ?: (root["room_id"] as? Number)?.toLong()
                ?: 0L
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // MESSAGES
    // ============================================
    suspend fun getMessages(roomId: Long, since: Long = 0): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            val r = api.getChatMessages(roomId, since)
            if (!r.isSuccessful) return@withContext Result.failure(Exception("HTTP ${r.code()}"))
            val body = r.body()?.string() ?: return@withContext Result.failure(Exception("Kosong"))
            val root = gson.fromJson(body, Map::class.java) as Map<*, *>
            val msgs = root["messages"] as? List<*> ?: return@withContext Result.success(emptyList())
            val out = msgs.mapNotNull { item ->
                if (item !is Map<*, *>) return@mapNotNull null
                ChatMessage(
                    id = (item["id"] as? Number)?.toLong() ?: 0,
                    roomId = roomId,
                    senderCode = item["sender_code"]?.toString() ?: "",
                    senderName = item["sender_name"]?.toString() ?: "",
                    text = item["text"]?.toString() ?: "",
                    createdAt = (item["created_at"] as? Number)?.toLong() ?: 0,
                    readAt = (item["read_at"] as? Number)?.toLong() ?: 0,
                    fromMe = item["fromMe"] as? Boolean ?: false
                )
            }
            Result.success(out)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(roomId: Long, text: String): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val bodyJson = """{"room_id":$roomId,"text":${gson.toJson(text)}}"""
            val reqBody = bodyJson.toRequestBody("application/json".toMediaType())
            val r = api.postChatMessage(reqBody)
            if (!r.isSuccessful) return@withContext Result.failure(Exception("HTTP ${r.code()}"))
            val body = r.body()?.string() ?: return@withContext Result.failure(Exception("Kosong"))
            val root = gson.fromJson(body, Map::class.java) as Map<*, *>
            val msg = root["message"] as? Map<*, *>
            Result.success(
                ChatMessage(
                    id = (msg?.get("id") as? Number)?.toLong() ?: System.currentTimeMillis(),
                    roomId = roomId,
                    text = text,
                    createdAt = System.currentTimeMillis(),
                    fromMe = true
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // CONTACTS
    // ============================================
    suspend fun getContacts(): Result<List<ChatContact>> = withContext(Dispatchers.IO) {
        try {
            val r = api.getChatContacts()
            if (!r.isSuccessful) return@withContext Result.failure(Exception("HTTP ${r.code()}"))
            val body = r.body()?.string() ?: return@withContext Result.failure(Exception("Kosong"))
            val root = gson.fromJson(body, Map::class.java) as Map<*, *>
            val users = (root["contacts"] ?: root["users"]) as? List<*> ?: return@withContext Result.success(emptyList())
            val now = System.currentTimeMillis()
            val out = users.mapNotNull { item ->
                if (item !is Map<*, *>) return@mapNotNull null
                val lastSeen = (item["lastSeen"] as? Number)?.toLong()
                    ?: (item["last_seen"] as? Number)?.toLong() ?: 0
                ChatContact(
                    code = (item["code"] ?: item["user_code"])?.toString() ?: "",
                    name = item["name"]?.toString() ?: "User",
                    avatar = item["avatar"]?.toString()?.ifBlank { null },
                    bio = item["bio"]?.toString()?.ifBlank { null },
                    isOnline = (now - lastSeen) < 60_000
                )
            }
            Result.success(out)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // TYPING INDICATOR
    // ============================================
    suspend fun sendTyping(roomId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bodyJson = """{"room_id":$roomId}"""
            val reqBody = bodyJson.toRequestBody("application/json".toMediaType())
            api.postChatTyping(reqBody)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkTyping(roomId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val r = api.getChatTyping(roomId)
            if (!r.isSuccessful) return@withContext false
            val body = r.body()?.string() ?: return@withContext false
            val root = gson.fromJson(body, Map::class.java) as Map<*, *>
            root["typing"] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    // ============================================
    // BLOCK
    // ============================================
    suspend fun blockUser(userCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bodyJson = """{"target":"$userCode"}"""
            val reqBody = bodyJson.toRequestBody("application/json".toMediaType())
            val r = api.postChatBlock(reqBody)
            if (r.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${r.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun heartbeat(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.postChatHeartbeat()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
