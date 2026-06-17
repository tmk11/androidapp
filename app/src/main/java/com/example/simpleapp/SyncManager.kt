package com.example.simpleapp

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Pushes local changes to Supabase and pulls remote changes back into the
 * local cache. Runs on a single background thread; results are delivered on
 * the main thread.
 */
class SyncManager(private val db: NailDb, private val prefs: Prefs) {

    interface Listener {
        fun onSyncStart()
        fun onSyncDone(success: Boolean)
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun syncAsync(listener: Listener?) {
        val password = prefs.getPassword() ?: return
        listener?.let { main.post { it.onSyncStart() } }
        executor.execute {
            val success = try {
                doSync(password)
            } catch (e: Exception) {
                false
            }
            listener?.let { main.post { it.onSyncDone(success) } }
        }
    }

    private fun doSync(password: String): Boolean {
        // 1) Push local changes first. Stop on the first failure so a dropped
        //    network simply leaves the change pending for the next attempt.
        for (p in db.pending()) {
            if (p.deleted) {
                if (Api.deleteEntry(password, p.uuid)) db.hardDelete(p.uuid) else return false
            } else {
                val remote = RemoteEntry(p.uuid, p.tech, p.cents, p.day, p.createdAt, p.enteredBy)
                if (Api.addEntry(password, remote)) db.markSynced(p.uuid) else return false
            }
        }

        // 2) Pull the full server state into the local cache.
        val remote = Api.fetchAll(password) ?: return false
        val remoteUuids = HashSet<String>(remote.size * 2)
        for (r in remote) {
            remoteUuids.add(r.uuid)
            db.upsertRemote(r)
        }

        // 3) Anything that was synced locally but is gone on the server was
        //    deleted from another device — remove it here too.
        for (uuid in db.syncedUuids()) {
            if (!remoteUuids.contains(uuid)) db.hardDelete(uuid)
        }
        return true
    }
}
