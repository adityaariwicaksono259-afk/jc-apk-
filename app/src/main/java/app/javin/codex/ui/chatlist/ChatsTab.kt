// app/src/main/java/com/example/messageapp/ui/chatlist/ChatsTab.kt
package app.javin.codex.ui.chatlist

import androidx.compose.runtime.Composable
import app.javin.codex.viewmodel.ChatListViewModel

@Composable
fun ChatsTab(
    myUid: String,
    vm: ChatListViewModel,
    onOpenChat: (String) -> Unit,
    onOpenContacts: () -> Unit = {},
    onOpenNewGroup: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    ChatListScreen(
        myUid = myUid,
        vm = vm,
        onOpenChat = onOpenChat,
        onOpenContacts = onOpenContacts,
        onOpenNewGroup = onOpenNewGroup,
        onOpenProfile = onOpenProfile,
        onLogout = onLogout
    )
}
