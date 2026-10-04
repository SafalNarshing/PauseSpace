package com.pausespace.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pausespace.app.data.BreathPresets
import com.pausespace.app.data.Ritual
import com.pausespace.app.data.Words
import com.pausespace.app.ui.components.ChevronChip
import com.pausespace.app.ui.components.EaseInOut
import com.pausespace.app.ui.components.Icons
import com.pausespace.app.ui.components.RowLabel
import com.pausespace.app.ui.components.StepButton
import com.pausespace.app.ui.components.enter
import com.pausespace.app.ui.components.rememberCountUp
import com.pausespace.app.ui.components.rememberShown
import com.pausespace.app.ui.components.tap
import com.pausespace.app.ui.theme.LocalTokens
import com.pausespace.app.ui.theme.ts
import kotlin.math.roundToInt

@Composable
fun RitualScreen(
    ritual: Ritual,
    words: Words,
    onChange: ((Ritual) -> Ritual) -> Unit,
    onWords: () -> Unit,
) {
    val t = LocalTokens.current
    val shown = rememberShown()
    val c = rememberCountUp(shown, 1000)
    val dash by animateFloatAsState(if (shown) 1f else 0f, tween(1400, 350, EaseInOut), label = "dash")
    val area by animateFloatAsState(if (shown) 0.14f else 0f, tween(700, 1200), label = "area")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.enter(shown, durationMs = 650, fadeMs = 450).padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BasicText("Ritual", style = ts(36, 900, t.ink, spacing = -0.035f, lineHeight = 1f))
            BasicText("The breath that comes before an app opens.", style = ts(15, 600, t.muted))
        }

        // Hero: total pause + breath wave
        Column(
            Modifier
                .enter(shown, delayMs = 60, scaleFrom = 0.97f, durationMs = 800)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(t.heroBg)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicText(
                        "${(ritual.cycleSeconds * ritual.rounds * c).roundToInt()}",
                        style = ts(54, 900, t.heroFg, spacing = -0.04f, lineHeight = 0.9f, tabular = true),
                    )
                    BasicText("sec pause", Modifier.padding(bottom = 4.dp), style = ts(18, 800, t.heroSub))
                }
                BasicText("${ritual.rounds} × breath", Modifier.padding(bottom = 4.dp), style = ts(13, 800, t.heroSub))
            }
            BreathWave(ritual, dash, area, Modifier.padding(top = 4.dp).fillMaxWidth().height(96.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val segs = buildList {
                    add(Triple("In ${ritual.inhale}s", ritual.inhale, TextAlign.Start))
                    if (ritual.hold > 0) add(Triple("Hold ${ritual.hold}s", ritual.hold, TextAlign.Center))
                    add(Triple("Out ${ritual.exhale}s", ritual.exhale, TextAlign.End))
                }
                segs.forEach { (label, w, align) ->
                    BasicText(
                        label,
                        Modifier.weight(w.toFloat()),
                        style = ts(12, 800, t.heroSub).copy(textAlign = align),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }

        // Presets
        Row(
            Modifier.enter(shown, delayMs = 160, durationMs = 650, fadeMs = 450),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BreathPresets.forEach { p ->
                val on = ritual.preset == p.id
                Column(
                    Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (on) t.ink else t.bg)
                        .border(1.5.dp, if (on) t.ink else t.line, RoundedCornerShape(22.dp))
                        .tap { onChange { it.copy(inhale = p.inhale, hold = p.hold, exhale = p.exhale, preset = p.id) } },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val fg = if (on) t.bg else t.ink
                    BasicText(p.name, style = ts(14, 800, fg, lineHeight = 1.15f))
                    BasicText(
                        "${p.inhale}·${p.hold}·${p.exhale}",
                        style = ts(12, 700, fg.copy(alpha = 0.75f), tabular = true),
                    )
                }
            }
        }

        // Steppers
        Column(
            Modifier.enter(shown, delayMs = 230, durationMs = 650, fadeMs = 450),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            data class St(val label: String, val v: String, val dec: String, val inc: String, val step: (Ritual, Int) -> Ritual)
            val steppers = listOf(
                St("Breathe in", "${ritual.inhale}s", "Shorter breath in", "Longer breath in") { r, d -> r.copy(inhale = (r.inhale + d).coerceIn(1, 12), preset = null) },
                St("Hold", "${ritual.hold}s", "Shorter hold", "Longer hold") { r, d -> r.copy(hold = (r.hold + d).coerceIn(0, 12), preset = null) },
                St("Breathe out", "${ritual.exhale}s", "Shorter breath out", "Longer breath out") { r, d -> r.copy(exhale = (r.exhale + d).coerceIn(1, 12), preset = null) },
                St("Rounds", "${ritual.rounds}×", "Fewer rounds", "More rounds") { r, d -> r.copy(rounds = (r.rounds + d).coerceIn(1, 6)) },
            )
            steppers.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { st ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(26.dp)).background(t.surface).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
                                BasicText(st.label, Modifier.weight(1f).padding(bottom = 4.dp), style = ts(13, 800, t.muted))
                                BasicText(st.v, style = ts(26, 900, t.ink, spacing = -0.03f, tabular = true))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StepButton(Icons.Minus, st.dec, t.bg) { onChange { st.step(it, -1) } }
                                StepButton(Icons.Plus, st.inc, t.bg) { onChange { st.step(it, 1) } }
                            }
                        }
                    }
                }
            }
        }

        // Skips per day
        Row(
            Modifier
                .enter(shown, delayMs = 300, durationMs = 650, fadeMs = 450)
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(1.5.dp, t.line, RoundedCornerShape(26.dp))
                .padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RowLabel("Skips per day", "Jump straight to the choice")
            StepButton(Icons.Minus, "Fewer skips", t.surface, Modifier.width(44.dp).clip(CircleShape)) {
                onChange { it.copy(skipsPerDay = (it.skipsPerDay - 1).coerceIn(0, 10)) }
            }
            BasicText(
                "${ritual.skipsPerDay}",
                Modifier.width(26.dp),
                style = ts(20, 900, t.ink, tabular = true).copy(textAlign = TextAlign.Center),
            )
            StepButton(Icons.Plus, "More skips", t.surface, Modifier.width(44.dp).clip(CircleShape)) {
                onChange { it.copy(skipsPerDay = (it.skipsPerDay + 1).coerceIn(0, 10)) }
            }
        }

        // Words on screen
        Row(
            Modifier
                .enter(shown, delayMs = 360, durationMs = 650, fadeMs = 450)
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(1.5.dp, t.line, RoundedCornerShape(26.dp))
                .tap(onClick = onWords)
                .padding(start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RowLabel("Words on screen", if (words.shuffle) "Shuffled each time" else words.current, subMaxLines = 1)
            ChevronChip(40.dp, t.surface)
        }
    }
}

/** The design's in → hold → out curve on a 300×100 box, drawn with a dash entrance and soft fill. */
@Composable
private fun BreathWave(r: Ritual, dash: Float, area: Float, modifier: Modifier) {
    val t = LocalTokens.current
    Canvas(modifier) {
        val sx = size.width / 300f
        val sy = size.height / 100f
        val total = r.cycleSeconds.toFloat()
        val w = 300f
        val xi = r.inhale / total * w
        val xh = xi + r.hold / total * w
        val lo = 86f
        val hi = 14f
        val line = Path().apply {
            moveTo(0f, lo * sy)
            cubicTo(xi * 0.55f * sx, lo * sy, xi * 0.45f * sx, hi * sy, xi * sx, hi * sy)
            lineTo(xh * sx, hi * sy)
            cubicTo((xh + (w - xh) * 0.55f) * sx, hi * sy, (xh + (w - xh) * 0.45f) * sx, lo * sy, w * sx, lo * sy)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(w * sx, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, t.heroFg.copy(alpha = area))
        val pm = PathMeasure().apply { setPath(line, false) }
        val seg = Path()
        pm.getSegment(0f, pm.length * dash, seg, true)
        drawPath(seg, t.heroFg, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

