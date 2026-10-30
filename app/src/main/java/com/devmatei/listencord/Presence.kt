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

// everything here runs on the main thread
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
                    NowPlaying.status.value = "connected to discord"
                    flush()
                }
            }

            override fun onDisconnected(code: Int?) {
                scope.launch {
                    ready = false
                    connecting = false
                    sent = null
                    NowPlaying.status.value = "disconnected"
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
            val art = if (prefs.showArt) Artwork.find(prefs, t) ?: prefs.fallbackArt.ifBlank { null } else null
            NowPlaying.art.value = art
            pending = build(t, art)
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
            NowPlaying.status.value = "set your discord application id"
            return
        }
        try {
            connecting = true
            client.connect(id)
            NowPlaying.status.value = "connecting to discord"
        } catch (_: Exception) {
            connecting = false
            NowPlaying.status.value = "discord isn't installed"
            return
        }
        // discord never answered, give up until the next change
        timeout?.cancel()
        timeout = scope.launch {
            delay(15_000)
            if (!ready) {
                disconnect()
                NowPlaying.status.value = "discord didn't respond"
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
        NowPlaying.status.value = "idle"
    }

    // position updates come in all the time, only care if it actually jumped
    private fun same(a: Track, b: Track) =
        a.copy(start = 0) == b.copy(start = 0) && (!b.playing || abs(a.start - b.start) < 3000)

    private fun build(t: Track, art: String?): Activity {
        fun fill(template: String) = field(t.fill(template))

        val largeText = fill(prefs.largeText)
        val lastfm = if (prefs.links && t.artist.isNotBlank()) {
            "https://www.last.fm/music/${Artwork.enc(t.artist)}"
        } else null

        return Activity(
            name = fill(prefs.name) ?: "music",
            activityType = ActivityType.entries.firstOrNull { it.value == prefs.type } ?: ActivityType.LISTENING,
            details = fill(prefs.details),
            state = fill(prefs.state),
            statusDisplayType = StatusDisplayType.entries.firstOrNull { it.value == prefs.display },
            stateUrl = lastfm?.takeIf { it.length <= 256 },
            detailsUrl = lastfm?.let { "$it/_/${Artwork.enc(t.title)}" }?.takeIf { it.length <= 256 },
            timestamps = if (prefs.timestamps && t.playing && t.duration > 0) {
                ActivityTimestamps(t.start, t.start + t.duration)
            } else null,
            assets = if (art != null || largeText != null) {
                ActivityAssets(
                    largeImage = art,
                    largeText = largeText,
                    largeUrl = null,
                    smallImage = null,
                    smallText = null,
                    smallUrl = null,
                )
            } else null,
        )
    }

    // discord wants 2..128 chars
    private fun field(s: String): String? {
        val v = s.trim()
        if (v.isEmpty()) return null
        return v.padEnd(2, ' ').take(128)
    }
}
