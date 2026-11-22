package com.devmatei.listencord

object Podcasts {
    // dedicated podcast players, everything they play is a podcast.
    // mixed apps like spotify or youtube music aren't here, those get sniffed per track
    val KNOWN = setOf(
        "de.danoeh.antennapod",
        "au.com.shiftyjelly.pocketcasts",
        "com.bambuna.podcastaddict",
        "fm.player",
        "fm.castbox.audiobook.radio.podcast",
        "com.podbean.app.podcast",
        "com.itunestoppodcastplayer.app",
        "com.google.android.apps.podcasts",
        "com.spreaker.android",
        "com.stitcher.app",
    )

    // "ep 12", "episode 4", "ep. 101", "#231"
    private val episode = Regex("""(?i)(\bep(isode)?\.?\s*#?\d+|#\d{2,})""")

    private const val LONG = 20 * 60 * 1000L

    // media3 players (and anything else on top of it) say what each item is, under this key
    const val MEDIA_TYPE_KEY = "androidx.media3.session.EXTRAS_KEY_MEDIA_TYPE_COMPAT"
    private const val TYPE_MUSIC = 1L
    private const val TYPE_PODCAST_EPISODE = 3L
    private const val TYPE_PODCAST = 16L

    fun detect(
        pkg: String,
        title: String,
        artist: String,
        album: String,
        genre: String,
        mediaId: String,
        duration: Long,
        mediaType: Long,
        apps: Set<String>,
    ): Boolean {
        if (pkg in apps) return true
        // when the player tells us, believe it. this is what makes mixed apps work properly
        if (mediaType == TYPE_PODCAST_EPISODE || mediaType == TYPE_PODCAST) return true
        if (mediaType == TYPE_MUSIC) return false
        if (genre.contains("podcast", true)) return true
        // spotify and friends put the content uri in the media id
        if (mediaId.contains("spotify:episode:") || mediaId.contains("/episode/") || mediaId.contains(":show:")) return true

        // the rest are guesses, only trust them on long stuff so a self-titled
        // album or "symphony #5" doesn't get caught
        if (duration < LONG) return false
        if (album.isNotBlank() && album.equals(artist, true)) return true
        return episode.containsMatchIn(title)
    }
}
