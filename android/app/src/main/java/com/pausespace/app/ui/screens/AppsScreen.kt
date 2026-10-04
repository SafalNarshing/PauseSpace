package com.pausespace.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pausespace.app.data.AppInfo
import com.pausespace.app.data.Rules
import com.pausespace.app.ui.components.AppTile
import com.pausespace.app.ui.components.Icon
import com.pausespace.app.ui.components.Icons
import com.pausespace.app.ui.components.Toggle
import com.pausespace.app.ui.components.enter
import com.pausespace.app.ui.components.rememberShown
import com.pausespace.app.ui.components.tap
import com.pausespace.app.ui.theme.LocalTokens
import com.pausespace.app.ui.theme.ts

private enum class Filter(val label: String) { ALL("All"), ON("Paused"), OFF("Not paused") }

@Composable
fun AppsScreen(
    apps: List<AppInfo>?,
    weekMs: Map<String, Long>,
    paused: Set<String>,
    contentPadding: PaddingValues,
    onToggle: (String, Boolean) -> Unit,
    onOpen: (String) -> Unit,
) {
    val t = LocalTokens.current
    val shown = rememberShown(apps != null)
    val ready = shown && apps != null
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    // Paused apps first, then by how much they were used this week. Order is fixed on entry
    // so rows don't jump around while toggling.
    val ordered = remember(apps, weekMs) {
        apps.orEmpty().sortedWith(
            compareByDescending<AppInfo> { it.pkg in paused }
                .thenByDescending { weekMs[it.pkg] ?: 0L }
                .thenBy { it.label.lowercase() },
        )
    }
    val visible = ordered.filter {
        when (filter) {
            Filter.ALL -> true
            Filter.ON -> it.pkg in paused
            Filter.OFF -> it.pkg !in paused
        } && (query.isBlank() || it.label.contains(query.trim(), ignoreCase = true))
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Row(
                Modifier.enter(ready, durationMs = 650, fadeMs = 450).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicText("Apps", style = ts(36, 900, t.ink, spacing = -0.035f, lineHeight = 1f))
                    BasicText(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Black, color = t.accent)) { append("${paused.size} paused") }
                            append(" before they open")
                        },
                        style = ts(15, 600, t.muted),
                    )
                }
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(t.surface)
                        .semantics { contentDescription = if (searching) "Close search" else "Search apps" }
                        .tap {
                            searching = !searching
                            if (!searching) query = ""
                        },
                    contentAlignment = Alignment.Center,
                ) { Icon(if (searching) Icons.Close else Icons.Search, t.ink, 20.dp) }
            }
        }
        if (searching) {
            item {
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { focus.requestFocus() }
                Box(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(t.surface)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) BasicText("Search apps", style = ts(15, 600, t.muted))
                    BasicTextField(
                        query,
                        { query = it },
                        Modifier.fillMaxWidth().focusRequester(focus),
                        textStyle = ts(15, 700, t.ink),
                        singleLine = true,
                        cursorBrush = SolidColor(t.accent),
                    )
                }
            }
        }
        item {
            Row(
                Modifier.enter(ready, delayMs = 80, durationMs = 650, fadeMs = 450).padding(top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Filter.entries.forEach { f ->
                    val on = f == filter
                    Box(
                        Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (on) t.ink else t.bg)
                            .border(1.5.dp, if (on) t.ink else t.line, RoundedCornerShape(20.dp))
                            .tap { filter = f }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { BasicText(f.label, style = ts(14, 800, if (on) t.bg else t.ink)) }
                }
            }
        }
        if (apps != null && visible.isEmpty()) {
            item {
                BasicText(
                    if (query.isNotBlank()) "No apps match “$query”." else "Nothing here yet.",
                    Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                    style = ts(15, 700, t.muted),
                )
            }
        }
        itemsIndexed(visible, key = { _, a -> a.pkg }) { i, a ->
            val on = a.pkg in paused
            val card by animateColorAsState(if (on) t.surface else t.bg, tween(200), label = "card")
            Row(
                Modifier
                    .enter(ready, delayMs = 140 + minOf(i, 10) * 55, dy = 0.dp, dx = 28.dp, durationMs = 650, fadeMs = 450)
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(card)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier.weight(1f).heightIn(min = 56.dp).tap { onOpen(a.pkg) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AppTile(a.pkg, a.letter, 46.dp, 16.dp, 18)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        BasicText(a.label, style = ts(16, 800, t.ink), maxLines = 1)
                        BasicText(
                            "${Rules.duration(weekMs[a.pkg] ?: 0L)} this week",
                            style = ts(13, 700, t.muted),
                        )
                    }
                }
                Toggle(on, "Pause ${a.label}") { onToggle(a.pkg, it) }
            }
        }
    }
}
