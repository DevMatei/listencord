package com.devmatei.listencord.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devmatei.listencord.NowPlaying
import com.devmatei.listencord.Prefs
import com.devmatei.listencord.Status
import com.devmatei.listencord.Track

@Composable
fun NowPage(prefs: Prefs, granted: Boolean, onGrant: () -> Unit, openPresence: () -> Unit) {
    val status by NowPlaying.status.collectAsState()
    val live by NowPlaying.track.collectAsState()
    val tick = prefsTick(prefs)
    val missingId = remember(tick) { prefs.appId.isBlank() }
    val noApps = remember(tick) { prefs.apps.isEmpty() }
    var enabled by remember { mutableStateOf(prefs.enabled) }

    Page("listencord") {
        AnimatedVisibility(!granted) {
            Setup(
                Icons.Rounded.NotificationsActive, "needs notification access",
                "android only lets apps see what's playing through this, nothing else is read. if it's greyed out, allow restricted settings in app info first.",
                "allow", onGrant,
            )
        }
        AnimatedVisibility(granted && missingId) {
            Setup(
                Icons.Rounded.Key, "needs a discord app",
                "make one at discord.com/developers, the name doesn't matter, then paste its id.",
                "set it", openPresence,
            )
        }

        NowCard(live, enabled, noApps)

        DiscordCard(enabled, status) {
            enabled = it
            prefs.enabled = it
        }

        Column {
            Header("on your profile")
            PresencePreview(prefs)
        }
    }
}

// big now playing card, tinted from the cover like the system media player does
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NowCard(t: Track?, enabled: Boolean, noApps: Boolean) {
    val cover by NowPlaying.cover.collectAsState()
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val accent by produceState<Color?>(null, cover) { value = cover?.let { accentOf(it) } }
    val container by animateColorAsState(
        accent?.copy(alpha = 0.28f)?.compositeOver(base) ?: base,
        tween(600),
        label = "tint",
    )
    val strong = accent ?: MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        AnimatedContent(
            t?.let { it.title to it.artist },
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
            label = "now",
        ) { key ->
            if (key == null || t == null) {
                when {
                    !enabled -> Empty("sharing is off", "flip it on below to start")
                    noApps -> Empty("pick a player in apps", "then hit play")
                    else -> Empty("nothing playing", "play something in one of your apps")
                }
                return@AnimatedContent
            }
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIcon(t.pkg, 22.dp)
                    Text(t.app, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), maxLines = 1)
                    if (t.podcast) Pill("podcast", MaterialTheme.colorScheme.tertiaryContainer, Icons.Rounded.Podcasts)
                    if (!t.playing) Pill("paused", icon = Icons.Rounded.Pause)
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val bmp = cover
                    if (bmp != null) {
                        Image(
                            bmp.asImageBitmap(), null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(112.dp).clip(RoundedCornerShape(24.dp)),
                        )
                    } else {
                        Spinner(if (t.podcast) Icons.Rounded.Podcasts else Icons.Rounded.MusicNote, t.playing, 112)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            t.title,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val sub = listOf(t.artist, t.album).filter { it.isNotBlank() }.distinct().joinToString(" · ")
                        if (sub.isNotEmpty()) {
                            Text(
                                sub,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (t.duration > 0) Progress(t, Modifier.padding(top = 20.dp), strong)
            }
        }
    }
}

@Composable
private fun Empty(title: String, sub: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Spinner(Icons.Rounded.MusicNote, false, 96)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// cookie shape that slowly turns while something plays
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Spinner(icon: ImageVector, spinning: Boolean, size: Int) {
    val turn = rememberInfiniteTransition(label = "spin")
    val angle by turn.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size.dp)
                .graphicsLayer { rotationZ = if (spinning) angle else 0f }
                .background(MaterialTheme.colorScheme.primary, MaterialShapes.Cookie9Sided.toShape()),
        )
        Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size((size * 0.42f).dp))
    }
}

@Composable
private fun DiscordCard(enabled: Boolean, status: Status, onChange: (Boolean) -> Unit) {
    val ok = status == Status.CONNECTED || status == Status.CONNECTING || status == Status.IDLE
    val container by animateColorAsState(
        if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "discord",
    )
    Card(
        onClick = { onChange(!enabled) },
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Badge(
                if (enabled && ok) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                48.dp,
            )
            Column(Modifier.weight(1f)) {
                Text(if (enabled) "sharing on discord" else "not sharing", style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(dot(status), CircleShape))
                    Text(status.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = enabled, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun dot(s: Status) = when (s) {
    Status.CONNECTED -> Color(0xFF23A55A)
    Status.CONNECTING -> Color(0xFFF0B232)
    Status.IDLE, Status.DISCONNECTED -> MaterialTheme.colorScheme.outline
    else -> MaterialTheme.colorScheme.error
}

@Composable
private fun Setup(icon: ImageVector, title: String, text: String, action: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Badge(icon, MaterialTheme.colorScheme.tertiary)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onClick, modifier = Modifier.align(Alignment.End)) {
                Text(action)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.padding(start = 4.dp).size(18.dp))
            }
        }
    }
}
