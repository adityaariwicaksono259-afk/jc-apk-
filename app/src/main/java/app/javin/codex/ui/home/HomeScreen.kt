package app.javin.codex.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.javin.codex.ui.chatlist.ChatListScreen
import app.javin.codex.ui.contacts.ContactsScreen
import app.javin.codex.ui.profile.ProfileScreen
import app.javin.codex.viewmodel.ChatListViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(
    onOpenChat: (String) -> Unit,
    onLoggedOut: () -> Unit,
    onOpenNewGroup: () -> Unit = {},
    onOpenContacts: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    var tab by remember { mutableStateOf(0) }
    val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val listVm = remember { ChatListViewModel() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                    label = { Text("Chats") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Contacts, contentDescription = null) },
                    label = { Text("Contatos") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Perfil") }
                )
            }
        }
    ) { insets ->
        when (tab) {
            0 -> ChatListScreen(
                myUid = myUid,
                vm = listVm,
                onOpenChat = onOpenChat,
                onOpenContacts = { tab = 1; onOpenContacts() },
                onOpenNewGroup = onOpenNewGroup,
                onOpenProfile = { tab = 2; onOpenProfile() },
                onLogout = onLoggedOut
            )
            1 -> ContactsScreen(
                myUid = myUid,
                onOpenChat = onOpenChat,
                modifier = Modifier.padding(insets)
            )
            2 -> ProfileScreen(
                onLoggedOut = onLoggedOut,
                modifier = Modifier.padding(insets)
            )
        }
    }
}
