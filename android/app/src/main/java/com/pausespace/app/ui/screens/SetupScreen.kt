package com.pausespace.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pausespace.app.data.Permission
import com.pausespace.app.ui.components.EaseInOut
import com.pausespace.app.ui.components.Icon
import com.pausespace.app.ui.components.Icons
import com.pausespace.app.ui.components.Mark
import com.pausespace.app.ui.components.enter
import com.pausespace.app.ui.components.rememberShown
import com.pausespace.app.ui.components.tap
import com.pausespace.app.ui.theme.LocalTokens
import com.pausespace.app.ui.theme.ts

@Composable
fun SetupScreen(
    granted: Map<Permission, Boolean>,
    onAllow: (Permission) -> Unit,
    onContinue: () -> Unit,
) {
    val t = LocalTokens.current
    val shown = rememberShown()
    val bars by animateFloatAsState(if (shown) 1f else 0f, tween(700, 200, EaseInOut), label = "bars")
    val wave by animateFloatAsState(if (shown) 1f else 0f, tween(1200, 650, EaseInOut), label = "wave")

    Column(
        Modifier
            .fillMaxSize()
            .background(t.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(34.dp),
        ) {
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(
                    Modifier
                        .enter(shown, dy = 0.dp, scaleFrom = 0.6f, durationMs = 900, fadeMs = 400, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f))
                        .size(88.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(t.surface),
                    contentAlignment = Alignment.Center,
                ) { Mark(62.dp, t.ink, barWidth = 6f, waveWidth = 3f, bars = bars, wave = wave) }
                Column(
                    Modifier.enter(shown, delayMs = 300, durationMs = 800, fadeMs = 600),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BasicText(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Black)) { append("Pause") }
                            withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = t.accent)) { append("Space") }
                        },
                        style = ts(44, 900, t.ink, spacing = -0.035f, lineHeight = 1f),
                    )
                    BasicText(
                        "A breath of room between you and the feed.",
                        Modifier.widthIn(max = 300.dp),
                        style = ts(19, 600, t.muted, lineHeight = 1.4f),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BasicText(
                    "Three permissions, then you're set",
                    Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
                    style = ts(15, 800, t.ink),
                )
                Permission.entries.forEachIndexed { i, p ->
                    PermissionRow(i + 1, p, granted[p] == true, shown, 500 + i * 110) { onAllow(p) }
                }
                BasicText(
                    "Your usage stays on this phone. PauseSpace only notices when an app opens, never what you do inside it.",
                    Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp),
                    style = ts(13, 600, t.muted, lineHeight = 1.45f),
                )
            }
        }
        Row(
            Modifier
                .padding(top = 16.dp)
                .enter(shown, delayMs = 900, dy = 40.dp, durationMs = 800, easing = CubicBezierEasing(0.34f, 1.2f, 0.64f, 1f))
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(t.primary)
                .tap(onClick = onContinue)
                .padding(start = 26.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText("Choose apps to pause", style = ts(17, 800, t.onPrimary))
            Box(Modifier.size(48.dp).clip(CircleShape).background(t.onPrimary), contentAlignment = Alignment.Center) {
                Icon(Icons.Arrow, t.primary, 20.dp)
            }
        }
    }
}

@Composable
private fun PermissionRow(n: Int, p: Permission, ok: Boolean, shown: Boolean, delayMs: Int, onAllow: () -> Unit) {
    val t = LocalTokens.current
    val bg by animateColorAsState(if (ok) t.surface else t.bg, tween(300), label = "bg")
    val border by animateColorAsState(if (ok) t.surface else t.line, tween(300), label = "border")
    Row(
        Modifier
            .enter(shown, delayMs = delayMs)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(24.dp))
            .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(if (ok) t.bg else t.surface),
            contentAlignment = Alignment.Center,
        ) { BasicText("$n", style = ts(14, 900, t.accent)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(p.title, style = ts(15, 800, t.ink))
            BasicText(p.why, style = ts(13, 600, t.muted, lineHeight = 1.35f))
        }
        if (ok) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(t.primary)
                    .semantics { contentDescription = "${p.title} allowed" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Check, t.onPrimary, 18.dp) }
        } else {
            Box(
                Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(t.ink)
                    .semantics { contentDescription = "Allow ${p.title}" }
                    .tap(onClick = onAllow)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { BasicText("Allow", style = ts(14, 800, t.bg)) }
        }
    }
}
