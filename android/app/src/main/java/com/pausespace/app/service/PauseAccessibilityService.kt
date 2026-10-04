package com.pausespace.app.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.mutableStateOf
import com.pausespace.app.data.Apps
import com.pausespace.app.data.InterventionLog
import com.pausespace.app.data.Outcome
import com.pausespace.app.data.Rules
import com.pausespace.app.data.Session
import com.pausespace.app.data.Stats
import com.pausespace.app.data.Store
import com.pausespace.app.data.Usage
import com.pausespace.app.overlay.InterventionModel
import com.pausespace.app.overlay.OverlayController

/**
 * Notices when a paused app comes to the front and puts the breathing pause over it.
 * Only the package and activity class of the foreground window are read — never its content.
 */
class PauseAccessibilityService : AccessibilityService() {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var overlay: OverlayController

    private var foreground: String? = null
    private val ignored = mutableSetOf<String>()
    private val activityCache = HashMap<String, Boolean>()

    /** pkg → time the user's chosen session ends. Long.MAX_VALUE = no limit, ends when they leave. */
    private val allowance = HashMap<String, Long>()
    private val checkIns = HashMap<String, Runnable>()

    private class Active(val id: Long, val pkg: String) {
        /** A choice was made; the overlay is only showing the closing message. */
        var decided = false
    }

    private var active: Active? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Store.init(this)
        InterventionLog.init(this)
        overlay = OverlayController(this)
        ignored += packageName
        // System UI and system dialogs (permission prompts, chooser) sit on top of an app without leaving it.
        ignored += listOf(
            "com.android.systemui",
            "android",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.intentresolver",
        )
        getSystemService(InputMethodManager::class.java)?.enabledInputMethodList?.forEach { ignored += it.packageName }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ignored) return
        // Dialogs, toasts and popups also fire this event; only real activities count as "opening" an app.
        val cls = event.className?.toString() ?: return
        if (!isActivity(pkg, cls)) return
        onForeground(pkg)
    }

    private fun onForeground(pkg: String) {
        val prev = foreground
        foreground = pkg
        if (prev != null && prev != pkg && allowance[prev] == Long.MAX_VALUE) allowance.remove(prev)

        active?.let { a ->
            if (a.pkg != pkg && !a.decided) {
                // Left during the pause (home, recents, another app) — the app never opened.
                InterventionLog.setOutcome(a.id, Outcome.LET_GO)
                dismiss()
            }
        }
        maybePause(pkg)
    }

    private fun maybePause(pkg: String) {
        if (active != null) return
        val prefs = Store.current
        val rule = prefs.rules[pkg] ?: return
        if (!rule.paused || !Rules.inSchedule(rule, Usage.minuteOfDay())) return
        val now = System.currentTimeMillis()
        allowance[pkg]?.let { until -> if (until > now) return else allowance.remove(pkg) }
        show(pkg)
    }

    private fun show(pkg: String) {
        val prefs = Store.current
        val rule = prefs.rule(pkg)
        val id = InterventionLog.start(pkg)
        val reflect = Stats.reflect(pkg)
        val sessions = mutableStateOf(emptyList<Session>())
        val label = Apps.label(this, pkg)
        val model = InterventionModel(
            pkg = pkg,
            label = label,
            letter = label.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "•",
            ritual = prefs.ritual,
            rounds = Rules.rounds(prefs.ritual, rule, reflect.triesToday),
            line = Rules.line(prefs.words),
            skipsTotal = prefs.ritual.skipsPerDay,
            skipsLeft = Rules.skipsLeft(prefs.ritual.skipsPerDay, Stats.skipsUsedToday()),
            length = rule.length,
            opensLeft = Rules.opensLeft(rule, reflect.opensToday),
            dailyOpens = rule.dailyOpens,
            reflect = reflect,
            sessions = sessions,
            now = System.currentTimeMillis(),
        )
        val a = Active(id, pkg)
        active = a
        val app = applicationContext
        Thread {
            val list = Stats.sessionsToday(app, pkg)
            main.post { sessions.value = list }
        }.start()
        overlay.show(
            model,
            onBack = {
                if (a.decided) dismiss() else {
                    InterventionLog.setOutcome(id, Outcome.LET_GO)
                    a.decided = true
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    dismiss()
                }
            },
            onSkip = { InterventionLog.markSkipped(id) },
            onChoose = { outcome, minutes ->
                a.decided = true
                InterventionLog.setOutcome(id, outcome)
                when (outcome) {
                    Outcome.LET_GO -> performGlobalAction(GLOBAL_ACTION_HOME)
                    Outcome.NO_LIMIT -> allowance[pkg] = Long.MAX_VALUE
                    Outcome.OPEN -> allow(pkg, (minutes ?: 5) * 60_000L)
                }
            },
            onFinish = { dismiss() },
        )
    }

    private fun allow(pkg: String, ms: Long) {
        allowance[pkg] = System.currentTimeMillis() + ms
        checkIns.remove(pkg)?.let(main::removeCallbacks)
        val r = Runnable {
            checkIns.remove(pkg)
            allowance.remove(pkg)
            val rule = Store.current.rule(pkg)
            // "Breathe again when time is up": pause again if they're still in the app.
            if (rule.checkIn && foreground == pkg) maybePause(pkg)
        }
        checkIns[pkg] = r
        main.postDelayed(r, ms)
    }

    private fun dismiss() {
        overlay.hide()
        active = null
    }

    private fun isActivity(pkg: String, cls: String): Boolean = activityCache.getOrPut("$pkg/$cls") {
        try {
            packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            // Visible package but not an activity (a dialog or popup) → false. A package we can't see
            // (package visibility) → fall back to excluding framework widget classes.
            if (Apps.isInstalled(this, pkg)) false else !cls.startsWith("android.")
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        if (::overlay.isInitialized) overlay.hide()
        super.onDestroy()
    }
}
