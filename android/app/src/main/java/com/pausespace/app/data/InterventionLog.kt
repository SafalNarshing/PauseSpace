package com.pausespace.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Every breathing pause, and what came after it. Local SQLite only. */
object InterventionLog {
    private lateinit var db: Helper
    private val changes = MutableStateFlow(0)

    /** Bumps on every write so screens can reload. */
    val version: StateFlow<Int> = changes.asStateFlow()

    fun init(context: Context) {
        if (!::db.isInitialized) db = Helper(context.applicationContext)
    }

    fun start(pkg: String, ts: Long = System.currentTimeMillis()): Long {
        val id = db.writableDatabase.insert("interventions", null, ContentValues().apply {
            put("ts", ts)
            put("pkg", pkg)
            put("skipped", 0)
        })
        changes.value++
        return id
    }

    fun setOutcome(id: Long, outcome: Outcome) {
        db.writableDatabase.update(
            "interventions",
            ContentValues().apply { put("outcome", outcome.key) },
            "id = ? AND outcome IS NULL",
            arrayOf(id.toString()),
        )
        changes.value++
    }

    fun markSkipped(id: Long) {
        db.writableDatabase.update(
            "interventions",
            ContentValues().apply { put("skipped", 1) },
            "id = ?",
            arrayOf(id.toString()),
        )
        changes.value++
    }

    fun since(fromTs: Long): List<Intervention> {
        val out = mutableListOf<Intervention>()
        db.readableDatabase.rawQuery(
            "SELECT id, ts, pkg, outcome, skipped FROM interventions WHERE ts >= ? ORDER BY ts",
            arrayOf(fromTs.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                out += Intervention(
                    id = c.getLong(0),
                    ts = c.getLong(1),
                    pkg = c.getString(2),
                    outcome = Outcome.of(if (c.isNull(3)) null else c.getString(3)),
                    skipped = c.getInt(4) == 1,
                )
            }
        }
        return out
    }

    private class Helper(context: Context) : SQLiteOpenHelper(context, "pausespace.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE interventions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, pkg TEXT NOT NULL, " +
                    "outcome TEXT, skipped INTEGER NOT NULL DEFAULT 0)",
            )
            db.execSQL("CREATE INDEX idx_interventions_ts ON interventions(ts)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
}
