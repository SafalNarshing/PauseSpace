package com.pausespace.app.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayShare(val label: String, val share: Float?, val today: Boolean)

data class AppToday(val pkg: String, val label: String, val ms: Long, val letGo: Int, val tries: Int)

data class TodayStats(
    val dateLabel: String,
    /** One entry per try today, true when it was let go. */
    val ticks: List<Boolean>,
    val screenMs: Long,
    /** Today minus the usual by this time of day; null without history. */
    val vsUsualMs: Long?,
    val skipsUsed: Int,
    val skipsPerDay: Int,
    val week: List<DayShare>,
    val byApp: List<AppToday>,
) {
    val tries get() = ticks.size
    val letGo get() = ticks.count { it }
}

data class AppDay(val ms: Long, val tries: Int, val letGo: Int, val opens: Int)

/** Everything the reflect step of the overlay shows about one app today. */
data class ReflectInfo(
    val triesToday: Int,
    val letGoTimes: List<Long>,
    val opensToday: Int,
    val letGoTotalToday: Int,
)

object Stats {
    private fun isOpen(i: Intervention) = i.outcome == Outcome.OPEN || i.outcome == Outcome.NO_LIMIT

    fun today(context: Context, prefs: Prefs, now: Long = System.currentTimeMillis()): TodayStats {
        val day0 = Usage.startOfDay(now)
        val week0 = Usage.startOfDay(now, 6)
        val log = InterventionLog.since(week0)
        val todayLog = log.filter { it.ts >= day0 }
        val paused = prefs.pausedPackages
        val sessions = Usage.sessions(context, week0, now)

        fun pausedMs(from: Long, to: Long) = paused.sumOf { p ->
            sessions[p].orEmpty().sumOf { s -> (minOf(s.end, to) - maxOf(s.start, from)).coerceAtLeast(0) }
        }

        val screenMs = pausedMs(day0, now)
        val elapsed = now - day0
        val history = (1..6).map { d -> Usage.startOfDay(now, d) }.map { pausedMs(it, it + elapsed) }
        val vsUsual = if (history.any { it > 0 }) screenMs - history.average().toLong() else null

        val dayFmt = SimpleDateFormat("EEEEE", Locale.getDefault())
        val week = (6 downTo 0).map { d ->
            val from = Usage.startOfDay(now, d)
            val to = if (d == 0) now + 1 else Usage.startOfDay(now, d - 1)
            val day = log.filter { it.ts in from until to }
            DayShare(
                label = dayFmt.format(Date(from)),
                share = if (day.isEmpty()) null else day.count { it.outcome == Outcome.LET_GO } / day.size.toFloat(),
                today = d == 0,
            )
        }

        val byApp = paused.map { p ->
            val t = todayLog.filter { it.pkg == p }
            AppToday(
                pkg = p,
                label = Apps.label(context, p),
                ms = sessions[p].orEmpty().sumOf { s -> (s.end - maxOf(s.start, day0)).coerceAtLeast(0) },
                letGo = t.count { it.outcome == Outcome.LET_GO },
                tries = t.size,
            )
        }.sortedWith(compareByDescending<AppToday> { it.ms }.thenByDescending { it.tries }.thenBy { it.label })

        return TodayStats(
            dateLabel = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(now)),
            ticks = todayLog.map { it.outcome == Outcome.LET_GO },
            screenMs = screenMs,
            vsUsualMs = vsUsual,
            skipsUsed = todayLog.count { it.skipped },
            skipsPerDay = prefs.ritual.skipsPerDay,
            week = week,
            byApp = byApp,
        )
    }

    /** Foreground time per package over the last 7 days (including today). */
    fun weekUsage(context: Context, now: Long = System.currentTimeMillis()): Map<String, Long> =
        Usage.sessions(context, Usage.startOfDay(now, 6), now).mapValues { (_, l) -> l.sumOf { it.ms } }

    fun appDay(context: Context, pkg: String, now: Long = System.currentTimeMillis()): AppDay {
        val day0 = Usage.startOfDay(now)
        val t = InterventionLog.since(day0).filter { it.pkg == pkg }
        return AppDay(
            ms = Usage.sessions(context, day0, now)[pkg].orEmpty().sumOf { it.ms },
            tries = t.size,
            letGo = t.count { it.outcome == Outcome.LET_GO },
            opens = t.count(::isOpen),
        )
    }

    fun skipsUsedToday(now: Long = System.currentTimeMillis()) =
        InterventionLog.since(Usage.startOfDay(now)).count { it.skipped }

    /** Today's counts from the local log only — fast enough to run before the overlay appears. */
    fun reflect(pkg: String, now: Long = System.currentTimeMillis()): ReflectInfo {
        val all = InterventionLog.since(Usage.startOfDay(now))
        val mine = all.filter { it.pkg == pkg }
        return ReflectInfo(
            triesToday = mine.size,
            letGoTimes = mine.filter { it.outcome == Outcome.LET_GO }.map { it.ts },
            opensToday = mine.count(::isOpen),
            letGoTotalToday = all.count { it.outcome == Outcome.LET_GO },
        )
    }

    /** Today's foreground sessions for [pkg]. Reads usage stats, so call it off the main thread. */
    fun sessionsToday(context: Context, pkg: String, now: Long = System.currentTimeMillis()): List<Session> =
        // The app is technically resumed underneath the pause; that just-started session isn't real use.
        Usage.sessions(context, Usage.startOfDay(now), now)[pkg].orEmpty().filter { it.start < now - 15_000L }

    fun hm(ts: Long): String = Calendar.getInstance().run {
        timeInMillis = ts
        "%02d:%02d".format(get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE))
    }
}
