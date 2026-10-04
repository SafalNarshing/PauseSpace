package com.pausespace.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pausespace.app.R
import com.pausespace.app.data.ThemeMode

/** Colour tokens, copied 1:1 from the design's `tk()` palette. */
@Immutable
data class Tokens(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val off: Color,
    val accent: Color,
    val primary: Color,
    val onPrimary: Color,
    val heroBg: Color,
    val heroFg: Color,
    val heroSub: Color,
    val heroDim: Color,
    val soft: Color,
    val disc: Color,
    val edge: Color,
    val seg: Color,
    val navBg: Color,
    val navShadow: Color,
)

val LightTokens = Tokens(
    dark = false,
    bg = Color(0xFFFFFFFF),
    surface = Color(0xFFF0F6FD),
    surface2 = Color(0xFFE1EDFA),
    ink = Color(0xFF0B1B2B),
    muted = Color(0xFF56677A),
    line = Color(0xFFDFEAF6),
    off = Color(0xFFC8DAEE),
    accent = Color(0xFF1E6BD6),
    primary = Color(0xFF1E6BD6),
    onPrimary = Color(0xFFFFFFFF),
    heroBg = Color(0xFF1E6BD6),
    heroFg = Color(0xFFFFFFFF),
    heroSub = Color(0xFFEAF2FE),
    heroDim = Color(0x4DFFFFFF),
    soft = Color(0xFFE4F0FD),
    disc = Color(0xFFCFE3FA),
    edge = Color(0xFF8DB8EE),
    seg = Color(0xFFFFFFFF),
    navBg = Color(0xF0FFFFFF),
    navShadow = Color(0x2914468C),
)

val DarkTokens = Tokens(
    dark = true,
    bg = Color(0xFF070C12),
    surface = Color(0xFF0F1924),
    surface2 = Color(0xFF152436),
    ink = Color(0xFFE8F0F8),
    muted = Color(0xFF8FA3B8),
    line = Color(0xFF1B2A3B),
    off = Color(0xFF26384D),
    accent = Color(0xFF6AAAF5),
    primary = Color(0xFF6AAAF5),
    onPrimary = Color(0xFF04192F),
    heroBg = Color(0xFF14304F),
    heroFg = Color(0xFFE8F0F8),
    heroSub = Color(0xFFB5CDEA),
    heroDim = Color(0x38E8F0F8),
    soft = Color(0xFF132B47),
    disc = Color(0xFF173B63),
    edge = Color(0xFF2F6AA8),
    seg = Color(0xFF22364D),
    navBg = Color(0xEB0F1924),
    navShadow = Color(0x8C000000),
)

val LocalTokens = staticCompositionLocalOf { LightTokens }

@OptIn(ExperimentalTextApi::class)
val Nunito = FontFamily(
    listOf(300, 400, 500, 600, 700, 800, 900).map { w ->
        Font(
            R.font.nunito,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    },
)

/** Text style shorthand: size in sp, weight 300–900, letter spacing in em. */
fun ts(
    size: Int,
    weight: Int,
    color: Color = Color.Unspecified,
    spacing: Float = 0f,
    lineHeight: Float? = null,
    tabular: Boolean = false,
): TextStyle = TextStyle(
    fontFamily = Nunito,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    color = color,
    letterSpacing = if (spacing == 0f) TextUnit.Unspecified else spacing.em,
    lineHeight = lineHeight?.let { (size * it).sp } ?: TextUnit.Unspecified,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

/** App-tile tint, the design's `tint(h, s)` helper. */
fun tint(hue: Float, sat: Float, dark: Boolean): Pair<Color, Color> =
    if (dark) {
        Color.hsl(hue, (sat * 0.45f) / 100f, 0.17f) to Color.hsl(hue, sat / 100f, 0.76f)
    } else {
        Color.hsl(hue, minOf(sat, 80f) / 100f, 0.95f) to Color.hsl(hue, sat / 100f, 0.36f)
    }

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun PauseTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTokens provides if (dark) DarkTokens else LightTokens, content = content)
}
