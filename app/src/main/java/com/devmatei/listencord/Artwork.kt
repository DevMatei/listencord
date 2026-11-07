package com.devmatei.listencord

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Artwork {
    // album -> url, "" means we looked and found nothing
    private val cache = LruCache<String, String>(100)

    fun clear() = cache.evictAll()

    suspend fun find(prefs: Prefs, t: Track): String? = withContext(Dispatchers.IO) {
        val key = "${t.artist}|${t.album.ifBlank { t.title }}".lowercase()
        cache.get(key)?.let { return@withContext it.ifEmpty { null } }

        var failed = false
        fun attempt(block: () -> String?) = try {
            block()
        } catch (_: Exception) {
            failed = true
            null
        }

        // for podcast tracks, try iTunes Search API first (no auth needed)
        val url = (if (t.isPodcast()) attempt { itunes(t) } else null)
            ?: (if (prefs.hasNavidrome) attempt { navidrome(prefs, t) } else null)
            ?: (if (prefs.lbToken.isNotBlank()) attempt { listenbrainz(prefs.lbToken, t) } else null)

        // don't remember misses that were just us being offline
        if (url != null || !failed) cache.put(key, url ?: "")
        url
    }

    // iTunes Search API for podcast artwork (no auth required)
    // in players, album = episode title and artist = show name, so search by both
    private fun itunes(t: Track): String? {
        if (t.album.isBlank() && t.artist.isBlank()) return null
        val term = t.album.ifBlank { t.artist }
        val q = "term=${enc(term)}&media=podcast&entity=podcast&limit=10"
        val json = JSONObject(get("https://itunes.apple.com/search?$q"))
        val results = json.optJSONArray("results") ?: return null
        for (i in 0 until results.length()) {
            val r = results.getJSONObject(i)
            // narrow to results that match the show name when we have one
            if (t.artist.isNotBlank() &&
                !r.optString("artistName").contains(t.artist, true) &&
                !t.artist.contains(r.optString("artistName"), true)
            ) continue
            return r.optString("artworkUrl600")
                .ifBlank { r.optString("artworkUrl100") }
                .ifBlank { r.optString("artworkUrl30") }
                .takeIf { it.startsWith("http") }
        }
        return null
    }

    // same trick feishin uses: getAlbumInfo2 hands back a public /share/img url,
    // so the subsonic creds never end up on discord
    private fun navidrome(p: Prefs, t: Track): String? {
        val songs = subsonic(p, "search3", "query=${enc(t.title)}&songCount=50&albumCount=0&artistCount=0")
            .optJSONObject("searchResult3")?.optJSONArray("song") ?: return null

        var albumId = ""
        for (i in 0 until songs.length()) {
            val s = songs.getJSONObject(i)
            if (!s.optString("title").equals(t.title, true)) continue
            val artist = s.optString("artist")
            if (t.artist.isNotBlank() && artist.isNotBlank() &&
                !artist.contains(t.artist, true) && !t.artist.contains(artist, true)
            ) continue

            if (albumId.isEmpty()) albumId = s.optString("albumId")
            if (t.album.isNotBlank() && s.optString("album").equals(t.album, true)) {
                albumId = s.optString("albumId")
                break
            }
        }
        if (albumId.isEmpty()) return null

        val info = subsonic(p, "getAlbumInfo2", "id=${enc(albumId)}").optJSONObject("albumInfo") ?: return null
        return listOf("largeImageUrl", "mediumImageUrl", "smallImageUrl")
            .map { info.optString(it) }
            .firstOrNull { it.startsWith("http") }
    }

    private fun listenbrainz(token: String, t: Track): String? {
        if (t.artist.isBlank()) return null
        var q = "artist_name=${enc(t.artist)}&recording_name=${enc(t.title)}"
        if (t.album.isNotBlank()) q += "&release_name=${enc(t.album)}"
        val json = JSONObject(get("https://api.listenbrainz.org/1/metadata/lookup/?$q", "Token $token"))

        val caaMbid = json.optString("caa_release_mbid")
        val caaId = json.optLong("caa_id")
        if (caaMbid.isNotEmpty() && caaId > 0) return "https://coverartarchive.org/release/$caaMbid/$caaId-500.jpg"

        val release = json.optString("release_mbid")
        return if (release.isNotEmpty()) "https://coverartarchive.org/release/$release/front-500" else null
    }

    // returns null if it worked, otherwise what went wrong
    suspend fun testNavidrome(p: Prefs): String? = withContext(Dispatchers.IO) {
        try {
            subsonic(p, "ping", "")
            null
        } catch (e: Exception) {
            e.message ?: "couldn't connect"
        }
    }

    suspend fun testListenbrainz(token: String): String? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(get("https://api.listenbrainz.org/1/validate-token", "Token $token"))
            if (json.optBoolean("valid")) null else "token isn't valid"
        } catch (e: Exception) {
            e.message ?: "couldn't connect"
        }
    }

    private fun subsonic(p: Prefs, method: String, params: String): JSONObject {
        val auth = "u=${enc(p.ndUser)}&t=${p.ndToken}&s=${p.ndSalt}&v=1.16.1&c=listencord&f=json"
        val res = JSONObject(get("${p.ndUrl.trimEnd('/')}/rest/$method?$auth&$params"))
            .getJSONObject("subsonic-response")
        if (res.optString("status") != "ok") {
            throw IOException(res.optJSONObject("error")?.optString("message") ?: "server said no")
        }
        return res
    }

    private fun get(url: String, auth: String? = null): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "listencord/1.0")
        auth?.let { c.setRequestProperty("Authorization", it) }
        try {
            if (c.responseCode !in 200..299) throw IOException("http ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
