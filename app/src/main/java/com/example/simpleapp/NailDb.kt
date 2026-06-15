package com.example.simpleapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** A single revenue entry for one technician. */
data class Entry(
    val id: Long,
    val tech: String,
    val cents: Long,
    val day: String,
    val createdAt: Long
)

/** SQLite storage for revenue entries. */
class NailDb(context: Context) : SQLiteOpenHelper(context.applicationContext, DB_NAME, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE entries(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "tech TEXT NOT NULL, " +
                "amount_cents INTEGER NOT NULL, " +
                "day TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX idx_entries_day ON entries(day)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS entries")
        onCreate(db)
    }

    fun addEntry(tech: String, cents: Long, day: String) {
        val values = ContentValues().apply {
            put("tech", tech)
            put("amount_cents", cents)
            put("day", day)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insert("entries", null, values)
    }

    fun deleteEntry(id: Long) {
        writableDatabase.delete("entries", "id=?", arrayOf(id.toString()))
    }

    fun entriesForDay(day: String): List<Entry> {
        val list = ArrayList<Entry>()
        readableDatabase.rawQuery(
            "SELECT id, tech, amount_cents, day, created_at FROM entries " +
                "WHERE day=? ORDER BY created_at DESC",
            arrayOf(day)
        ).use { c ->
            while (c.moveToNext()) {
                list.add(Entry(c.getLong(0), c.getString(1), c.getLong(2), c.getString(3), c.getLong(4)))
            }
        }
        return list
    }

    /** A technician's entries for one day, oldest first (customer #1, #2, ...). */
    fun entriesForTechDay(tech: String, day: String): List<Entry> {
        val list = ArrayList<Entry>()
        readableDatabase.rawQuery(
            "SELECT id, tech, amount_cents, day, created_at FROM entries " +
                "WHERE tech=? AND day=? ORDER BY created_at ASC",
            arrayOf(tech, day)
        ).use { c ->
            while (c.moveToNext()) {
                list.add(Entry(c.getLong(0), c.getString(1), c.getLong(2), c.getString(3), c.getLong(4)))
            }
        }
        return list
    }

    /** Totals (cents) grouped by technician for days matching the LIKE [pattern]. */
    fun totalsByTechLike(pattern: String): Map<String, Long> {
        val map = HashMap<String, Long>()
        readableDatabase.rawQuery(
            "SELECT tech, SUM(amount_cents) FROM entries WHERE day LIKE ? GROUP BY tech",
            arrayOf(pattern)
        ).use { c ->
            while (c.moveToNext()) map[c.getString(0)] = c.getLong(1)
        }
        return map
    }

    /** Totals (cents) grouped by technician across all days. */
    fun totalsByTechAll(): Map<String, Long> {
        val map = HashMap<String, Long>()
        readableDatabase.rawQuery(
            "SELECT tech, SUM(amount_cents) FROM entries GROUP BY tech",
            null
        ).use { c ->
            while (c.moveToNext()) map[c.getString(0)] = c.getLong(1)
        }
        return map
    }

    companion object {
        private const val DB_NAME = "nail_revenue.db"
        val TECHS = listOf("Ha", "David", "Tu", "Anh")
    }
}
