package app.javin.codex.data.model

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val artwork: String? = null,
    val streamUrl: String? = null,        // mp3 langsung
    val downloadUrl: String? = null,      // mp3 download
    val duration: Int = 0,                // detik
    val source: String = "audius",        // audius / spotify
    val album: String? = null,
    val playCount: Long = 0,
    val isDownloadable: Boolean = false
)

data class MusicAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val artwork: String? = null,
    val tracks: List<MusicTrack> = emptyList()
)

data class MusicPlaylist(
    val id: String,
    val title: String,
    val cover: String? = null,
    val trackCount: Int = 0,
    val tracks: List<MusicTrack> = emptyList()
)
