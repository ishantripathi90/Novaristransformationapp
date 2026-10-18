package com.novaris.transformationlab

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

data class Account(val name: String, val email: String, val company: String, val token: String)

class AuthFailure(message: String, val status: Int = 0) : IOException(message)

/** No passwords or hard-coded credentials are ever stored in the APK. */
class AuthService {
    private val endpoint = BuildConfig.BASE_URL.trimEnd('/') + "/novaris-app-api.php"

    suspend fun register(name: String, email: String, password: String, company: String): Account {
        val reply = request("POST", "register", null,
            JSONObject().put("name", name).put("email", email).put("password", password).put("company", company))
        return accountFrom(reply)
    }
    suspend fun signIn(email: String, password: String): Account {
        val reply = request("POST", "login", null,
            JSONObject().put("email", email).put("password", password))
        return accountFrom(reply)
    }
    suspend fun verify(account: Account): Account {
        val json = request("GET", "me", account.token, null)
        val u = json.getJSONObject("user")
        return Account(u.optString("name"), u.optString("email"), u.optString("company"), account.token)
    }
    suspend fun signOut(token: String) { request("POST", "logout", token, JSONObject()) }

    private fun accountFrom(json: JSONObject): Account {
        val u = json.getJSONObject("user")
        return Account(u.getString("name"), u.getString("email"), u.optString("company"), json.getString("token"))
    }

    private suspend fun request(method: String, action: String, token: String?, body: JSONObject?): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL("$endpoint?action=$action").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 12000
            connection.readTimeout = 18000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = try { JSONObject(response) } catch (_: Exception) {
                throw AuthFailure("Account service isn't ready. Upload SERVER_UPLOAD to your hosting first.", code)
            }
            if (code !in 200..299 || !json.optBoolean("ok")) {
                throw AuthFailure(json.optString("error", "Unable to connect. Please try again."), code)
            }
            json
        } catch (e: AuthFailure) {
            throw e
        } catch (e: Exception) {
            throw AuthFailure("Can't reach Novaris right now. Check your internet and the website setup.")
        } finally {
            connection.disconnect()
        }
    }
}

/** AES-GCM encryption using a non-exportable Android Keystore key (Android 8+). */
class SessionVault(context: Context) {
    private val prefs = context.getSharedPreferences("novaris_secure_session", Context.MODE_PRIVATE)
    private val alias = "NovarisSessionKeyV1"

    private fun secretKey(): java.security.Key {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getKey(alias, null)
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }
    fun save(account: Account) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val plaintext = JSONObject().put("name", account.name).put("email", account.email)
            .put("company", account.company).put("token", account.token).toString().toByteArray(StandardCharsets.UTF_8)
        val encoded = Base64.encodeToString(cipher.iv + cipher.doFinal(plaintext), Base64.NO_WRAP)
        prefs.edit().putString("session", encoded).apply()
    }
    fun load(): Account? = try {
        val encoded = prefs.getString("session", null) ?: return null
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        val data = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), StandardCharsets.UTF_8))
        Account(data.getString("name"), data.getString("email"), data.optString("company"), data.getString("token"))
    } catch (_: Exception) {
        clear(); null
    }
    fun clear() { prefs.edit().remove("session").apply() }
}

/** This records only tests opened, not answers or an invented 'completed' status. */
class RecentTests(context: Context) {
    private val prefs = context.getSharedPreferences("novaris_recent", Context.MODE_PRIVATE)
    fun get(): List<String> = prefs.getString("keys", "")!!.split(',').filter { key -> ASSESSMENTS.any { it.key == key } }
    fun add(key: String) {
        val updated = (listOf(key) + get().filterNot { it == key }).take(8)
        prefs.edit().putString("keys", updated.joinToString(",")).apply()
    }
}
