package com.pausespace.app.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pausespace.app.data.Apps
import com.pausespace.app.ui.theme.LocalTokens
import com.pausespace.app.ui.theme.tint
import com.pausespace.app.ui.theme.ts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// ── Motion ────────────────────────────────────────────────────────────────

val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
val EaseBack = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1f)
val EaseInOut = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
val EaseBreath = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

@Composable
fun reducedMotion(): Boolean {
    val cr = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/** False on first frame, true ~60 ms later — the design's `shown` flag. Instantly true with reduced motion. */
@Composable
fun rememberShown(key: Any? = Unit): Boolean {
    val reduced = reducedMotion()
    var shown by remember(key) { mutableStateOf(reduced) }
    LaunchedEffect(key) {
        if (!reduced) {
            delay(60)
            shown = true
        }
    }
    return shown
}

/** Fade + slide (+ optional scale) entrance, matching the design's staggered transitions. */
@Composable
fun Modifier.enter(
    shown: Boolean,
    delayMs: Int = 0,
    dy: Dp = 16.dp,
    dx: Dp = 0.dp,
    scaleFrom: Float = 1f,
    durationMs: Int = 700,
    fadeMs: Int = 500,
    easing: CubicBezierEasing = EaseOutQuint,
): Modifier {
    val p by animateFloatAsState(if (shown) 1f else 0f, tween(durationMs, delayMs, easing), label = "enter")
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(fadeMs, delayMs, LinearEasing), label = "fade")
    val density = LocalDensity.current
    return graphicsLayer {
        alpha = a
        translationY = with(density) { dy.toPx() } * (1f - p)
        translationX = with(density) { dx.toPx() } * (1f - p)
        val s = scaleFrom + (1f - scaleFrom) * p
        scaleX = s
        scaleY = s
    }
}

/** Animated count-up 0→1 with ease-out cubic, like the design's rAF counters. */
@Composable
fun rememberCountUp(shown: Boolean, durationMs: Int = 1100): Float {
    val reduced = reducedMotion()
    val c by animateFloatAsState(
        if (shown || reduced) 1f else 0f,
        if (reduced) tween(0) else tween(durationMs, 0, CubicBezierEasing(0.33f, 1f, 0.68f, 1f)),
        label = "count",
    )
    return c
}

// ── Mark & icons ──────────────────────────────────────────────────────────

private val BARS = listOf("M48 16v88", "M72 16v88")
private const val WAVE = "M6 70c22 0 32-10 54-10s32 10 54 10"

/**
 * The PauseSpace mark: two pause bars over a breath wave (120×120 viewBox).
 * [bars]/[wave] are draw progress 0..1 for the stroke-dash entrance.
 */
@Composable
fun Mark(
    size: Dp,
    color: Color,
    barWidth: Float,
    waveWidth: Float,
    modifier: Modifier = Modifier,
    bars: Float = 1f,
    wave: Float = 1f,
) {
    val barPaths = remember { BARS.map { PathParser().parsePathString(it).toPath() } }
    val wavePath = remember { PathParser().parsePathString(WAVE).toPath() }
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 120f
        scale(k, pivot = Offset.Zero) {
            barPaths.forEach { drawPath(partial(it, bars), color, style = Stroke(barWidth, cap = StrokeCap.Round)) }
            drawPath(partial(wavePath, wave), color, style = Stroke(waveWidth, cap = StrokeCap.Round))
        }
    }
}

private fun partial(path: Path, fraction: Float): Path {
    if (fraction >= 1f) return path
    val out = Path()
    if (fraction <= 0f) return out
    val pm = PathMeasure()
    pm.setPath(path, false)
    pm.getSegment(0f, pm.length * fraction, out, true)
    return out
}

/** Builds a stroked icon from SVG path data on a 24×24 viewBox. */
fun strokeIcon(name: String, width: Float, vararg d: String, viewport: Float = 24f): ImageVector =
    ImageVector.Builder(name, viewport.dp, viewport.dp, viewport, viewport).apply {
        d.forEach {
            addPath(
                pathData = addPathNodes(it),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

object Icons {
    val Check = strokeIcon("check", 2.8f, "M5 12.5l4.5 4.5L19 7.5")
    val Arrow = strokeIcon("arrow", 2.6f, "M5 12h14M13 6l6 6-6 6")
    val Back = strokeIcon("back", 2.4f, "M15 6l-6 6 6 6")
    val Chevron = strokeIcon("chevron", 2.6f, "M9 6l6 6-6 6")
    val Minus = strokeIcon("minus", 2.6f, "M6 12h12")
    val Plus = strokeIcon("plus", 2.6f, "M6 12h12M12 6v12")
    val Down = strokeIcon("down", 3f, "M12 5v14M6 13l6 6 6-6")
    val Up = strokeIcon("up", 3f, "M12 19V5M6 11l6-6 6 6")
    val Skip = strokeIcon("skip", 2.4f, "M5 6l7 6-7 6M13 6l7 6-7 6")
    val Search = strokeIcon("search", 2.3f, circle(11f, 11f, 6.5f), "M16 16l4 4")
    val Close = strokeIcon("close", 2.4f, "M6 6l12 12M18 6L6 18")
    val Sun = strokeIcon(
        "sun", 2.2f, circle(12f, 12f, 4f),
        "M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4",
    )
    val Moon = strokeIcon("moon", 2.2f, "M20 14.6A8.2 8.2 0 0 1 9.4 4a8.2 8.2 0 1 0 10.6 10.6z")
    val Home = strokeIcon(
        "home", 2.1f,
        "M3.5 11.2l7.2-6.1a2 2 0 0 1 2.6 0l7.2 6.1",
        "M5.5 10v8.5a2 2 0 0 0 2 2h9a2 2 0 0 0 2-2V10",
        "M10 20.5v-3.5a2 2 0 0 1 4 0v3.5",
    )
    val Grid = strokeIcon(
        "grid", 2.1f,
        circle(7.5f, 7.5f, 3.4f), circle(16.5f, 7.5f, 3.4f), circle(7.5f, 16.5f, 3.4f), circle(16.5f, 16.5f, 3.4f),
    )
}

@Composable
fun Icon(icon: ImageVector, color: Color, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier.size(size),
    )
}

// ── Controls ──────────────────────────────────────────────────────────────

fun Modifier.tap(label: String? = null, role: Role = Role.Button, onClick: () -> Unit): Modifier =
    clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        role = role,
        onClickLabel = label,
        onClick = onClick,
    )

/** The design's 50×30 pill switch inside a 56×44 hit area. */
@Composable
fun Toggle(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    val t = LocalTokens.current
    val track by animateColorAsState(if (checked) t.primary else t.off, tween(200), label = "track")
    val knob by animateDpAsState(if (checked) 20.dp else 0.dp, tween(240, easing = EaseOutQuint), label = "knob")
    Box(
        Modifier
            .size(56.dp, 44.dp)
            .semantics {
                contentDescription = label
                stateDescription = if (checked) "On" else "Off"
            }
            .tap(role = Role.Switch) { onChange(!checked) },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(Modifier.size(50.dp, 30.dp).clip(RoundedCornerShape(15.dp)).background(track)) {
            Box(
                Modifier
                    .offset(x = 3.dp + knob, y = 3.dp)
                    .size(24.dp)
                    .shadow(2.dp, CircleShape)
                    .background(Color.White, CircleShape),
            )
        }
    }
}

/** Rounded −/+ button used by every stepper. */
@Composable
fun RowScope.StepButton(
    icon: ImageVector,
    label: String,
    bg: Color,
    modifier: Modifier = Modifier.weight(1f),
    onClick: () -> Unit,
) {
    val t = LocalTokens.current
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .semantics { contentDescription = label }
            .tap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, t.ink, 16.dp) }
}

/** The app's real launcher icon; a tinted letter tile (as in the design) until it loads or if it can't. */
@Composable
fun AppTile(pkg: String, letter: String, size: Dp, radius: Dp, fontSize: Int) {
    val t = LocalTokens.current
    val context = LocalContext.current
    val icon by produceState(Apps.cachedIcon(pkg), pkg) {
        if (value == null) value = withContext(Dispatchers.IO) { Apps.icon(context, pkg) }
    }
    val img = icon
    if (img != null) {
        Image(img, contentDescription = null, modifier = Modifier.size(size))
        return
    }
    val (h, s) = Apps.tintOf(pkg)
    val (bg, fg) = tint(h, s, t.dark)
    Box(
        Modifier.size(size).clip(RoundedCornerShape(radius)).background(bg),
        contentAlignment = Alignment.Center,
    ) { BasicText(letter, style = ts(fontSize, 900, fg)) }
}

@Composable
fun BackHeader(title: String, backLabel: String, onBack: () -> Unit) {
    val t = LocalTokens.current
    Row(
        Modifier.height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(t.surface)
                .semantics { contentDescription = backLabel }
                .tap(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Back, t.ink, 20.dp) }
        Spacer(Modifier.weight(1f))
        BasicText(title, style = ts(15, 800, t.ink))
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(48.dp))
    }
}

/** Two-line label column used by setting rows. */
@Composable
fun RowScope.RowLabel(title: String, sub: String, subMaxLines: Int = Int.MAX_VALUE) {
    val t = LocalTokens.current
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        BasicText(title, style = ts(15, 800, t.ink))
        BasicText(sub, style = ts(13, 600, t.muted), maxLines = subMaxLines, softWrap = subMaxLines > 1)
    }
}

/** Round chevron chip at the end of a navigable row. */
@Composable
fun ChevronChip(size: Dp, bg: Color) {
    val t = LocalTokens.current
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(Icons.Chevron, t.ink, 16.dp)
    }
}

// ── Navigation pill ───────────────────────────────────────────────────────

enum class Tab(val label: String) { TODAY("Today"), APPS("Apps"), RITUAL("Ritual") }

@Composable
fun NavPill(current: Tab, shown: Boolean, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalTokens.current
    Row(
        modifier
            .enter(shown, delayMs = 400, dy = 90.dp, durationMs = 700, fadeMs = 400, easing = CubicBezierEasing(0.34f, 1.3f, 0.64f, 1f))
            .shadow(18.dp, RoundedCornerShape(40.dp), ambientColor = t.navShadow, spotColor = t.navShadow)
            .clip(RoundedCornerShape(40.dp))
            .background(t.navBg)
            .border(1.dp, t.line, RoundedCornerShape(40.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            val on = tab == current
            // One element per tab that morphs between the round icon button and the labelled pill,
            // so switching tabs slides the highlight instead of rebuilding the bar.
            val bg by animateColorAsState(if (on) t.primary else t.surface, tween(300), label = "navBg")
            val fg by animateColorAsState(if (on) t.onPrimary else t.ink, tween(300), label = "navFg")
            val iconSize = if (tab == Tab.RITUAL) 24.dp else 22.dp
            val inset = (56.dp - iconSize) / 2
            val start by animateDpAsState(if (on) 18.dp else inset, tween(350, easing = EaseOutQuint), label = "navStart")
            val end by animateDpAsState(if (on) 24.dp else inset, tween(350, easing = EaseOutQuint), label = "navEnd")
            Row(
                Modifier
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(bg)
                    .semantics {
                        contentDescription = tab.label
                        if (on) stateDescription = "Current page"
                    }
                    .tap { if (!on) onSelect(tab) }
                    .padding(start = start, end = end),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (tab) {
                    Tab.TODAY -> Icon(Icons.Home, fg, iconSize)
                    Tab.APPS -> Icon(Icons.Grid, fg, iconSize)
                    Tab.RITUAL -> Mark(iconSize, fg, barWidth = 11f, waveWidth = 7f)
                }
                AnimatedVisibility(
                    visible = on,
                    enter = fadeIn(tween(250, 80)) + expandHorizontally(tween(350, easing = EaseOutQuint)),
                    exit = fadeOut(tween(120)) + shrinkHorizontally(tween(350, easing = EaseOutQuint)),
                ) {
                    BasicText(
                        tab.label,
                        Modifier.padding(start = 8.dp),
                        style = ts(15, 800, fg),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/** Thin progress bar with a growing fill, e.g. the per-app rows on Today. */
@Composable
fun Bar(fraction: Float, height: Dp, track: Color, fill: Color, modifier: Modifier = Modifier, delayMs: Int = 0) {
    val f by animateFloatAsState(fraction, tween(900, delayMs, EaseOutQuint), label = "bar")
    Box(modifier.height(height).clip(RoundedCornerShape(height / 2)).background(track)) {
        if (f > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(f)
                    .clip(RoundedCornerShape(height / 2))
                    .background(fill),
            )
        }
    }
}
