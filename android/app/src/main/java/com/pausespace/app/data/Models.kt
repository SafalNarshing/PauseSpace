package com.pausespace.app.data

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class BreathPreset(val id: String, val name: String, val inhale: Int, val hold: Int, val exhale: Int)

val BreathPresets = listOf(
    BreathPreset("calm", "Calm", 4, 4, 6),
    BreathPreset("even", "Even", 4, 4, 4),
    BreathPreset("deep", "Deep", 4, 7, 8),
    BreathPreset("quick", "Quick", 3, 0, 3),
)

data class Ritual(
    val inhale: Int = 4,
    val hold: Int = 4,
    val exhale: Int = 6,
    val rounds: Int = 2,
    /** Matching preset id, or null once a stepper has customised the pattern. */
    val preset: String? = "calm",
    val skipsPerDay: Int = 3,
) {
    val cycleSeconds get() = inhale + hold + exhale
    val pattern get() = "$inhale·$hold·$exhale"
}

val WordLines = listOf(
    "Is this a choice, or a reflex?",
    "What were you about to do?",
    "You can open it. You don't have to.",
    "Nothing has changed since you last looked.",
    "Breathe first. Decide after.",
)

data class Words(
    val selected: Int = 0,
    val custom: String = "",
    val useCustom: Boolean = false,
    val shuffle: Boolean = false,
) {
    val customActive get() = useCustom && custom.isNotBlank()
    val current get() = if (customActive) custom.trim() else WordLines[selected.coerceIn(WordLines.indices)]
}

/** "When I open it anyway": how long before PauseSpace checks in. */
enum class SessionLength(val label: String, val minutes: Int?) {
    ASK("Ask", null),
    M5("5m", 5),
    M10("10m", 10),
    M15("15m", 15),
    NO_LIMIT("No limit", null),
}

data class AppRule(
    val paused: Boolean = false,
    val grow: Boolean = true,
    val length: SessionLength = SessionLength.ASK,
    val checkIn: Boolean = true,
    val dailyOpens: Int = 5,
    val allDay: Boolean = true,
    /** Minutes from midnight. A window may wrap past midnight (e.g. 21:00 → 07:00). */
    val startMin: Int = 9 * 60,
    val endMin: Int = 17 * 60,
)

data class Prefs(
    val onboarded: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val ritual: Ritual = Ritual(),
    val words: Words = Words(),
    val rules: Map<String, AppRule> = emptyMap(),
) {
    fun rule(pkg: String) = rules[pkg] ?: AppRule()
    val pausedPackages get() = rules.filterValues { it.paused }.keys
}

enum class Outcome(val key: String) {
    LET_GO("let"),
    OPEN("open"),
    NO_LIMIT("endless"),
    ;

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key }
    }
}

/** One time the breathing pause appeared in front of an app. */
data class Intervention(
    val id: Long,
    val ts: Long,
    val pkg: String,
    val outcome: Outcome?,
    val skipped: Boolean,
)
