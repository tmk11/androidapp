package com.example.simpleapp

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** A revenue entry as stored on the server. */
data class RemoteEntry(
    val uuid: String,
    val tech: String,
    val cents: Long,
    val day: String,
    val createdAt: Long,
    val enteredBy: String?
)

/**
 * Thin client over the Supabase RPC endpoints. Every call carries the shared
 * password, which the server-side functions verify (the tables themselves are
 * locked by row-level security). All methods are blocking and must be called
 * off the main thread.
 */
object Api {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private class Resp(val code: Int, val body: String?)

    private fun rpc(fn: String, body: JSONObject): Resp {
        val request = Request.Builder()
            .url("${Backend.URL}/rest/v1/rpc/$fn")
            .addHeader("apikey", Backend.ANON_KEY)
            .addHeader("Authorization", "Bearer ${Backend.ANON_KEY}")
            .post(body.toString().toRequestBody(jsonType))
            .build()
        client.newCall(request).execute().use { resp ->
            return Resp(resp.code, resp.body?.string())
        }
    }

    private fun ok(code: Int) = code in 200..299

    /** null = network error; true/false = whether a password already exists. */
    fun isPasswordSet(): Boolean? = try {
        val r = rpc("app_is_password_set", JSONObject())
        if (ok(r.code)) r.body?.trim().equals("true", ignoreCase = true) else null
    } catch (e: Exception) {
        null
    }

    /** Sets the password if none exists yet. null = network error. */
    fun initPassword(password: String): Boolean? = try {
        val r = rpc("app_init_password", JSONObject().put("p_new", password))
        if (ok(r.code)) r.body?.trim().equals("true", ignoreCase = true) else null
    } catch (e: Exception) {
        null
    }

    /** Verifies the password. null = network error. */
    fun checkPassword(password: String): Boolean? = try {
        val r = rpc("app_check_password", JSONObject().put("p_password", password))
        if (ok(r.code)) r.body?.trim().equals("true", ignoreCase = true) else null
    } catch (e: Exception) {
        null
    }

    fun addEntry(password: String, e: RemoteEntry): Boolean = try {
        val body = JSONObject()
            .put("p_password", password)
            .put("p_client_uuid", e.uuid)
            .put("p_tech", e.tech)
            .put("p_cents", e.cents)
            .put("p_day", e.day)
            .put("p_created_at_ms", e.createdAt)
            .put("p_entered_by", e.enteredBy ?: JSONObject.NULL)
        ok(rpc("app_add_entry", body).code)
    } catch (ex: Exception) {
        false
    }

    fun deleteEntry(password: String, uuid: String): Boolean = try {
        val body = JSONObject().put("p_password", password).put("p_client_uuid", uuid)
        ok(rpc("app_delete_entry", body).code)
    } catch (ex: Exception) {
        false
    }

    /** Returns all server entries, or null on network/auth error. */
    fun fetchAll(password: String): List<RemoteEntry>? = try {
        val r = rpc("app_fetch_all", JSONObject().put("p_password", password))
        if (!ok(r.code) || r.body == null) {
            null
        } else {
            val arr = JSONArray(r.body)
            val list = ArrayList<RemoteEntry>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    RemoteEntry(
                        o.getString("client_uuid"),
                        o.getString("tech"),
                        o.getLong("amount_cents"),
                        o.getString("day"),
                        o.getLong("created_at_ms"),
                        if (o.isNull("entered_by")) null else o.getString("entered_by")
                    )
                )
            }
            list
        }
    } catch (e: Exception) {
        null
    }
}
