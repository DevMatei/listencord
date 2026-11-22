package com.devmatei.listencord

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import night.milkyway.antisocialcord.DiscordRpcClient
import night.milkyway.antisocialcord.api.AbstractRpcEventHandler
import night.milkyway.antisocialcord.model.Activity
import night.milkyway.antisocialcord.model.ActivityAssets
import night.milkyway.antisocialcord.model.ActivityTimestamps
import night.milkyway.antisocialcord.model.ActivityType
import night.milkyway.antisocialcord.model.RpcPayload
import night.milkyway.antisocialcord.model.StatusDisplayType
import kotlin.math.abs

// twemoji studio microphone, pinned so it can't change under us
private const val TWEMOJI = "https://cdn.jsdelivr.net/gh/jdecked/twemoji@15.1.0/assets/72x72"
private const val GENERIC_PODCAST_ART = "$TWEMOJI/1f399.png"
private const val GENERIC_MUSIC_ART = "$TWEMOJI/1f3b5.png"

class Presence(context: Context, private val prefs: Prefs) {
    private val client = DiscordRpcClient(context.applicationContext)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var ready = false
    private var connecting = false
    private var sent: Track? = null
    private var pending: Activity? = null
    private var lookup: Job? = null
    private var idle: Job? = null
    private var timeout: Job? = null

    init {
        client.addListener(object : AbstractRpcEventHandler() {
            override fun onReady(payload: RpcPayload) {
                scope.launch {
                    ready = true
                    connecting = false
                    timeout?.cancel()
                    NowPlaying.status.value = Status.CONNECTED
                    flush()
                }
            }

            override fun onDisconnected(code: Int?) {
                scope.launch {
                    ready = false
                    connecting = false
                    sent = null
                    NowPlaying.status.value = Status.DISCONNECTED
                }
            }
        })
    }

    fun update(t: Track?) {
        NowPlaying.track.value = t
        if (t == null || !(t.playing || prefs.showPaused)) {
            clear()
            return
        }
        val last = sent
        if (last != null && same(last, t)) return
        sent = t
        idle?.cancel()

        // only hit the network when the album actually changes, Artwork caches the rest
        lookup?.cancel()
        lookup = scope.launch {
            val shown = Shown.of(t, prefs)
            val art = if (shown.art) Artwork.find(prefs, t) ?: prefs.fallbackArt.ifBlank { null } else null
            NowPlaying.art.value = art
            pending = build(t, shown, art)
            flush()
        }
    }

    // forget what we sent so the next update goes out even if nothing changed
    fun invalidate() {
        sent = null
    }

    fun stop() {
        disconnect()
        scope.cancel()
    }

    private fun clear() {
        lookup?.cancel()
        if (sent == null && pending == null) return
        sent = null
        pending = null
        NowPlaying.art.value = null
        if (ready) client.clearActivity()

        // let go of discord after a bit so we aren't keeping it awake for nothing
        idle?.cancel()
        idle = scope.launch {
            delay(60_000)
            disconnect()
        }
    }

    private fun flush() {
        val activity = pending ?: return
        if (ready) client.setActivity(activity) else connect()
    }

    private fun connect() {
        if (connecting) return
        val id = prefs.appId.trim().toLongOrNull()
        if (id == null) {
            NowPlaying.status.value = Status.NO_ID
            return
        }
        try {
            connecting = true
            client.connect(id)
            NowPlaying.status.value = Status.CONNECTING
        } catch (_: Exception) {
            connecting = false
            NowPlaying.status.value = Status.NO_DISCORD
            return
        }
        // discord never answered, give up until the next change
        timeout?.cancel()
        timeout = scope.launch {
            delay(15_000)
            if (!ready) {
                disconnect()
                NowPlaying.status.value = Status.NO_ANSWER
            }
        }
    }

    private fun disconnect() {
        timeout?.cancel()
        if (!ready && !connecting) return
        try {
            client.disconnect()
        } catch (_: Exception) {
        }
        ready = false
        connecting = false
        sent = null
        NowPlaying.status.value = Status.IDLE
    }

    // position updates come in all the time, only care if it actually jumped
    private fun same(a: Track, b: Track) =
        a.copy(start = 0) == b.copy(start = 0) && (!b.playing || abs(a.start - b.start) < 3000)

    private fun build(t: Track, shown: Shown, art: String?): Activity {
        val lastfm = if (shown.links) "https://www.last.fm/music/${Artwork.enc(t.artist)}" else null
        // with no image discord draws a grey question mark, an emoji looks better
        val image = when {
            shown.genericArt -> GENERIC_PODCAST_ART
            art != null -> art
            t.podcast -> GENERIC_PODCAST_ART
            else -> GENERIC_MUSIC_ART
        }

        return Activity(
            name = shown.name,
            activityType = ActivityType.entries.firstOrNull { it.value == prefs.type } ?: ActivityType.LISTENING,
            details = shown.details,
            state = shown.state,
            statusDisplayType = StatusDisplayType.entries.firstOrNull { it.value == prefs.display },
            stateUrl = lastfm?.takeIf { it.length <= 256 },
            detailsUrl = lastfm?.let { "$it/_/${Artwork.enc(t.title)}" }?.takeIf { it.length <= 256 },
            timestamps = if (shown.timestamps) ActivityTimestamps(t.start, t.start + t.duration) else null,
            assets = ActivityAssets(
                    largeImage = image,
                    largeText = shown.largeText,
                    largeUrl = null,
                    smallImage = null,
                    smallText = null,
                    smallUrl = null,
                ),
        )
    }
}
