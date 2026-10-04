package com.pausespace.app.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.pausespace.app.data.WordLines
import com.pausespace.app.data.Words
import com.pausespace.app.ui.components.BackHeader
import com.pausespace.app.ui.components.Mark
import com.pausespace.app.ui.components.RowLabel
import com.pausespace.app.ui.components.Toggle
import com.pausespace.app.ui.components.enter
import com.pausespace.app.ui.components.rememberShown
import com.pausespace.app.ui.components.tap
import com.pausespace.app.ui.theme.LocalTokens
import com.pausespace.app.ui.theme.ts

private const val MAX_CUSTOM = 80
private val Fan = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1f)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordsScreen(words: Words, onBack: () -> Unit, onChange: ((Words) -> Words) -> Unit) {
    val t = LocalTokens.current
    val shown = rememberShown()

    Column(
        Modifier.enter(shown, dy = 0.dp, dx = 40.dp, durationMs = 650, fadeMs = 450),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BackHeader("Words on screen", "Back to Ritual", onBack)

        // Preview card with two fanned cards behind it
        Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(196.dp)) {
            Box(
                Modifier
                    .enter(shown, delayMs = 450, dy = 22.dp, durationMs = 800, fadeMs = 0, easing = Fan)
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(t.surface2),
            )
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .enter(shown, delayMs = 350, dy = 12.dp, durationMs = 800, fadeMs = 0, easing = Fan)
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .height(172.dp)
                    .alpha(0.55f)
                    .clip(RoundedCornerShape(30.dp))
                    .background(t.edge),
            )
            Column(
                Modifier
                    .padding(top = 22.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(30.dp))
                    .background(t.heroBg)
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mark(18.dp, t.heroSub, barWidth = 11f, waveWidth = 7f)
                    BasicText(
                        if (words.shuffle) "Shuffled · shown while you breathe" else "Shown while you breathe",
                        style = ts(12, 800, t.heroSub),
                    )
                }
                BasicText(words.current, style = ts(25, 800, t.heroFg, spacing = -0.02f, lineHeight = 1.2f), maxLines = 3)
            }
        }

        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BasicText("Pick a line", Modifier.padding(horizontal = 4.dp), style = ts(15, 800, t.ink))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WordLines.forEachIndexed { i, line ->
                    val on = !words.customActive && i == words.selected
                    Box(
                        Modifier
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (on) t.primary else t.bg)
                            .border(1.5.dp, if (on) t.primary else t.line, RoundedCornerShape(22.dp))
                            .tap { onChange { it.copy(selected = i, useCustom = false) } }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) { BasicText(line, style = ts(14, 700, if (on) t.onPrimary else t.ink, lineHeight = 1.3f)) }
                }
            }
        }

        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText("Or write your own", Modifier.padding(horizontal = 4.dp), style = ts(15, 800, t.ink))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(t.surface)
                    .border(1.5.dp, if (words.customActive) t.primary else t.surface, RoundedCornerShape(26.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                if (words.custom.isEmpty()) {
                    BasicText("Something only you would say to yourself", style = ts(15, 600, t.muted, lineHeight = 1.4f))
                }
                BasicTextField(
                    words.custom,
                    { v -> onChange { it.copy(custom = v.take(MAX_CUSTOM), useCustom = v.isNotBlank()) } },
                    Modifier.fillMaxWidth(),
                    textStyle = ts(15, 600, t.ink, lineHeight = 1.4f),
                    cursorBrush = SolidColor(t.accent),
                    minLines = 2,
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(1.5.dp, t.line, RoundedCornerShape(26.dp))
                .padding(start = 20.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RowLabel("Shuffle each time", "Rotate through every line")
            Toggle(words.shuffle, "Shuffle each time") { v -> onChange { it.copy(shuffle = v) } }
        }
    }
}
