package com.pausespace.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pausespace.app.ui.components.Icon
import com.pausespace.app.ui.components.Icons
import com.pausespace.app.ui.components.Mark
import com.pausespace.app.ui.components.enter
import com.pausespace.app.ui.components.rememberShown
import com.pausespace.app.ui.components.tap
import com.pausespace.app.ui.theme.LocalTokens
import com.pausespace.app.ui.theme.ts

private const val REPO_URL = "https://github.com/SafalNarshing/PauseSpace"
private const val PRIVACY_URL = "https://safalnarshing.github.io/PauseSpace/privacy/"

@Composable
fun AboutScreen() {
    val t = LocalTokens.current
    val context = LocalContext.current
    val shown = rememberShown()
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.enter(shown, durationMs = 650, fadeMs = 450).padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BasicText("About", style = ts(36, 900, t.ink, spacing = -0.035f, lineHeight = 1f))
            BasicText("Why PauseSpace exists, and how it works.", style = ts(15, 600, t.muted))
        }

        // Hero: mark, name, tagline, version
        Box(
            Modifier
                .enter(shown, delayMs = 60, scaleFrom = 0.97f, durationMs = 800)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(t.heroBg)
                .clipToBounds(),
        ) {
            Mark(
                200.dp, t.heroFg.copy(alpha = 0.13f), barWidth = 7f, waveWidth = 3f,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-30).dp),
            )
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(t.heroFg),
                    contentAlignment = Alignment.Center,
                ) { Mark(44.dp, t.heroBg, barWidth = 9f, waveWidth = 5f) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicText(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Black)) { append("Pause") }
                            withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append("Space") }
                        },
                        style = ts(34, 900, t.heroFg, spacing = -0.03f, lineHeight = 1f),
                    )
                    BasicText("A breath of room between you and the feed.", style = ts(16, 700, t.heroSub, lineHeight = 1.35f))
                }
                if (version.isNotEmpty()) {
                    Box(
                        Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(t.heroDim).padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { BasicText("Version $version", style = ts(12, 800, t.heroFg)) }
                }
            }
        }

        // What it is
        Card(shown, 140) {
            BasicText("What it is", style = ts(17, 800, t.ink))
            BasicText(
                "PauseSpace puts a short, calm breathing pause in front of the apps you reach for on autopilot. " +
                    "Instead of landing in the feed, you take one breath, then decide whether you really want to go in.",
                style = ts(14, 600, t.muted, lineHeight = 1.45f),
            )
        }

        // How it works
        Card(shown, 200) {
            BasicText("How it works", style = ts(17, 800, t.ink))
            Step(1, "Reach", "Open an app you've chosen to pause.")
            Step(2, "Breathe", "A calm, full-screen breathing pause appears first.")
            Step(3, "Decide", "Close it, or open it on purpose, with a timer or no limit.")
        }

        // Privacy
        Card(shown, 260) {
            BasicText("Private by design", style = ts(17, 800, t.ink))
            BasicText(
                "Everything stays on this phone. No accounts, no tracking. PauseSpace only notices when an app opens, never what you do inside it.",
                style = ts(14, 600, t.muted, lineHeight = 1.45f),
            )
        }

        // Open source on GitHub
        Column(
            Modifier
                .enter(shown, delayMs = 320)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(t.ink)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(t.bg), contentAlignment = Alignment.Center) {
                    Icon(Icons.GitHub, t.ink, 26.dp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    BasicText("Open source on GitHub", style = ts(16, 800, t.bg))
                    BasicText("SafalNarshing/PauseSpace", style = ts(13, 600, t.bg.copy(alpha = 0.7f)))
                }
            }
            BasicText(
                "Check it out, and if you love it, give it a star ⭐",
                style = ts(15, 700, t.bg, lineHeight = 1.4f),
            )
            Row(
                Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(t.bg)
                    .tap(label = "Open the PauseSpace GitHub repository", role = Role.Button) { openUrl(context, REPO_URL) }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.GitHub, t.ink, 18.dp)
                BasicText("View on GitHub", style = ts(14, 800, t.ink))
                Icon(Icons.External, t.ink, 16.dp)
            }
        }

        // Review on Play Store
        Row(
            Modifier
                .enter(shown, delayMs = 380)
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(t.primary)
                .tap(label = "Review PauseSpace on the Play Store", role = Role.Button) { openPlayStore(context) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Star, t.onPrimary, 20.dp)
            BasicText("  Review us on Play Store", style = ts(16, 800, t.onPrimary))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .tap(label = "Open the privacy policy", role = Role.Button) { openUrl(context, PRIVACY_URL) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            BasicText("Privacy policy", style = ts(14, 800, t.accent))
            BasicText("  ", style = ts(14, 800, t.accent))
            Icon(Icons.External, t.accent, 14.dp)
        }

        BasicText(
            "Made with care for people who want their attention back.",
            Modifier.fillMaxWidth().padding(top = 6.dp),
            style = ts(13, 600, t.muted).copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
        )
    }
}

@Composable
private fun Card(shown: Boolean, delayMs: Int, content: @Composable () -> Unit) {
    val t = LocalTokens.current
    Column(
        Modifier
            .enter(shown, delayMs = delayMs)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(t.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
private fun Step(n: Int, title: String, body: String) {
    val t = LocalTokens.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(t.bg), contentAlignment = Alignment.Center) {
            BasicText("$n", style = ts(14, 900, t.accent))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            BasicText(title, style = ts(15, 800, t.ink))
            BasicText(body, style = ts(13, 600, t.muted, lineHeight = 1.35f))
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Opens the Play Store listing, falling back to the web listing when the Play Store app isn't installed. */
private fun openPlayStore(context: Context) {
    val id = context.packageName
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        openUrl(context, "https://play.google.com/store/apps/details?id=$id")
    }
}
