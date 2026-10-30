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
) {
    fun fill(template: String) = template
        .replace("{title}", title)
        .replace("{artist}", artist)
        .replace("{album}", album)
        .replace("{app}", app)
        .trim()
}

// what the service is doing right now, so the ui can show it
object NowPlaying {
    val track = MutableStateFlow<Track?>(null)
    // whatever the player put in its session, only used for the preview in the app
    val cover = MutableStateFlow<Bitmap?>(null)
    val art = MutableStateFlow<String?>(null)
    val status = MutableStateFlow("idle")
}
