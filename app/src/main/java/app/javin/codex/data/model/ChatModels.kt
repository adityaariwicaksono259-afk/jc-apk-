package app.javin.codex.data.model

data class ChatUser(
    val code: String,
    val name: String,
    val avatar: String? = null,
    val bio: String? = null,
    val lastSeen: Long = 0
)

data class ChatMessage(
    val id: Long = 0,
    val roomId: Long = 0,
    val senderCode: String = "",
    val text: String = "",
    val createdAt: Long = 0,
    val readAt: Long = 0,
    val fromMe: Boolean = false
)

data class ChatRoom(
    val id: Long = 0,
    val other: ChatUser = ChatUser("", ""),
    val lastMessage: ChatMessage? = null,
    val unread: Int = 0,
    val lastMessageAt: Long = 0
)

data class ChatContact(
    val code: String,
    val name: String,
    val avatar: String? = null,
    val bio: String? = null,
    val isOnline: Boolean = false
)

data class TypingStatus(
    val otherTyping: Boolean = false,
    val myTyping: Boolean = false
)
