package com.devmatei.listencord

import android.content.ComponentName
import android.content.SharedPreferences
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService

// we don't care about notifications, being a listener is just what lets us
// read media sessions. the system keeps us bound, so no foreground service,
// no wakelocks and no polling. we only wake up when a player tells us something.
class MediaListener : NotificationListenerService() {
    private lateinit var prefs: Prefs
    private lateinit var presence: Presence
    private val handler = Handler(Looper.getMainLooper())
    private val controllers = mutableListOf<MediaController>()
    private var sessions: MediaSessionManager? = null

    private val component get() = ComponentName(this, MediaListener::class.java)

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { watch(it.orEmpty()) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onSessionDestroyed() = refresh()
    }

    private val prefsChanged = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in Prefs.ART_KEYS) Artwork.clear()
        presence.invalidate()
        if (key == "apps") rewatch() else refresh()
    }

    // players love firing a bunch of callbacks at once, wait for them to settle
    private val update = Runnable { presence.update(current()) }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        presence = Presence(this, prefs)
    }

    override fun onListenerConnected() {
        val m = getSystemService(MediaSessionManager::class.java)
        sessions = m
        m.addOnActiveSessionsChangedListener(sessionsChanged, component, handler)
        prefs.sp.registerOnSharedPreferenceChangeListener(prefsChanged)
        rewatch()
    }

    override fun onListenerDisconnected() {
        sessions?.removeOnActiveSessionsChangedListener(sessionsChanged)
        sessions = null
        prefs.sp.unregisterOnSharedPreferenceChangeListener(prefsChanged)
        unwatch()
        handler.removeCallbacks(update)
        presence.update(null)
    }

    override fun onDestroy() {
        handler.removeCallbacks(update)
        presence.stop()
        super.onDestroy()
    }

    private fun rewatch() {
        val m = sessions ?: return
        try {
            watch(m.getActiveSessions(component))
        } catch (_: SecurityException) {
            // listener access got revoked
        }
    }

    private fun watch(list: List<MediaController>) {
        unwatch()
        val wanted = prefs.apps
        for (c in list) {
            if (c.packageName !in wanted) continue
            c.registerCallback(callback, handler)
            controllers += c
        }
        refresh()
    }

    private fun unwatch() {
        controllers.forEach { it.unregisterCallback(callback) }
        controllers.clear()
    }

    private fun refresh() {
        handler.removeCallbacks(update)
        handler.postDelayed(update, 1000)
    }

    private fun current(): Track? {
        if (!prefs.enabled) return null
        val c = controllers.firstOrNull { playing(it.playbackState) } ?: controllers.firstOrNull() ?: return null
        val meta = c.metadata ?: return null
        val state = c.playbackState

        val title = meta.text(MediaMetadata.METADATA_KEY_TITLE)
            .ifBlank { meta.text(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) }
        if (title.isBlank()) return null
        val artist = meta.text(MediaMetadata.METADATA_KEY_ARTIST)
            .ifBlank { meta.text(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) }
            .ifBlank { meta.text(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) }

        val isPlaying = playing(state)
        var position = state?.position ?: 0L
        if (isPlaying && state != null && state.lastPositionUpdateTime > 0) {
            position += ((SystemClock.elapsedRealtime() - state.lastPositionUpdateTime) * state.playbackSpeed).toLong()
        }

        return Track(
            pkg = c.packageName,
            app = label(c.packageName),
            title = title,
            artist = artist,
            album = meta.text(MediaMetadata.METADATA_KEY_ALBUM),
            duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0),
            playing = isPlaying,
            start = System.currentTimeMillis() - position.coerceAtLeast(0),
        )
    }

    private fun playing(s: PlaybackState?) =
        s?.state == PlaybackState.STATE_PLAYING || s?.state == PlaybackState.STATE_BUFFERING

    private fun MediaMetadata.text(key: String) = getString(key)?.trim().orEmpty()

    private fun label(pkg: String) = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) {
        pkg
    }
}
