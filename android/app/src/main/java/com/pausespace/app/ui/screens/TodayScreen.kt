package com.pausespace.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pausespace.app.data.Rules
import com.pausespace.app.data.TodayStats
import com.pausespace.app.ui.components.AppTile
import com.pausespace.app.ui.components.Bar
import com.pausespace.app.ui.components.EaseBack
import com.pausespace.app.ui.components.EaseInOut
import com.pausespace.app.ui.components.EaseOutQuint
import com.pausespace.app.ui.components.Icon
import com.pausespace.app.ui.components.Icons
import com.pausespace.app.ui.components.Mark
import com.pausespace.app.ui.components.enter
import com.pausespace.app.ui.components.rememberCountUp
import com.pausespace.app.ui.components.rememberShown
import com.pausespace.app.ui.components.tap
import com.pausespace.app.ui.theme.LocalTokens
import com.pausespace.app.ui.theme.ts
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    stats: TodayStats?,
    onToggleTheme: () -> Unit,
    onOpenApp: (String) -> Unit,
    onChooseApps: () -> Unit,
) {
    val t = LocalTokens.current
    val shown = rememberShown(stats != null)
    val ready = shown && stats != null
    val c = rememberCountUp(ready)
    val wave by animateFloatAsState(if (ready) 1f else 0f, tween(1200, 200, EaseInOut), label = "wave")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header
        Row(
            Modifier.enter(ready, dy = 18.dp).padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mark(30.dp, t.ink, barWidth = 9f, waveWidth = 5f, wave = wave)
                BasicText(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Black)) { append("Pause") }
                        withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append("Space") }
                    },
                    style = ts(20, 900, t.ink, spacing = -0.02f),
                )
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(t.surface).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { BasicText(stats?.dateLabel ?: "", style = ts(13, 700, t.muted)) }
                val rot by animateFloatAsState(if (t.dark) 0f else -90f, tween(600, easing = EaseBack), label = "rot")
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(t.surface)
                        .semantics { contentDescription = if (t.dark) "Switch to light theme" else "Switch to dark theme" }
                        .tap(onClick = onToggleTheme),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (t.dark) Icons.Sun else Icons.Moon, t.ink, 20.dp, Modifier.graphicsLayer { rotationZ = rot })
                }
            }
        }

        if (stats == null) return@Column

        Hero(stats, ready, c, wave)

        // Screen time + skips
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                Modifier
                    .weight(1f)
                    .enter(ready, delayMs = 160, dy = 18.dp)
                    .heightIn(min = 128.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(t.surface)
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                BasicText("Screen time", style = ts(14, 700, t.muted))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicText(
                        Rules.duration((stats.screenMs * c).toLong()),
                        style = ts(30, 900, t.ink, spacing = -0.03f, lineHeight = 1f, tabular = true),
                    )
                    stats.vsUsualMs?.let { d ->
                        Row(
                            Modifier.height(24.dp).clip(RoundedCornerShape(12.dp)).background(t.bg).padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(if (d <= 0) Icons.Down else Icons.Up, t.accent, 12.dp)
                            BasicText("${Rules.duration(abs(d))} vs usual", style = ts(12, 800, t.accent))
                        }
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .enter(ready, delayMs = 220, dy = 18.dp)
                    .heightIn(min = 128.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(t.surface2)
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                BasicText("Skips used", style = ts(14, 700, t.muted))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BasicText(
                        buildAnnotatedString {
                            append("${stats.skipsUsed}")
                            withStyle(SpanStyle(fontSize = ts(18, 900).fontSize, color = t.muted)) { append(" of ${stats.skipsPerDay}") }
                        },
                        style = ts(30, 900, t.ink, spacing = -0.03f, lineHeight = 1f),
                    )
                    if (stats.skipsPerDay > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            repeat(stats.skipsPerDay) { i ->
                                val filled = i < stats.skipsUsed
                                Bar(
                                    if (filled && ready) 1f else 0f, 8.dp, t.bg, t.primary,
                                    Modifier.weight(1f), delayMs = 650,
                                )
                            }
                        }
                    }
                }
            }
        }

        WeekCard(stats, ready)

        // By app
        Column(
            Modifier.enter(ready, delayMs = 340, dy = 18.dp).padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText("By app", Modifier.padding(start = 4.dp, bottom = 6.dp), style = ts(17, 800, t.ink))
            if (stats.byApp.isEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(t.surface)
                        .tap(onClick = onChooseApps)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText("No apps paused yet. Choose a few to start.", Modifier.weight(1f), style = ts(14, 700, t.muted))
                    Icon(Icons.Chevron, t.ink, 16.dp)
                }
            }
            val max = stats.byApp.maxOfOrNull { it.ms }?.coerceAtLeast(1) ?: 1
            stats.byApp.forEachIndexed { i, a ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 62.dp).tap { onOpenApp(a.pkg) }.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AppTile(a.pkg, a.label.take(1).uppercase(), 44.dp, 16.dp, 17)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row {
                            BasicText(a.label, Modifier.weight(1f), style = ts(15, 800, t.ink), maxLines = 1)
                            BasicText(Rules.duration(a.ms), style = ts(15, 800, t.ink, tabular = true))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Bar(
                                if (ready) maxOf(0.02f, a.ms / max.toFloat()) else 0f, 6.dp, t.surface, t.primary,
                                Modifier.weight(1f), delayMs = 700 + i * 90,
                            )
                            BasicText(
                                if (a.tries == 0) "no tries" else "${a.letGo} of ${a.tries} let go",
                                style = ts(12, 700, t.muted),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(stats: TodayStats, ready: Boolean, c: Float, wave: Float) {
    val t = LocalTokens.current
    Box(
        Modifier
            .enter(ready, delayMs = 60, dy = 18.dp, scaleFrom = 0.97f, durationMs = 800)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(t.heroBg)
            .clipToBounds(),
    ) {
        Mark(
            230.dp, t.heroFg.copy(alpha = 0.13f), barWidth = 7f, waveWidth = 3f, wave = wave,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 48.dp, y = (-36).dp),
        )
        Column(
            Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BasicText("You let go today", style = ts(15, 700, t.heroSub))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(
                    "${(stats.letGo * c).roundToInt()}",
                    Modifier.alignByBaseline(),
                    style = ts(96, 900, t.heroFg, spacing = -0.05f, lineHeight = 0.85f, tabular = true),
                )
                BasicText(
                    "/ ${stats.tries} ${if (stats.tries == 1) "try" else "tries"}",
                    Modifier.alignByBaseline(),
                    style = ts(28, 800, t.heroSub),
                )
            }
            val ticks = stats.ticks.takeLast(40)
            Row(
                Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .height(26.dp)
                    .semantics { contentDescription = "${stats.letGo} of ${stats.tries} tries closed without opening" },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (ticks.isEmpty()) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(t.heroDim))
                }
                ticks.forEachIndexed { i, letGo ->
                    val sy by animateFloatAsState(if (ready) 1f else 0.1f, tween(520, 380 + i * 40, EaseBack), label = "tick")
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleY = sy
                                alpha = if (ready) 1f else 0f
                                transformOrigin = TransformOrigin(0.5f, 1f)
                            }
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (letGo) t.heroFg else t.heroDim),
                    )
                }
            }
            BasicText(
                if (ticks.isEmpty()) "No tries yet. Each time you reach for a paused app, a bar appears here."
                else "Each bar is one time you reached for an app",
                style = ts(14, 600, t.heroSub),
            )
        }
    }
}

@Composable
private fun WeekCard(stats: TodayStats, ready: Boolean) {
    val t = LocalTokens.current
    Column(
        Modifier
            .enter(ready, delayMs = 280, dy = 18.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.5.dp, t.line, RoundedCornerShape(28.dp))
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText("This week", Modifier.weight(1f), style = ts(17, 800, t.ink))
            BasicText("share of tries let go", style = ts(13, 700, t.muted))
        }
        Row(Modifier.padding(top = 26.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            stats.week.forEachIndexed { i, d ->
                val share = d.share ?: 0f
                val h by animateFloatAsState(if (ready) share else 0f, tween(900, 480 + i * 70, EaseOutQuint), label = "week")
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(t.surface),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (h > 0f) {
                                if (d.today) {
                                    Box(Modifier.fillMaxWidth().fillMaxHeight(h).clip(RoundedCornerShape(14.dp)).background(t.primary))
                                } else {
                                    Stripes(Modifier.fillMaxWidth().fillMaxHeight(h).clip(RoundedCornerShape(14.dp)))
                                }
                            }
                        }
                        if (d.today) {
                            Box(
                                Modifier
                                    .enter(ready, delayMs = 1300, dy = 18.dp, durationMs = 500, fadeMs = 400, easing = EaseBack)
                                    .offset(y = -(112 * share + 8).dp)
                                    .wrapContentWidth(unbounded = true)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(t.ink)
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                BasicText(
                                    d.share?.let { "${(it * 100).roundToInt()}%" } ?: "–",
                                    style = ts(11, 800, t.bg),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                    BasicText(d.label, style = ts(12, if (d.today) 900 else 700, if (d.today) t.ink else t.muted))
                }
            }
        }
    }
}

/** The design's 135° stripes: edge 3px / disc 4px. */
@Composable
private fun Stripes(modifier: Modifier) {
    val t = LocalTokens.current
    Canvas(modifier.background(t.disc)) {
        val step = 7.dp.toPx() * 1.414f
        val w = 3.dp.toPx() * 1.414f
        var x = -size.height
        while (x < size.width + size.height) {
            drawLine(t.edge, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = w / 1.414f)
            x += step
        }
    }
}
