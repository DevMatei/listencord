package com.devmatei.listencord.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devmatei.listencord.Podcasts
import com.devmatei.listencord.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresencePage(prefs: Prefs) {
    var type by remember { mutableIntStateOf(prefs.type) }
    var display by remember { mutableIntStateOf(prefs.display) }
    var timestamps by remember { mutableStateOf(prefs.timestamps) }
    var paused by remember { mutableStateOf(prefs.showPaused) }
    var links by remember { mutableStateOf(prefs.links) }
    var podcastMode by remember { mutableIntStateOf(prefs.podcastMode) }
    var podcastSheet by remember { mutableStateOf(false) }
    var idReset by remember { mutableIntStateOf(0) }
    val tick = prefsTick(prefs)
    val podcastCount = remember(tick) { prefs.podcastApps.size }

    Page("presence", "what your profile shows") {
        PresencePreview(prefs)

        Column {
            Header("discord app")
            key(idReset) {
                Field(
                    "application id", prefs.appId,
                    hint = "the built in one works, make your own at discord.com/developers for a custom name or icon",
                    keyboard = KeyboardOptions(keyboardType = KeyboardType.Number),
                ) { prefs.appId = it.trim() }
            }
            if (prefs.appId != Prefs.DEFAULT_APP_ID) {
                TextButton(onClick = { prefs.appId = Prefs.DEFAULT_APP_ID; idReset++ }) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, null, Modifier.padding(end = 6.dp))
                    Text("use the built in one")
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header("text", Modifier.padding(bottom = 0.dp))
            Field("name", prefs.name, placeholder = "music") { prefs.name = it }
            Field("first line", prefs.details) { prefs.details = it }
            Field("second line", prefs.state) { prefs.state = it }
            Field("art hover text", prefs.largeText) { prefs.largeText = it }
            FlowRow(
                Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("{title}", "{artist}", "{album}", "{show}", "{app}").forEach {
                    Pill(it, MaterialTheme.colorScheme.surfaceContainerHighest)
                }
            }
            Hint("drop these in anywhere, {show} is the podcast name")
        }

        Column {
            Header("activity")
            Choice(listOf(2 to "listening", 0 to "playing", 3 to "watching", 5 to "competing"), type) {
                type = it
                prefs.type = it
            }
        }

        Column {
            Header("member list shows")
            Choice(listOf(0 to "name", 2 to "first line", 1 to "second line"), display) {
                display = it
                prefs.display = it
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header("podcasts", Modifier.padding(bottom = 0.dp))
            Choice(
                listOf(Prefs.PODCAST_FULL to "everything", Prefs.PODCAST_SHOW to "show only", Prefs.PODCAST_HIDDEN to "just \"a podcast\""),
                podcastMode,
            ) {
                podcastMode = it
                prefs.podcastMode = it
            }
            AnimatedContent(podcastMode, label = "podcast hint") {
                Hint(
                    when (it) {
                        Prefs.PODCAST_SHOW -> "shows the podcast's name and art, but not which episode"
                        Prefs.PODCAST_HIDDEN -> "no name, no episode, just a mic icon"
                        else -> "same text as music, with the podcast's art from itunes"
                    },
                )
            }
            Item(
                0, 1, "podcast apps",
                "$podcastCount ${if (podcastCount == 1) "app" else "apps"}, plus spotify episodes and anything that looks like one",
                Icons.Rounded.Podcasts,
                { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
            ) { podcastSheet = true }
        }

        Group("extras") {
            SwitchItem(0, 3, "progress bar", "shows time left on the song", timestamps, Icons.Rounded.Timer) {
                timestamps = it; prefs.timestamps = it
            }
            SwitchItem(1, 3, "show while paused", "keeps the status up when you pause", paused, Icons.Rounded.Pause) {
                paused = it; prefs.showPaused = it
            }
            SwitchItem(2, 3, "last.fm links", "makes the song and artist clickable", links, Icons.Rounded.Link) {
                links = it; prefs.links = it
            }
        }
    }

    if (podcastSheet) PodcastAppsSheet(prefs) { podcastSheet = false }
}

// which apps always count as podcasts. known podcast players plus whatever you watch
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PodcastAppsSheet(prefs: Prefs, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var chosen by remember { mutableStateOf(prefs.podcastApps) }
    var apps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val installed = (Podcasts.KNOWN + prefs.apps + chosen).filter {
                try {
                    ctx.packageManager.getApplicationInfo(it, 0)
                    true
                } catch (_: Exception) {
                    false
                }
            }
            installed.map { it to appLabel(ctx, it) }.sortedBy { it.second.lowercase() }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded))) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text("podcast apps", style = MaterialTheme.typography.headlineSmall)
            Text(
                "everything these play counts as a podcast. mixed apps like spotify get checked per episode, so leave them off.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (apps.isEmpty()) {
                item { Hint("none of your apps are podcast players yet", Modifier.padding(vertical = 16.dp)) }
            }
            itemsIndexed(apps, key = { _, it -> it.first }) { i, (pkg, label) ->
                SwitchItem(
                    i, apps.size, label,
                    sub = if (pkg in Podcasts.KNOWN) "known podcast player" else null,
                    checked = pkg in chosen,
                    leading = { AppIcon(pkg) },
                ) {
                    chosen = if (it) chosen + pkg else chosen - pkg
                    prefs.podcastApps = chosen
                }
            }
        }
    }
}
