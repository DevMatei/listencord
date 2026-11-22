package com.devmatei.listencord.ui

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import com.devmatei.listencord.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// the rounded shape everything on a page should match: settings rows, fields, buttons
val corner = 28.dp
val fieldShape = RoundedCornerShape(20.dp)

// every tab is the same shape: big collapsing title, scrolling column under it
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Page(
    title: String,
    subtitle: String? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                subtitle = subtitle?.let { { Text(it) } },
                actions = { actions() },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(pad)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            content = content,
        )
    }
}

@Composable
fun Header(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp),
    )
}

// a titled stack of rows that share one rounded shape, like the settings app
@Composable
fun Group(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column {
        title?.let { Header(it) }
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap), content = content)
    }
}

@Composable
fun Badge(
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    size: Dp = 40.dp,
) {
    Box(Modifier.size(size).background(color, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.55f), tint = contentColorFor(color))
    }
}

// small rounded label, for things like "podcast" or "player"
@Composable
fun Pill(
    text: String,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    icon: ImageVector? = null,
) {
    Surface(color = color, shape = CircleShape) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            icon?.let { Icon(it, null, Modifier.size(14.dp)) }
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Item(
    index: Int,
    count: Int,
    title: String,
    sub: String? = null,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        leadingContent = icon?.let { { Badge(it) } },
        supportingContent = sub?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
        trailingContent = trailing,
        colors = rowColors(),
        content = { Text(title) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SwitchItem(
    index: Int,
    count: Int,
    title: String,
    sub: String? = null,
    checked: Boolean,
    icon: ImageVector? = null,
    leading: @Composable (() -> Unit)? = icon?.let { { Badge(it) } },
    onChange: (Boolean) -> Unit,
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onChange,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        leadingContent = leading,
        supportingContent = sub?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = rowColors(),
        content = { Text(title) },
    )
}

@Composable
fun Field(
    label: String,
    initial: String,
    hint: String? = null,
    placeholder: String? = null,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    onSave: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it; onSave(it) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = hint?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = keyboard,
        shape = fieldShape,
        modifier = Modifier.fillMaxWidth(),
    )
}

// connected button group, one pick out of a few
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Choice(options: List<Pair<Int, String>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { i, (value, label) ->
            ToggleButton(
                checked = selected == value,
                onCheckedChange = { onSelect(value) },
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rowColors() = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

// bumps whenever a pref changes so composables reading prefs directly redraw
@Composable
fun prefsTick(prefs: Prefs): Int {
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(prefs) {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> tick++ }
        prefs.sp.registerOnSharedPreferenceChangeListener(l)
        onDispose { prefs.sp.unregisterOnSharedPreferenceChangeListener(l) }
    }
    return tick
}

// app icons get decoded once, scrolling the list back and forth shouldn't redo it
private val icons = LruCache<String, ImageBitmap>(64)

fun appLabel(ctx: Context, pkg: String) = try {
    ctx.packageManager.run { getApplicationLabel(getApplicationInfo(pkg, 0)).toString() }
} catch (_: Exception) {
    pkg
}

@Composable
fun AppIcon(pkg: String, size: Dp = 40.dp) {
    val ctx = LocalContext.current
    val icon by produceState(icons.get(pkg), pkg) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            try {
                ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap().also { icons.put(pkg, it) }
            } catch (_: Exception) {
                null
            }
        }
    }
    val modifier = Modifier.size(size).clip(CircleShape)
    icon?.let { Image(it, null, modifier) } ?: Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest))
}

// the most colorful bit of the cover, for tinting the now playing card
suspend fun accentOf(bmp: Bitmap): Color? = withContext(Dispatchers.Default) {
    val small = try {
        bmp.scale(24, 24)
    } catch (_: Exception) {
        return@withContext null
    }
    val hsv = FloatArray(3)
    var r = 0f
    var g = 0f
    var b = 0f
    var weight = 0f
    for (x in 0 until small.width) for (y in 0 until small.height) {
        val p = small[x, y]
        android.graphics.Color.colorToHSV(p, hsv)
        // favour saturated, not too dark pixels
        val w = hsv[1] * hsv[1] * hsv[2]
        if (w < 0.05f) continue
        r += android.graphics.Color.red(p) * w
        g += android.graphics.Color.green(p) * w
        b += android.graphics.Color.blue(p) * w
        weight += w
    }
    if (small !== bmp) small.recycle()
    if (weight == 0f) null else Color(r / weight / 255f, g / weight / 255f, b / weight / 255f)
}
