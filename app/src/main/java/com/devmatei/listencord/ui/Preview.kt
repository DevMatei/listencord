package com.devmatei.listencord.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devmatei.listencord.NowPlaying
import com.devmatei.listencord.Prefs
import com.devmatei.listencord.Shown
import com.devmatei.listencord.Track
import kotlinx.coroutines.delay

private val sample = Track("", "your player", "song title", "artist", "album", 0, true, 0)

private val verbs = mapOf(0 to "playing", 2 to "listening to", 3 to "watching", 5 to "competing in")

// roughly what people see on your profile, built from the same Shown the service sends
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PresencePreview(prefs: Prefs) {
    val tick = prefsTick(prefs)
    val live by NowPlaying.track.collectAsState()
    val cover by NowPlaying.cover.collectAsState()
    val t = live ?: sample
    val idle = live == null || !prefs.enabled
    val shown = remember(tick, t) { Shown.of(t, prefs) }
    val verb = remember(tick) { verbs[prefs.type] ?: "listening to" }
    val lines = listOfNotNull(shown.details, shown.state, shown.largeText)

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // discord's own green "online" dot, so it reads as a profile at a glance
                Box(Modifier.size(8.dp).background(if (idle) MaterialTheme.colorScheme.outline else Color(0xFF23A55A), CircleShape))
                Text(
                    if (idle) "preview, nothing playing" else "$verb ${shown.name.trim()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.alpha(if (idle) 0.6f else 1f),
            ) {
                Box {
                    val bmp = cover?.takeIf { live != null && shown.art }
                    Crossfade(if (shown.genericArt) null else bmp, label = "cover") { b ->
                        if (b != null) {
                            Image(
                                b.asImageBitmap(), null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)),
                            )
                        } else {
                            Box(
                                Modifier.size(80.dp).background(MaterialTheme.colorScheme.primary, MaterialShapes.Cookie9Sided.toShape()),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (t.podcast) Icons.Rounded.Podcasts else Icons.Rounded.MusicNote, null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(34.dp),
                                )
                            }
                        }
                    }
                    // the player's icon tucked in the corner, like discord does
                    if (!idle && t.pkg.isNotEmpty()) {
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .offset(6.dp, 6.dp)
                                .border(3.dp, MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                        ) { AppIcon(t.pkg, 26.dp) }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (lines.isEmpty()) {
                        Text(shown.name.trim(), style = MaterialTheme.typography.titleMedium)
                    }
                    lines.forEachIndexed { i, line ->
                        Text(
                            line.trim(),
                            style = if (i == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                            color = if (i == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            AnimatedVisibility(!idle && shown.timestamps) {
                Progress(t, Modifier.padding(top = 16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Progress(t: Track, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // only ticks while the app is actually on screen, frames stop when it isn't
    LaunchedEffect(t) {
        while (t.playing) {
            withFrameMillis { now = System.currentTimeMillis() }
            delay(1000)
        }
    }
    // a paused track's start was worked out when it paused, so freeze it there
    val frozen = remember(t) { System.currentTimeMillis() }
    val pos = ((if (t.playing) now else frozen) - t.start).coerceIn(0, t.duration)
    Column(modifier) {
        LinearWavyProgressIndicator(
            progress = { if (t.duration > 0) pos.toFloat() / t.duration else 0f },
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            amplitude = { if (t.playing) 1f else 0f },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text(clock(pos), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(clock(t.duration), style = MaterialTheme.typography.labelMedium)
        }
    }
}

fun clock(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}
