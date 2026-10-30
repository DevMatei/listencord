package com.devmatei.listencord.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devmatei.listencord.Prefs

// every page is the same shape: big collapsing title, scrolling column under it
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Page(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                subtitle = subtitle?.let { { Text(it) } },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "back") }
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(pad)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            content = content,
        )
    }
}

@Composable
fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
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
fun Badge(icon: ImageVector, color: Color = MaterialTheme.colorScheme.secondaryContainer) {
    Box(Modifier.size(40.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = contentColorFor(color))
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
    onChange: (Boolean) -> Unit,
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onChange,
        shapes = ListItemDefaults.segmentedShapes(index, count),
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
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    onSave: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it; onSave(it) },
        label = { Text(label) },
        supportingText = hint?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = keyboard,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    )
}

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
                colors = ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
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
        val l = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> tick++ }
        prefs.sp.registerOnSharedPreferenceChangeListener(l)
        onDispose { prefs.sp.unregisterOnSharedPreferenceChangeListener(l) }
    }
    return tick
}
