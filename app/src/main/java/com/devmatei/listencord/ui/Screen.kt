package com.devmatei.listencord.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devmatei.listencord.NowPlaying
import com.devmatei.listencord.Prefs

@Composable
fun Screen(prefs: Prefs, granted: Boolean, onGrant: () -> Unit) {
    var page by rememberSaveable { mutableStateOf("home") }
    BackHandler(page != "home") { page = "home" }
    val back = { page = "home" }

    AnimatedContent(
        page,
        transitionSpec = {
            val forward = targetState != "home"
            (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
        },
        label = "page",
    ) {
        when (it) {
            "apps" -> AppsPage(prefs, back)
            "presence" -> PresencePage(prefs, back)
            "art" -> ArtPage(prefs, back)
            else -> Home(prefs, granted, onGrant) { p -> page = p }
        }
    }
}

@Composable
private fun Home(prefs: Prefs, granted: Boolean, onGrant: () -> Unit, open: (String) -> Unit) {
    val ctx = LocalContext.current
    val status by NowPlaying.status.collectAsState()
    val tick = prefsTick(prefs)
    var enabled by remember { mutableStateOf(prefs.enabled) }

    val apps = remember(tick) { prefs.apps.map { appLabel(ctx, it) }.sorted() }
    val missingId = remember(tick) { prefs.appId.isBlank() }
    val sources = remember(tick) {
        listOfNotNull(
            "navidrome".takeIf { prefs.hasNavidrome },
            "listenbrainz".takeIf { prefs.lbToken.isNotBlank() },
            "itunes".takeIf { prefs.showArt },
        )
    }

    Page("listencord", status) {
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
                "set it",
            ) { open("presence") }
        }

        PresencePreview(prefs)

        Card(
            onClick = { enabled = !enabled; prefs.enabled = enabled },
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = if (enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (enabled) "showing on discord" else "paused",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = enabled, onCheckedChange = { enabled = it; prefs.enabled = it })
            }
        }

        Group("settings") {
            val chevron: @Composable () -> Unit = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) }
            Item(0, 3, "apps", apps.joinToString(", ").ifEmpty { "none picked yet" }, Icons.Rounded.Apps, chevron) { open("apps") }
            Item(1, 3, "presence", "what shows up and how", Icons.Rounded.Tune, chevron) { open("presence") }
            Item(
                2, 3, "album art",
                if (!prefs.showArt) "off" else sources.joinToString(", ").ifEmpty { "no sources yet" },
                Icons.Rounded.Album, chevron,
            ) { open("art") }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Setup(icon: ImageVector, title: String, text: String, action: String, onClick: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Badge(icon, MaterialTheme.colorScheme.tertiaryContainer)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onClick, shape = RoundedCornerShape(corner), modifier = Modifier.align(Alignment.End)) { Text(action) }
        }
    }
}
