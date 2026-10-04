package com.pausespace.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

/**
 * Settings shared by the app UI and the accessibility service (same process).
 * Persisted as one JSON blob; everything stays on the device.
 */
object Store {
    private const val KEY = "prefs_v1"
    private lateinit var sp: SharedPreferences
    private val state = MutableStateFlow(Prefs())
    val prefs: StateFlow<Prefs> = state.asStateFlow()

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = context.getSharedPreferences("pausespace", Context.MODE_PRIVATE)
        state.value = sp.getString(KEY, null)?.let { runCatching { decode(JSONObject(it)) }.getOrNull() } ?: Prefs()
    }

    val current: Prefs get() = state.value

    fun edit(change: (Prefs) -> Prefs) {
        state.update(change)
        sp.edit().putString(KEY, encode(state.value).toString()).apply()
    }

    fun editRitual(change: (Ritual) -> Ritual) = edit { it.copy(ritual = change(it.ritual)) }
    fun editWords(change: (Words) -> Words) = edit { it.copy(words = change(it.words)) }
    fun editRule(pkg: String, change: (AppRule) -> AppRule) =
        edit { it.copy(rules = it.rules + (pkg to change(it.rule(pkg)))) }

    private fun encode(p: Prefs) = JSONObject().apply {
        put("onboarded", p.onboarded)
        put("theme", p.theme.name)
        put("ritual", JSONObject().apply {
            put("inhale", p.ritual.inhale)
            put("hold", p.ritual.hold)
            put("exhale", p.ritual.exhale)
            put("rounds", p.ritual.rounds)
            put("preset", p.ritual.preset ?: "")
            put("skips", p.ritual.skipsPerDay)
        })
        put("words", JSONObject().apply {
            put("selected", p.words.selected)
            put("custom", p.words.custom)
            put("useCustom", p.words.useCustom)
            put("shuffle", p.words.shuffle)
        })
        put("rules", JSONObject().apply {
            p.rules.forEach { (pkg, r) ->
                put(pkg, JSONObject().apply {
                    put("paused", r.paused)
                    put("grow", r.grow)
                    put("length", r.length.name)
                    put("checkIn", r.checkIn)
                    put("dailyOpens", r.dailyOpens)
                    put("allDay", r.allDay)
                    put("start", r.startMin)
                    put("end", r.endMin)
                })
            }
        })
    }

    private fun decode(o: JSONObject): Prefs {
        val d = Prefs()
        val r = o.optJSONObject("ritual")
        val w = o.optJSONObject("words")
        val rules = o.optJSONObject("rules")
        return Prefs(
            onboarded = o.optBoolean("onboarded", false),
            theme = runCatching { ThemeMode.valueOf(o.optString("theme")) }.getOrDefault(ThemeMode.SYSTEM),
            ritual = if (r == null) d.ritual else Ritual(
                inhale = r.optInt("inhale", 4),
                hold = r.optInt("hold", 4),
                exhale = r.optInt("exhale", 6),
                rounds = r.optInt("rounds", 2),
                preset = r.optString("preset").ifEmpty { null },
                skipsPerDay = r.optInt("skips", 3),
            ),
            words = if (w == null) d.words else Words(
                selected = w.optInt("selected", 0),
                custom = w.optString("custom", ""),
                useCustom = w.optBoolean("useCustom", false),
                shuffle = w.optBoolean("shuffle", false),
            ),
            rules = buildMap {
                rules?.keys()?.forEach { pkg ->
                    val x = rules.getJSONObject(pkg)
                    put(pkg, AppRule(
                        paused = x.optBoolean("paused", false),
                        grow = x.optBoolean("grow", true),
                        length = runCatching { SessionLength.valueOf(x.optString("length")) }.getOrDefault(SessionLength.ASK),
                        checkIn = x.optBoolean("checkIn", true),
                        dailyOpens = x.optInt("dailyOpens", 5),
                        allDay = x.optBoolean("allDay", true),
                        startMin = x.optInt("start", 9 * 60),
                        endMin = x.optInt("end", 17 * 60),
                    ))
                }
            },
        )
    }
}
