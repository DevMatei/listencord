package com.devmatei.listencord.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.devmatei.listencord.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class App(val pkg: String, val label: String)

fun loadApps(ctx: Context): List<App> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { it.activityInfo.packageName }
        .distinct()
        .filter { it != ctx.packageName }
        .map { App(it, appLabel(ctx, it)) }
}

fun appLabel(ctx: Context, pkg: String) = try {
    ctx.packageManager.run { getApplicationLabel(getApplicationInfo(pkg, 0)).toString() }
} catch (_: Exception) {
    pkg
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppsPage(prefs: Prefs, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<App>?>(null) }
    var picked by remember { mutableStateOf(prefs.apps) }
    var query by rememberSaveable { mutableStateOf("") }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        val first = prefs.apps
        apps = withContext(Dispatchers.IO) {
            loadApps(ctx).sortedWith(compareBy({ it.pkg !in first }, { it.label.lowercase() }))
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("apps") },
                subtitle = { Text(if (picked.isEmpty()) "pick the players you use" else "${picked.size} picked") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "back") } },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@Scaffold
        }
        val shown = list.filter { query.isBlank() || it.label.contains(query, true) }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.fillMaxSize().imePadding(),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("search") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
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
                    trailingContent = { Checkbox(checked = on, onCheckedChange = null) },
                    colors = rowColors(),
                    modifier = Modifier.animateItem(),
                    content = { Text(app.label) },
                )
            }
        }
    }
}

@Composable
private fun AppIcon(pkg: String) {
    val ctx = LocalContext.current
    val icon by produceState<ImageBitmap?>(null, pkg) {
        value = withContext(Dispatchers.IO) {
            try {
                ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }
    val modifier = Modifier.size(40.dp).clip(CircleShape)
    icon?.let { Image(it, null, modifier) } ?: Box(modifier)
}
