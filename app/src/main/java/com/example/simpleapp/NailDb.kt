package com.example.simpleapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

/** A revenue entry shown in the UI. [enteredBy] = who/which device recorded it. */
data class Entry(
    val clientUuid: String,
    val tech: String,
    val cents: Long,
    val day: String,
    val createdAt: Long,
    val enteredBy: String?
)

/** A locally-changed entry waiting to be pushed to the server. */
data class PendingEntry(
    val uuid: String,
    val tech: String,
    val cents: Long,
    val day: String,
    val createdAt: Long,
    val enteredBy: String?,
    val deleted: Boolean
)

/**
 * Local SQLite cache (offline-first). Every row carries a stable [client_uuid]
 * used for syncing, a [synced] flag (0 = needs pushing), a [deleted] flag (soft
 * delete) and [entered_by] (who recorded it).
 */
class NailDb(context: Context) : SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE entries(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "client_uuid TEXT NOT NULL, " +
                "tech TEXT NOT NULL, " +
                "amount_cents INTEGER NOT NULL, " +
                "day TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "entered_by TEXT, " +
                "synced INTEGER NOT NULL DEFAULT 0, " +
                "deleted INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE UNIQUE INDEX idx_entries_uuid ON entries(client_uuid)")
        db.execSQL("CREATE INDEX idx_entries_day ON entries(day)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE entries ADD COLUMN client_uuid TEXT")
            db.execSQL("ALTER TABLE entries ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE entries ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE entries SET client_uuid = lower(hex(randomblob(16))) WHERE client_uuid IS NULL")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_entries_uuid ON entries(client_uuid)")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE entries ADD COLUMN entered_by TEXT")
        }
    }

    // --- Local edits ---

    fun addLocal(tech: String, cents: Long, day: String, enteredBy: String?) {
        val values = ContentValues().apply {
            put("client_uuid", UUID.randomUUID().toString())
            put("tech", tech)
            put("amount_cents", cents)
            put("day", day)
            put("created_at", System.currentTimeMillis())
            put("entered_by", enteredBy)
            put("synced", 0)
            put("deleted", 0)
        }
        writableDatabase.insert("entries", null, values)
    }

    fun softDelete(uuid: String) {
        writableDatabase.execSQL(
            "UPDATE entries SET deleted=1, synced=0 WHERE client_uuid=?",
            arrayOf(uuid)
        )
    }

    // --- Reads for the UI (visible rows only) ---

    fun entriesForTechDay(tech: String, day: String): List<Entry> {
        val list = ArrayList<Entry>()
        readableDatabase.rawQuery(
            "SELECT client_uuid, tech, amount_cents, day, created_at, entered_by FROM entries " +
                "WHERE tech=? AND day=? AND deleted=0 ORDER BY created_at ASC",
            arrayOf(tech, day)
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Entry(c.getString(0), c.getString(1), c.getLong(2), c.getString(3), c.getLong(4), c.getString(5))
                )
            }
        }
        return list
    }

    fun totalsByTechLike(pattern: String): Map<String, Long> {
        val map = HashMap<String, Long>()
        readableDatabase.rawQuery(
            "SELECT tech, SUM(amount_cents) FROM entries WHERE day LIKE ? AND deleted=0 GROUP BY tech",
            arrayOf(pattern)
        ).use { c ->
            while (c.moveToNext()) map[c.getString(0)] = c.getLong(1)
        }
        return map
    }

    fun totalsByTechAll(): Map<String, Long> {
        val map = HashMap<String, Long>()
        readableDatabase.rawQuery(
            "SELECT tech, SUM(amount_cents) FROM entries WHERE deleted=0 GROUP BY tech",
            null
        ).use { c ->
            while (c.moveToNext()) map[c.getString(0)] = c.getLong(1)
        }
        return map
    }

    // --- Sync support ---

    fun pending(): List<PendingEntry> {
        val list = ArrayList<PendingEntry>()
        readableDatabase.rawQuery(
            "SELECT client_uuid, tech, amount_cents, day, created_at, entered_by, deleted FROM entries " +
                "WHERE synced=0 ORDER BY created_at ASC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    PendingEntry(
                        c.getString(0), c.getString(1), c.getLong(2),
                        c.getString(3), c.getLong(4), c.getString(5), c.getInt(6) == 1
                    )
                )
            }
        }
        return list
    }

    fun markSynced(uuid: String) {
        writableDatabase.execSQL("UPDATE entries SET synced=1 WHERE client_uuid=?", arrayOf(uuid))
    }

    fun hardDelete(uuid: String) {
        writableDatabase.delete("entries", "client_uuid=?", arrayOf(uuid))
    }

    fun upsertRemote(r: RemoteEntry) {
        writableDatabase.execSQL(
            "INSERT INTO entries(client_uuid, tech, amount_cents, day, created_at, entered_by, synced, deleted) " +
                "VALUES(?,?,?,?,?,?,1,0) " +
                "ON CONFLICT(client_uuid) DO UPDATE SET " +
                "tech=excluded.tech, amount_cents=excluded.amount_cents, day=excluded.day, " +
                "created_at=excluded.created_at, entered_by=excluded.entered_by, synced=1, deleted=0",
            arrayOf(r.uuid, r.tech, r.cents, r.day, r.createdAt, r.enteredBy)
        )
    }

    /** UUIDs of locally-visible, already-synced rows (used to detect remote deletes). */
    fun syncedUuids(): Set<String> {
        val set = HashSet<String>()
        readableDatabase.rawQuery(
            "SELECT client_uuid FROM entries WHERE synced=1 AND deleted=0",
            null
        ).use { c ->
            while (c.moveToNext()) set.add(c.getString(0))
        }
        return set
    }

    companion object {
        private const val DB_NAME = "nail_revenue.db"
        private const val DB_VERSION = 3
        val TECHS = listOf("Ha", "David", "Tu", "Anh")
    }
}
