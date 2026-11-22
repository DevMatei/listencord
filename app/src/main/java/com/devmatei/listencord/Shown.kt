package com.devmatei.listencord

// what ends up on discord for a track, worked out in one place so the
// in-app preview can't drift from the real thing
data class Shown(
    val name: String,
    val details: String?,
    val state: String?,
    val largeText: String?,
    // false means don't look up art at all
    val art: Boolean,
    // show the generic podcast icon instead of art
    val genericArt: Boolean,
    val timestamps: Boolean,
    val links: Boolean,
) {
    companion object {
        fun of(t: Track, p: Prefs): Shown {
            val mode = if (t.podcast) p.podcastMode else Prefs.PODCAST_FULL
            return when (mode) {
                Prefs.PODCAST_HIDDEN -> Shown(
                    name = "a podcast", details = null, state = null, largeText = null,
                    art = false, genericArt = true, timestamps = false, links = false,
                )
                Prefs.PODCAST_SHOW -> Shown(
                    name = field(t.show) ?: "a podcast",
                    details = field(t.show), state = field(t.app), largeText = null,
                    art = p.showArt, genericArt = false, timestamps = false, links = false,
                )
                else -> {
                    val details = field(t.fill(p.details))
                    val state = field(t.fill(p.state))
                    // discord prints large text as its own line now, so skip it when it
                    // just repeats one above (self titled albums, shows that put their name in both)
                    val large = field(t.fill(p.largeText))?.takeUnless { it.equals(state, true) || it.equals(details, true) }
                    Shown(
                        name = field(t.fill(p.name)) ?: if (t.podcast) "a podcast" else "music",
                        details = details,
                        state = state,
                        largeText = large,
                        art = p.showArt, genericArt = false,
                        timestamps = p.timestamps && t.playing && t.duration > 0,
                        // last.fm only knows music
                        links = p.links && !t.podcast && t.artist.isNotBlank(),
                    )
                }
            }
        }

        // discord wants 2..128 chars
        fun field(s: String): String? {
            val v = s.trim()
            if (v.isEmpty()) return null
            return v.padEnd(2, ' ').take(128)
        }
    }
}
