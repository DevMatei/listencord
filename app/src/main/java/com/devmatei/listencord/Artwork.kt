package com.devmatei.listencord

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
        val key = if (t.podcast) "pod|${t.show}".lowercase()
        else "${t.artist}|${t.album.ifBlank { t.title }}".lowercase()
        cache.get(key)?.let { return@withContext it.ifEmpty { null } }

        var failed = false
        fun attempt(on: Boolean, block: () -> String?) = if (!on) null else try {
            block()
        } catch (_: Exception) {
            failed = true
            null
        }

        // your own server and listenbrainz don't know podcasts, itunes does
        val url = if (t.podcast) {
            attempt(prefs.itunes) { itunesPodcast(t) }
        } else {
            attempt(prefs.hasNavidrome) { navidrome(prefs, t) }
                ?: attempt(prefs.lbToken.isNotBlank()) { listenbrainz(prefs.lbToken, t) }
                ?: attempt(prefs.itunes) { itunesMusic(t) }
                // a podcast the player called music, only trust an exact show name
                ?: attempt(prefs.itunes) { itunesPodcast(t, exact = true) }
        }

        // don't remember misses that were just us being offline
        if (url != null || !failed) cache.put(key, url ?: "")
        url
    }

    // collectionName is the show, artistName is whoever publishes it
    private fun itunesPodcast(t: Track, exact: Boolean = false): String? {
        val names = listOf(t.show, t.album, t.artist).filter { it.isNotBlank() }.distinct()
        for (name in names) {
            val results = itunes("term=${enc(name)}&media=podcast&entity=podcast&limit=10")
            best(results, exact) { norm(it.optString("collectionName")) to norm(name) }?.let { return it }
        }
        return null
    }

    // no setup needed, so this is what most people end up with.
    // results are full of tributes and karaoke, so the artist has to match too
    private fun itunesMusic(t: Track): String? {
        if (t.artist.isBlank()) return null
        val byAlbum = t.album.isNotBlank()
        val term = "${t.artist} ${if (byAlbum) t.album else t.title}"
        val entity = if (byAlbum) "album" else "song"
        val results = itunes("term=${enc(term)}&media=music&entity=$entity&limit=25")
        val artist = norm(t.artist)
        val wanted = norm(if (byAlbum) t.album else t.title)
        return best(results) { r ->
            if (!close(norm(r.optString("artistName")), artist)) return@best null
            norm(r.optString(if (byAlbum) "collectionName" else "trackName")) to wanted
        }
    }

    private fun itunes(query: String) =
        JSONObject(get("https://itunes.apple.com/search?$query")).optJSONArray("results") ?: JSONArray()

    // exact name match wins, otherwise the first one that's close
    private fun best(results: JSONArray, exact: Boolean = false, names: (JSONObject) -> Pair<String, String>?): String? {
        var loose: String? = null
        for (i in 0 until results.length()) {
            val r = results.getJSONObject(i)
            val (got, want) = names(r) ?: continue
            if (want.isEmpty()) continue
            val art = r.optString("artworkUrl600")
                .ifBlank { r.optString("artworkUrl100").replace("100x100bb", "600x600bb") }
                .takeIf { it.startsWith("http") } ?: continue
            if (got == want) return art
            if (!exact && loose == null && close(got, want)) loose = art
        }
        return loose
    }

    private fun close(a: String, b: String) = a.isNotEmpty() && b.isNotEmpty() && (a.contains(b) || b.contains(a))

    // "Pinkerton (Deluxe Edition)" and "pinkerton" should match
    private val junk = Regex("""\s*[(\[][^)\]]*(edition|remaster|deluxe|version|single|ep)[^)\]]*[)\]]|\s+-\s+(single|ep)$""", RegexOption.IGNORE_CASE)
    private fun norm(s: String) = s.replace(junk, "").lowercase().filter { it.isLetterOrDigit() }

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
        c.setRequestProperty("User-Agent", "listencord/${BuildConfig.VERSION_NAME}")
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
