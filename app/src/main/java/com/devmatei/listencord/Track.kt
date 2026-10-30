package com.devmatei.listencord

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
)

// what the service is doing right now, so the ui can show it
object NowPlaying {
    val track = MutableStateFlow<Track?>(null)
    val art = MutableStateFlow<String?>(null)
    val status = MutableStateFlow("idle")
}
