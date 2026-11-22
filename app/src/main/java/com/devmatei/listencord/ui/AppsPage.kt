package com.devmatei.listencord.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devmatei.listencord.Podcasts
import com.devmatei.listencord.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class App(val pkg: String, val label: String, val player: Boolean)

fun loadApps(ctx: Context): List<App> {
    val pm = ctx.packageManager
    // real players expose a media browser (android auto, the system media picker use it).
    // media button receivers would be tempting but automation apps and file managers have those too
    val players = buildSet {
        pm.queryIntentServices(Intent("android.media.browse.MediaBrowserService"), 0)
            .forEach { add(it.serviceInfo.packageName) }
        addAll(Podcasts.KNOWN)
    }
    return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        .map { it.activityInfo.packageName }
        .distinct()
        .filter { it != ctx.packageName }
        .map { App(it, appLabel(ctx, it), it in players) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppsPage(prefs: Prefs) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<App>?>(null) }
    var picked by remember { mutableStateOf(prefs.apps) }
    val podcastApps = remember { prefs.podcastApps }
    var query by rememberSaveable { mutableStateOf("") }
    var onlyPlayers by rememberSaveable { mutableStateOf(true) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        val first = prefs.apps
        apps = withContext(Dispatchers.IO) {
            loadApps(ctx).sortedWith(compareBy({ it.pkg !in first }, { !it.player }, { it.label.lowercase() }))
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("apps") },
                subtitle = { Text(if (picked.isEmpty()) "pick the players you use" else "watching ${picked.size}") },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { pad ->
        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@Scaffold
        }
        // if nothing on the phone looks like a player, don't hide everything
        val anyPlayers = list.any { it.player }
        val shown = list.filter {
            (query.isBlank() || it.label.contains(query, true)) &&
                (!onlyPlayers || !anyPlayers || it.player || it.pkg in picked || query.isNotBlank())
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.fillMaxSize().imePadding(),
        ) {
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("search apps") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "clear") }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (anyPlayers) {
                item(key = "filters") {
                    Row(Modifier.padding(top = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(true to "players", false to "everything").forEach { (value, label) ->
                            FilterChip(
                                selected = onlyPlayers == value,
                                onClick = { onlyPlayers = value },
                                label = { Text(label) },
                                leadingIcon = if (onlyPlayers == value) {
                                    { Icon(Icons.Rounded.Check, null, Modifier.padding(0.dp)) }
                                } else null,
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            )
                        }
                    }
                }
            }
            itemsIndexed(shown, key = { _, it -> it.pkg }) { i, app ->
                val on = app.pkg in picked
                SegmentedListItem(
                    checked = on,
                    onCheckedChange = {
                        picked = if (on) picked - app.pkg else picked + app.pkg
                        prefs.apps = picked
                    },
                    shapes = ListItemDefaults.segmentedShapes(i, shown.size),
                    leadingContent = { AppIcon(app.pkg) },
                    supportingContent = when {
                        app.pkg in podcastApps -> ({ Text("podcast player") })
                        app.player -> ({ Text("media player") })
                        else -> null
                    },
                    trailingContent = { Checkbox(checked = on, onCheckedChange = null) },
                    colors = rowColors(),
                    modifier = Modifier.animateItem(),
                    content = { Text(app.label) },
                )
            }
            if (shown.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "nothing matches",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
        }
    }
}
