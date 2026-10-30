package com.devmatei.listencord.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devmatei.listencord.NowPlaying
import com.devmatei.listencord.Prefs
import com.devmatei.listencord.Track
import kotlinx.coroutines.delay

private val sample = Track("", "your player", "song title", "artist", "album", 0, true, 0)

private val verbs = mapOf(0 to "playing", 2 to "listening to", 3 to "watching", 5 to "competing in")

// roughly what people see on your profile
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PresencePreview(prefs: Prefs) {
    val tick = prefsTick(prefs)
    val live by NowPlaying.track.collectAsState()
    val cover by NowPlaying.cover.collectAsState()
    val t = live ?: sample
    val idle = live == null || !prefs.enabled

    val lines = remember(tick, t) {
        listOf(prefs.details, prefs.state, prefs.largeText).map { t.fill(it) }.filter { it.isNotEmpty() }
    }
    val header = remember(tick, t) {
        "${verbs[prefs.type] ?: "listening to"} ${t.fill(prefs.name).ifEmpty { "music" }}"
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                if (idle) "preview, nothing playing" else header,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.alpha(if (idle) 0.6f else 1f),
            ) {
                Crossfade(cover?.takeIf { live != null && prefs.showArt }, label = "cover") { bmp ->
                    if (bmp != null) {
                        Image(
                            bmp.asImageBitmap(), null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(88.dp).clip(MaterialTheme.shapes.large),
                        )
                    } else {
                        Box(
                            Modifier.size(88.dp).background(
                                MaterialTheme.colorScheme.primary,
                                MaterialShapes.Cookie9Sided.toShape(),
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote, null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    lines.forEachIndexed { i, line ->
                        Text(
                            line,
                            style = if (i == 0) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                            fontWeight = if (i == 0) FontWeight.SemiBold else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            AnimatedVisibility(!idle && t.playing && prefs.timestamps && t.duration > 0) {
                Progress(t)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Progress(t: Track) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // only ticks while the app is actually on screen, frames stop when it isn't
    LaunchedEffect(t) {
        while (true) {
            withFrameMillis { now = System.currentTimeMillis() }
            delay(1000)
        }
    }
    val pos = (now - t.start).coerceIn(0, t.duration)
    Column(Modifier.padding(top = 16.dp)) {
        LinearWavyProgressIndicator(
            progress = { pos.toFloat() / t.duration },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text(clock(pos), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(clock(t.duration), style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun clock(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
