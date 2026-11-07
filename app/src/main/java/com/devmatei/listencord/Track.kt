package com.devmatei.listencord

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow

data class Track(
    val pkg: String,
    val app: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val playing: Boolean,
    // wall clock ms of when the song would have started at this position
    val start: Long,
    val genre: String = "",
) {
    fun fill(template: String) = template
        .replace("{title}", title)
        .replace("{artist}", artist)
        .replace("{album}", album)
        .replace("{app}", app)
        .trim()

    // known podcast app package names that may not set genre
    private val podcastPkgs = setOf(
        "com.ranisaurus.antennapod",
        "com.pocketcasts",
        "com.player.fm",
        "fm.player",
        "com.google.android.googlequicksearchbox",
        "com.spotify.music",
        "de.danoeh.antennapod",
    )

    // podcasts usually report artist == album (both = show name); many players don't set genre
    private fun looksLikePodcast(): Boolean =
        album.isNotBlank() && artist == album

    fun isPodcast(): Boolean =
        genre.contains("podcast", true) || pkg in podcastPkgs || looksLikePodcast()
}

// what the service is doing right now, so the ui can show it
object NowPlaying {
    val track = MutableStateFlow<Track?>(null)
    // whatever the player put in its session, only used for the preview in the app
    val cover = MutableStateFlow<Bitmap?>(null)
    val art = MutableStateFlow<String?>(null)
    val status = MutableStateFlow("idle")
}
