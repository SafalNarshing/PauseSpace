package com.pausespace.app.data

import kotlin.random.Random

/** Pure decision logic, kept free of Android types so it is unit-testable. */
object Rules {
    const val MAX_ROUNDS = 6

    /** Is the rule's pause window active at [minuteOfDay] (0..1439)? Windows may wrap midnight. */
    fun inSchedule(rule: AppRule, minuteOfDay: Int): Boolean {
        if (rule.allDay) return true
        val s = rule.startMin
        val e = rule.endMin
        return when {
            s == e -> true
            s < e -> minuteOfDay in s until e
            else -> minuteOfDay >= s || minuteOfDay < e
        }
    }

    /** "Longer with each try": +1 round from the 3rd try of the day, one more for each try after. */
    fun rounds(ritual: Ritual, rule: AppRule, tryNumberToday: Int): Int {
        val extra = if (rule.grow && tryNumberToday >= 3) tryNumberToday - 2 else 0
        return (ritual.rounds + extra).coerceIn(1, MAX_ROUNDS)
    }

    fun line(words: Words, random: Random = Random.Default): String {
        if (!words.shuffle) return words.current
        val pool = WordLines + listOfNotNull(words.custom.trim().takeIf { it.isNotEmpty() })
        return pool[random.nextInt(pool.size)]
    }

    fun skipsLeft(perDay: Int, usedToday: Int) = (perDay - usedToday).coerceAtLeast(0)

    fun opensLeft(rule: AppRule, opensToday: Int) = (rule.dailyOpens - opensToday).coerceAtLeast(0)

    /** Position on the 06:00–21:00 strip in the "Used today" card, as 0..1. */
    fun stripFraction(minuteOfDay: Int): Float = ((minuteOfDay / 60f - 6f) / 15f).coerceIn(0f, 1f)

    fun scheduleLabel(rule: AppRule): String =
        if (rule.allDay) "All day,\nevery day" else "${hm(rule.startMin)} –\n${hm(rule.endMin)}"

    fun hm(minuteOfDay: Int) = "%02d:%02d".format(minuteOfDay / 60 % 24, minuteOfDay % 60)

    /** 102 min → "1h 42m", 242 min → "4h 02m", 52 min → "52m". */
    fun duration(ms: Long): String {
        val mins = (ms / 60_000L).toInt()
        return if (mins >= 60) "${mins / 60}h ${"%02d".format(mins % 60)}m" else "${mins}m"
    }

    fun ordinal(n: Int): String {
        val suffix = if (n % 100 in 11..13) "th" else when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }
}
