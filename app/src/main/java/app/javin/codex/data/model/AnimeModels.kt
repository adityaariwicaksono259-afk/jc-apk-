package app.javin.codex.data.model

data class AnimeItem(
    val title: String,
    val url: String,
    val poster: String? = null,
    val source: String = "animasu",   // animasu / samehadaku / otakudesu
    val episode: String? = null,      // untuk ongoing/completed list
    val status: String? = null        // ongoing / completed
)

data class AnimeDetail(
    val title: String,
    val url: String,
    val poster: String? = null,
    val synopsis: String? = null,
    val genres: List<String> = emptyList(),
    val status: String? = null,
    val totalEpisode: String? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    val source: String = "animasu"
)

data class AnimeEpisode(
    val title: String,
    val url: String,
    val number: Int = 0,
    val date: String? = null
)

data class AnimeStreamResult(
    val streamUrl: String,
    val title: String,
    val source: String
)
