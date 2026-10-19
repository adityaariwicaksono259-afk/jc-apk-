package app.javin.codex.viewmodel

import androidx.lifecycle.ViewModel
import app.javin.codex.data.ChatRepository
import app.javin.codex.model.Chat
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatListViewModel(
    private val repo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats = _chats.asStateFlow()

    private var reg: ListenerRegistration? = null

    fun start(myUid: String) {
        reg?.remove()
        reg = repo.observeChats(myUid) { list ->
            _chats.value = list
        }
    }

    fun stop() {
        reg?.remove()
        reg = null
    }
}
