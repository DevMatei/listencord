package com.devmatei.listencord.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devmatei.listencord.Prefs

@Composable
fun PresencePage(prefs: Prefs, onBack: () -> Unit) {
    var type by remember { mutableStateOf(prefs.type) }
    var display by remember { mutableStateOf(prefs.display) }
    var timestamps by remember { mutableStateOf(prefs.timestamps) }
    var paused by remember { mutableStateOf(prefs.showPaused) }
    var links by remember { mutableStateOf(prefs.links) }
    var podcastGeneric by remember { mutableStateOf(prefs.podcastGeneric) }

    Page("presence", onBack = onBack) {
        PresencePreview(prefs)

        Column {
            Header("discord app")
            Field(
                "application id", prefs.appId,
                hint = "make an app at discord.com/developers and paste its id",
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Number),
            ) { prefs.appId = it.trim() }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Header("text")
            Field("name", prefs.name) { prefs.name = it }
            Field("first line", prefs.details) { prefs.details = it }
            Field("second line", prefs.state) { prefs.state = it }
            Field("album hover text", prefs.largeText) { prefs.largeText = it }
            Text(
                "use {title} {artist} {album} {app} anywhere",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
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

        Group("podcasts") {
            SwitchItem(
                0, 1,
                "show podcasts generically",
                "if on, shows 'listening to a podcast' with no details or art",
                podcastGeneric
            ) { podcastGeneric = it; prefs.podcastGeneric = it }
        }

        Group("extras") {
            SwitchItem(0, 3, "progress bar", "shows time left on the song", timestamps) { timestamps = it; prefs.timestamps = it }
            SwitchItem(1, 3, "show while paused", null, paused) { paused = it; prefs.showPaused = it }
            SwitchItem(2, 3, "last.fm links", "makes the song and artist clickable", links) { links = it; prefs.links = it }
        }
    }
}
