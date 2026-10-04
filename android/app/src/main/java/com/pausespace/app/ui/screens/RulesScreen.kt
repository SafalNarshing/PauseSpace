package com.pausespace.app.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pausespace.app.data.AppDay
import com.pausespace.app.data.AppRule
import com.pausespace.app.data.Ritual
import com.pausespace.app.data.Rules
import com.pausespace.app.data.SessionLength
import com.pausespace.app.ui.components.AppTile
import com.pausespace.app.ui.components.BackHeader
import com.pausespace.app.ui.components.ChevronChip
import com.pausespace.app.ui.components.Icon
import com.pausespace.app.ui.components.Icons
import com.pausespace.app.ui.components.RowLabel
import com.pausespace.app.ui.components.StepButton
import com.pausespace.app.ui.components.Toggle
import com.pausespace.app.ui.components.enter
import com.pausespace.app.ui.components.rememberShown
import com.pausespace.app.ui.components.tap
import com.pausespace.app.ui.theme.LocalTokens
import com.pausespace.app.ui.theme.ts

@Composable
fun RulesScreen(
    pkg: String,
    label: String,
    rule: AppRule,
    ritual: Ritual,
    day: AppDay?,
    onBack: () -> Unit,
    onRitual: () -> Unit,
    onChange: ((AppRule) -> AppRule) -> Unit,
) {
    val t = LocalTokens.current
    val shown = rememberShown()
    var editingHours by remember { mutableStateOf(false) }

    Column(
        Modifier.enter(shown, dy = 0.dp, dx = 40.dp, durationMs = 650, fadeMs = 450),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackHeader("App rules", "Back to apps", onBack)

        Column(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppTile(pkg, label.take(1).uppercase(), 76.dp, 26.dp, 30)
            BasicText(label, style = ts(30, 900, t.ink, spacing = -0.03f, lineHeight = 1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("${Rules.duration(day?.ms ?: 0)} today", t.surface, t.ink)
                Chip("${day?.tries ?: 0} ${if (day?.tries == 1) "try" else "tries"}", t.surface, t.ink)
                Chip("${day?.letGo ?: 0} let go", t.primary, t.onPrimary)
            }
        }

        if (!rule.paused) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(t.surface2)
                    .padding(start = 20.dp, end = 14.dp)
                    .heightIn(min = 66.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RowLabel("Pause $label", "Breathe before it opens")
                Toggle(false, "Pause $label") { v -> onChange { it.copy(paused = v) } }
            }
        }

        Section {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).tap(onClick = onRitual).padding(start = 20.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RowLabel("Breathing", "Ritual default · ${ritual.pattern}, ${ritual.rounds} ${if (ritual.rounds == 1) "round" else "rounds"}")
                ChevronChip(36.dp, t.bg)
            }
            Divider()
            SwitchRow("Longer with each try", "+1 round from the 3rd try of the day", rule.grow, "Longer pause with each try") { v ->
                onChange { it.copy(grow = v) }
            }
        }

        Section {
            Column(
                Modifier.padding(start = 20.dp, end = 14.dp, top = 18.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    BasicText("When I open it anyway", style = ts(15, 800, t.ink))
                    BasicText("How long before PauseSpace checks in", style = ts(13, 600, t.muted))
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(t.bg).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SessionLength.entries.forEach { o ->
                        val on = o == rule.length
                        Box(
                            Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (on) t.primary else t.bg)
                                .tap { onChange { it.copy(length = o) } },
                            contentAlignment = Alignment.Center,
                        ) { BasicText(o.label, style = ts(13, 800, if (on) t.onPrimary else t.muted), maxLines = 1) }
                    }
                }
            }
            Divider()
            SwitchRow("Breathe again when time is up", "Off for No limit sessions", rule.checkIn, "Breathe again when time is up") { v ->
                onChange { it.copy(checkIn = v) }
            }
        }

        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(28.dp)).background(t.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            ) {
                BasicText("Daily opens", style = ts(13, 700, t.muted))
                BasicText("${rule.dailyOpens}", style = ts(34, 900, t.ink, spacing = -0.03f, lineHeight = 1f, tabular = true))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StepButton(Icons.Minus, "Fewer daily opens", t.bg) { onChange { it.copy(dailyOpens = (it.dailyOpens - 1).coerceIn(1, 30)) } }
                    StepButton(Icons.Plus, "More daily opens", t.bg) { onChange { it.copy(dailyOpens = (it.dailyOpens + 1).coerceIn(1, 30)) } }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(28.dp))
                    .background(t.surface)
                    .tap { editingHours = true }
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                BasicText("Paused during", style = ts(13, 700, t.muted))
                BasicText(Rules.scheduleLabel(rule), style = ts(22, 900, t.ink, spacing = -0.02f, lineHeight = 1.1f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText("Set hours", style = ts(13, 800, t.accent))
                    Icon(Icons.Chevron, t.accent, 14.dp)
                }
            }
        }

        if (rule.paused) {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .tap { onChange { it.copy(paused = false) } },
                contentAlignment = Alignment.Center,
            ) { BasicText("Stop pausing $label", style = ts(15, 800, t.muted)) }
        }
    }

    if (editingHours) {
        HoursDialog(rule, onDismiss = { editingHours = false }, onChange = onChange)
    }
}

@Composable
private fun Chip(text: String, bg: Color, fg: Color) {
    Box(
        Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(bg).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { BasicText(text, style = ts(13, 800, fg)) }
}

@Composable
private fun Section(content: @Composable () -> Unit) {
    val t = LocalTokens.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(t.surface)) { content() }
}

@Composable
private fun Divider() {
    val t = LocalTokens.current
    Box(Modifier.fillMaxWidth().height(1.5.dp).background(t.bg))
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, a11y: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(start = 20.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowLabel(title, sub)
        Toggle(checked, a11y, onChange)
    }
}

@Composable
private fun HoursDialog(rule: AppRule, onDismiss: () -> Unit, onChange: ((AppRule) -> AppRule) -> Unit) {
    val t = LocalTokens.current
    val context = LocalContext.current
    fun pick(initial: Int, set: (Int) -> Unit) {
        TimePickerDialog(context, { _, h, m -> set(h * 60 + m) }, initial / 60, initial % 60, true).show()
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(t.bg).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicText("Paused during", Modifier.padding(start = 4.dp), style = ts(20, 900, t.ink))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(t.surface).padding(start = 18.dp, end = 10.dp).heightIn(min = 66.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RowLabel("All day, every day", "Pause whenever it opens")
                Toggle(rule.allDay, "All day") { v -> onChange { it.copy(allDay = v) } }
            }
            if (!rule.allDay) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("From" to rule.startMin, "Until" to rule.endMin).forEachIndexed { i, (name, value) ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(t.surface)
                                .tap {
                                    pick(value) { m -> onChange { if (i == 0) it.copy(startMin = m) else it.copy(endMin = m) } }
                                }
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            BasicText(name, style = ts(13, 700, t.muted))
                            BasicText(Rules.hm(value), style = ts(26, 900, t.ink, tabular = true))
                        }
                    }
                }
                BasicText(
                    "Outside these hours the app opens without a pause.",
                    Modifier.padding(horizontal = 4.dp),
                    style = ts(13, 600, t.muted),
                )
            }
            Box(
                Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(t.primary).tap(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) { BasicText("Done", style = ts(16, 800, t.onPrimary)) }
        }
    }
}
