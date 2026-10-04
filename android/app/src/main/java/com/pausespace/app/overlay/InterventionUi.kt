package com.pausespace.app.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pausespace.app.data.Outcome
import com.pausespace.app.data.Ritual
import com.pausespace.app.data.Rules
import com.pausespace.app.data.Session
import com.pausespace.app.data.SessionLength
import com.pausespace.app.data.Stats
import com.pausespace.app.ui.components.AppTile
import com.pausespace.app.ui.components.EaseBreath
import com.pausespace.app.ui.components.EaseInOut
import com.pausespace.app.ui.components.Icon
import com.pausespace.app.ui.components.Icons
import com.pausespace.app.ui.components.Mark
import com.pausespace.app.ui.components.enter
import com.pausespace.app.ui.components.reducedMotion
import com.pausespace.app.ui.components.tap
import com.pausespace.app.ui.theme.LocalTokens
import com.pausespace.app.ui.theme.ts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Everything the overlay needs, captured when it opens. */
data class InterventionModel(
    val pkg: String,
    val label: String,
    val letter: String,
    val ritual: Ritual,
    val rounds: Int,
    val line: String,
    val skipsTotal: Int,
    val skipsLeft: Int,
    val length: SessionLength,
    val opensLeft: Int,
    val dailyOpens: Int,
    val reflect: com.pausespace.app.data.ReflectInfo,
    /** Filled in from usage stats after the overlay is already up. */
    val sessions: State<List<Session>>,
    val now: Long,
)

private enum class Stage { BREATHE, REFLECT, DONE }

private data class Phase(val label: String, val seconds: Int, val big: Boolean)

private fun phases(r: Ritual) = buildList {
    add(Phase("Breathe in", r.inhale.coerceAtLeast(1), true))
    if (r.hold > 0) add(Phase("Hold", r.hold, true))
    add(Phase("Breathe out", r.exhale.coerceAtLeast(1), false))
}

@Composable
fun InterventionScreen(
    m: InterventionModel,
    onSkip: () -> Unit,
    onChoose: (Outcome, Int?) -> Unit,
    onFinish: (Outcome) -> Unit,
) {
    val t = LocalTokens.current
    var stage by remember { mutableStateOf(Stage.BREATHE) }
    var viaSkip by remember { mutableStateOf(false) }
    var skipsLeft by remember { mutableIntStateOf(m.skipsLeft) }
    var outcome by remember { mutableStateOf(Outcome.LET_GO) }
    var minutes by remember { mutableStateOf<Int?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(t.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when (stage) {
            Stage.BREATHE -> Breathe(
                m,
                skipsLeft,
                onSkip = {
                    if (skipsLeft > 0) {
                        skipsLeft--
                        viaSkip = true
                        onSkip()
                        stage = Stage.REFLECT
                    }
                },
                onDone = { stage = Stage.REFLECT },
            )
            Stage.REFLECT -> Reflect(m, viaSkip, skipsLeft) { o, mins ->
                outcome = o
                minutes = mins
                onChoose(o, mins)
                stage = Stage.DONE
            }
            Stage.DONE -> Done(m, outcome, minutes) { onFinish(outcome) }
        }
    }
}

// ── Breathe ───────────────────────────────────────────────────────────────

@Composable
private fun Breathe(m: InterventionModel, skipsLeft: Int, onSkip: () -> Unit, onDone: () -> Unit) {
    val t = LocalTokens.current
    val reduced = reducedMotion()
    val q = remember(m.ritual) { phases(m.ritual) }
    var seen by remember { mutableStateOf(reduced) }
    var idx by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(1) }
    var left by remember { mutableIntStateOf(q[0].seconds) }
    var armed by remember { mutableStateOf(false) }
    val disc = remember { Animatable(0.12f) }

    LaunchedEffect(Unit) {
        delay(50)
        seen = true
        delay(if (reduced) 120 else 700)
        armed = true
        while (true) {
            val ph = q[idx]
            launch {
                val target = if (ph.big) 1f else 0.12f
                if (reduced) disc.snapTo(target)
                else disc.animateTo(target, tween(ph.seconds * 1000, easing = EaseBreath))
            }
            for (s in ph.seconds downTo 1) {
                left = s
                delay(1000)
            }
            idx++
            if (idx >= q.size) {
                idx = 0
                round++
                if (round > m.rounds) {
                    onDone()
                    return@LaunchedEffect
                }
            }
            left = q[idx].seconds
        }
    }

    val ph = q[idx.coerceIn(q.indices)]
    val back = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1f)

    Column(
        Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 34.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        AppPill(m, "Opening ${m.label}", Modifier.enter(seen, dy = (-22).dp, scaleFrom = 0.85f, fadeMs = 400, easing = back))

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(30.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // 264dp circle on a 390dp-wide design; shrink on narrow screens so the outer ring fits.
                val d = minOf(264.dp, maxWidth - 68.dp)
                Box(
                    Modifier
                        .size(d + 68.dp)
                        .semantics { contentDescription = "${ph.label}, $left seconds" },
                    contentAlignment = Alignment.Center,
                ) {
                    Ring(d + 68.dp, 1.dp, t.line, Modifier.enter(seen, delayMs = 260, dy = 0.dp, scaleFrom = 0.7f, durationMs = 1100, fadeMs = 700))
                    Ring(d + 34.dp, 1.dp, t.line, Modifier.enter(seen, delayMs = 160, dy = 0.dp, scaleFrom = 0.75f, durationMs = 1000, fadeMs = 600))
                    Ring(d, 2.dp, t.edge, Modifier.enter(seen, delayMs = 60, dy = 0.dp, scaleFrom = 0.8f, durationMs = 900, easing = CubicBezierEasing(0.34f, 1.3f, 0.64f, 1f)))
                    Box(
                        Modifier
                            .size(d)
                            .graphicsLayer {
                                val s = if (armed) disc.value else 0.12f
                                scaleX = s
                                scaleY = s
                                alpha = if (armed) ((disc.value - 0.12f) / 0.88f).coerceIn(0f, 1f) else 0f
                            }
                            .clip(CircleShape)
                            .background(t.disc),
                    )
                    BasicText(
                        "$left",
                        Modifier.enter(seen, delayMs = 380, dy = 0.dp, scaleFrom = 0.85f, fadeMs = 500, easing = back),
                        style = ts(76, 900, t.accent, spacing = -0.04f, lineHeight = 1f, tabular = true),
                    )
                }
            }
            Column(
                Modifier.enter(seen, delayMs = 520, dy = 18.dp, durationMs = 800, fadeMs = 600),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicText(ph.label, style = ts(36, 900, t.ink, spacing = -0.03f, lineHeight = 1f))
                BasicText(
                    m.line,
                    Modifier.widthIn(max = 300.dp),
                    style = ts(17, 600, t.muted, lineHeight = 1.35f).copy(textAlign = TextAlign.Center),
                )
                if (m.rounds > 1) RoundDots(m.rounds, round.coerceAtMost(m.rounds))
            }
        }

        Box(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).enter(seen, delayMs = 1100, dy = 0.dp, fadeMs = 600),
            contentAlignment = Alignment.Center,
        ) {
            if (skipsLeft > 0) {
                Row(
                    Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, t.line, RoundedCornerShape(24.dp))
                        .semantics { contentDescription = "Skip breathing, $skipsLeft of ${m.skipsTotal} skips left today" }
                        .tap(onClick = onSkip)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Skip, t.muted, 15.dp)
                    BasicText("Skip · $skipsLeft left", style = ts(14, 800, t.muted))
                }
            }
        }
    }
}

@Composable
private fun Ring(size: androidx.compose.ui.unit.Dp, width: androidx.compose.ui.unit.Dp, color: Color, modifier: Modifier) {
    Box(modifier.requiredSize(size).border(width, color, CircleShape))
}

@Composable
private fun RoundDots(total: Int, current: Int) {
    val t = LocalTokens.current
    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val cur = i == current - 1
            val w by animateFloatAsState(if (cur) 28f else 8f, tween(400), label = "dot")
            Box(
                Modifier
                    .width(w.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (i < current) t.primary else t.off),
            )
        }
    }
}

@Composable
private fun AppPill(m: InterventionModel, text: String, modifier: Modifier = Modifier) {
    val t = LocalTokens.current
    Row(
        modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(t.surface).padding(start = 7.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppTile(m.pkg, m.letter, 30.dp, 11.dp, 13)
        BasicText(text, style = ts(14, 800, t.ink), maxLines = 1)
    }
}

// ── Reflect ───────────────────────────────────────────────────────────────

private sealed interface Entry {
    val ts: Long
    data class Used(override val ts: Long, val end: Long) : Entry
    data class LetGo(override val ts: Long) : Entry
    data class Now(override val ts: Long) : Entry
}

@Composable
private fun Reflect(m: InterventionModel, viaSkip: Boolean, skipsLeft: Int, choose: (Outcome, Int?) -> Unit) {
    val t = LocalTokens.current
    val seen = rememberSeen()
    val r = m.reflect
    val blur by animateFloatAsState(if (seen) 0f else 6f, tween(700, 100), label = "blur")
    val pop by animateFloatAsState(if (seen) 1f else 0f, tween(500, 650, CubicBezierEasing(0.34f, 1.5f, 0.64f, 1f)), label = "pop")

    Column(
        Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.enter(seen, dy = (-22).dp, durationMs = 650, fadeMs = 400).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AppPill(m, m.label, Modifier.weight(1f, fill = false))
                Box(
                    Modifier
                        .padding(start = 8.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (viaSkip) t.ink else t.surface)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        if (viaSkip) "Skipped · $skipsLeft left" else Stats.hm(m.now),
                        style = ts(13, 800, if (viaSkip) t.bg else t.muted, tabular = true),
                    )
                }
            }
            Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                BasicText(
                    m.line,
                    Modifier.enter(seen, delayMs = 100, dy = 18.dp, durationMs = 800, fadeMs = 600).blur(blur.dp),
                    style = ts(30, 900, t.ink, spacing = -0.03f, lineHeight = 1.12f),
                )
                BasicText(
                    buildAnnotatedString {
                        append("You've tried to open ${m.label} ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Black, color = t.accent)) {
                            append(if (r.triesToday == 1) "once" else "${r.triesToday} times")
                        }
                        append(" today.")
                    },
                    Modifier.enter(seen, delayMs = 260, dy = 18.dp, fadeMs = 500),
                    style = ts(16, 700, t.muted, lineHeight = 1.4f),
                )
            }
            UsedToday(m, seen, pop)
        }

        Column(
            Modifier.padding(top = 12.dp).enter(seen, delayMs = 560, dy = 40.dp, durationMs = 800, easing = CubicBezierEasing(0.34f, 1.2f, 0.64f, 1f)),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val canOpen = m.opensLeft > 0
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("Close", t.primary, t.onPrimary, null, Modifier.weight(1f)) { choose(Outcome.LET_GO, null) }
                if (canOpen) {
                    when (m.length) {
                        SessionLength.ASK, SessionLength.NO_LIMIT ->
                            BigButton("Open, no limit", t.bg, t.ink, t.line, Modifier.weight(1f)) { choose(Outcome.NO_LIMIT, null) }
                        else ->
                            BigButton("Open for ${m.length.minutes}m", t.bg, t.ink, t.line, Modifier.weight(1f)) { choose(Outcome.OPEN, m.length.minutes) }
                    }
                }
            }
            if (!canOpen) {
                BasicText(
                    "That's all ${m.dailyOpens} of today's opens for ${m.label}.",
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    style = ts(13, 700, t.muted).copy(textAlign = TextAlign.Center),
                )
            } else if (m.length == SessionLength.ASK) {
                Row(
                    Modifier.fillMaxWidth().semantics { contentDescription = "Or open ${m.label} with a timer" },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText("or set a timer", Modifier.padding(end = 4.dp), style = ts(13, 700, t.muted))
                    listOf(5, 10, 15).forEach { mins ->
                        Box(
                            Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).tap { choose(Outcome.OPEN, mins) }.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) { BasicText("${mins}m", style = ts(14, 800, t.accent)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsedToday(m: InterventionModel, seen: Boolean, pop: Float) {
    val t = LocalTokens.current
    val r = m.reflect
    val sessions = m.sessions.value
    val usedMs = sessions.sumOf { it.ms }
    val entries = remember(r, sessions) {
        (sessions.map { Entry.Used(it.start, it.end) } + r.letGoTimes.map { Entry.LetGo(it) })
            .sortedBy { it.ts }
            .takeLast(4) + Entry.Now(m.now)
    }
    Column(
        Modifier
            .enter(seen, delayMs = 380, dy = 18.dp, durationMs = 750)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(t.surface)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            BasicText("Used today", Modifier.weight(1f), style = ts(14, 800, t.ink))
            BasicText(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = ts(16, 900).fontSize, fontWeight = FontWeight.Black, color = t.ink)) {
                        append("${(usedMs / 60_000L)} min")
                    }
                    append(" · ${sessions.size} ${if (sessions.size == 1) "session" else "sessions"}")
                },
                style = ts(13, 700, t.muted),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BoxWithConstraints(
                Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(18.dp)).background(t.bg),
            ) {
                val w = maxWidth
                fun x(ts: Long) = w * Rules.stripFraction(com.pausespace.app.data.Usage.minuteOfDay(ts))
                sessions.forEach { s ->
                    val start = x(s.start)
                    val width = maxOf(8.dp, x(s.end) - start)
                    Box(
                        Modifier
                            .offset(x = start)
                            .padding(vertical = 8.dp)
                            .width(width)
                            .height(20.dp)
                            .graphicsLayer { scaleY = pop }
                            .clip(RoundedCornerShape(4.dp))
                            .background(t.ink),
                    )
                }
                r.letGoTimes.forEach { ts ->
                    Box(
                        Modifier
                            .offset(x = x(ts) - 6.dp, y = 12.dp)
                            .size(12.dp)
                            .graphicsLayer { scaleX = pop; scaleY = pop }
                            .border(3.dp, t.accent, CircleShape),
                    )
                }
                Box(
                    Modifier
                        .offset(x = x(m.now) - 2.dp, y = 6.dp)
                        .width(4.dp)
                        .height(24.dp)
                        .graphicsLayer { scaleY = pop }
                        .clip(RoundedCornerShape(2.dp))
                        .background(t.accent),
                )
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("06", "09", "12", "15", "18", "21").forEach {
                    BasicText(it, style = ts(11, 800, t.muted, tabular = true))
                }
            }
        }
        Column {
            entries.forEachIndexed { i, e ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.5.dp).background(t.bg))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.width(8.dp), contentAlignment = Alignment.Center) {
                        when (e) {
                            is Entry.Used -> Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(t.ink))
                            is Entry.LetGo -> Box(Modifier.size(8.dp).border(2.dp, t.accent, CircleShape))
                            is Entry.Now -> Box(Modifier.width(3.dp).height(10.dp).clip(RoundedCornerShape(2.dp)).background(t.accent))
                        }
                    }
                    val (left, right, rightColor, rightWeight) = when (e) {
                        is Entry.Used -> Quad("${Stats.hm(e.ts)} → ${Stats.hm(e.end)}", "${maxOf(1, (e.end - e.ts) / 60_000L)} min", t.muted, 700)
                        is Entry.LetGo -> Quad(Stats.hm(e.ts), "Let go", t.accent, 800)
                        is Entry.Now -> Quad(Stats.hm(e.ts), "Now", t.muted, 700)
                    }
                    BasicText(left, Modifier.weight(1f), style = ts(14, 700, t.ink, tabular = true))
                    BasicText(right, style = ts(13, rightWeight, rightColor))
                }
            }
        }
    }
}

private data class Quad(val a: String, val b: String, val c: Color, val d: Int)

@Composable
private fun BigButton(text: String, bg: Color, fg: Color, border: Color?, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(bg)
            .then(if (border != null) Modifier.border(1.5.dp, border, RoundedCornerShape(30.dp)) else Modifier)
            .tap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { BasicText(text, style = ts(16, 800, fg), maxLines = 1) }
}

// ── Done ──────────────────────────────────────────────────────────────────

@Composable
private fun Done(m: InterventionModel, outcome: Outcome, minutes: Int?, onCta: () -> Unit) {
    val t = LocalTokens.current
    val seen = rememberSeen()
    val dash by animateFloatAsState(if (seen) 1f else 0f, tween(1100, 350, EaseInOut), label = "dash")
    val rot by animateFloatAsState(if (seen) 0f else -8f, tween(900, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)), label = "rot")

    val (title, body, cta) = when (outcome) {
        Outcome.OPEN -> Triple(
            "$minutes minutes.",
            "${m.label} opens now. PauseSpace will check in at ${Stats.hm(System.currentTimeMillis() + (minutes ?: 0) * 60_000L)}.",
            "Open ${m.label}",
        )
        Outcome.NO_LIMIT -> Triple(
            "No limit.",
            "${m.label} opens now. PauseSpace stays quiet until you close it. The time still counts toward today.",
            "Open ${m.label}",
        )
        Outcome.LET_GO -> Triple(
            "Let go.",
            "${m.label} stays closed. That is your ${Rules.ordinal(m.reflect.letGoTotalToday + 1)} let-go today.",
            "Back to home screen",
        )
    }

    Column(
        Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier)
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                Modifier
                    .enter(seen, dy = 0.dp, scaleFrom = 0.6f, durationMs = 900, fadeMs = 400, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f))
                    .graphicsLayer { rotationZ = rot }
                    .size(120.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(t.surface),
                contentAlignment = Alignment.Center,
            ) { Mark(80.dp, t.accent, barWidth = 6f, waveWidth = 3f, wave = dash) }
            BasicText(
                title,
                Modifier.enter(seen, delayMs = 250, dy = 18.dp),
                style = ts(46, 900, t.ink, spacing = -0.04f, lineHeight = 1f).copy(textAlign = TextAlign.Center),
            )
            BasicText(
                body,
                Modifier.widthIn(max = 290.dp).enter(seen, delayMs = 380, dy = 18.dp),
                style = ts(16, 600, t.muted, lineHeight = 1.45f).copy(textAlign = TextAlign.Center),
            )
        }
        BigButton(
            cta, t.primary, t.onPrimary, null,
            Modifier.fillMaxWidth().enter(seen, delayMs = 520, dy = 40.dp, durationMs = 800, easing = CubicBezierEasing(0.34f, 1.2f, 0.64f, 1f)),
            onClick = onCta,
        )
    }
}

@Composable
private fun rememberSeen(): Boolean {
    val reduced = reducedMotion()
    var seen by remember { mutableStateOf(reduced) }
    LaunchedEffect(Unit) {
        delay(50)
        seen = true
    }
    return seen
}
