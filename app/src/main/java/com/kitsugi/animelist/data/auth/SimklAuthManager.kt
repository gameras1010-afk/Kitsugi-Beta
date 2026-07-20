package com.kitsugi.animelist.data.auth

import android.content.Context
import android.util.Log
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * SIMKL Entegrasyon – TV/CLI PIN Akışı
 *
 * @deprecated T4-04: Bu sınıf [ExternalAuthManager] ile duplike işlevsellik içerir.
 *   Yeni kod `ExternalAuthManager.authorizeSimkl()` kullanmalıdır.
 *   Bu dosya yalnızca geriye dönük uyumluluk ve referans için tutulmaktadır.
 *
 * SIMKL OAuth PIN Flow: https://api.simkl.org/api-reference/oauth
 *
 * Akış:
 *  1. requestDeviceCode()  → pin code + verification URL al
 *  2. pollForToken()       → kullanıcı onayana kadar yokla (polling)
 *  3. Token elde edilince  → SettingsDataStore'a kaydet
 *  4. getUserProfile()     → profil bilgilerini çek ve yansıt
 */
@Deprecated(
    message = "Use ExternalAuthManager for Simkl authentication. SimklAuthManager duplicates logic from ExternalAuthManager.",
    replaceWith = ReplaceWith("ExternalAuthManager", "com.kitsugi.animelist.data.auth.ExternalAuthManager")
)
class SimklAuthManager(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    companion object {
        private const val TAG = "SimklAuthManager"
        // T3-01: BuildConfig'den alınır — local.properties'te `simkl_client_id=` ile override edilebilir
        private val CLIENT_ID get() = com.kitsugi.animelist.BuildConfig.SIMKL_CLIENT_ID
        private const val BASE_URL = "https://api.simkl.com"
        private const val POLL_INTERVAL_MS = 5000L
        private const val MAX_POLL_DURATION_MS = 5 * 60 * 1000L  // 5 dakika
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ── 1. Cihaz kodu / PIN al ───────────────────────────────────────────────

    /**
     * SIMKL'den PIN kodu ve doğrulama URL'si alır.
     * Kullanıcı bu PIN'i verification_url'e giderek onaylar.
     *
     * @return DeviceCodeResult veya null (ağ hatası)
     */
    suspend fun requestDeviceCode(): DeviceCodeResult? = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("client_id", CLIENT_ID)
            .put("redirect_uri", "urn:ietf:wg:oauth:2.0:oob")
            .toString()

        val request = Request.Builder()
            .url("$BASE_URL/oauth/pin")
            .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
            .header("Content-Type", "application/json")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "requestDeviceCode: HTTP ${response.code}")
                    return@withContext null
                }
                val body = JSONObject(response.body?.string().orEmpty())
                DeviceCodeResult(
                    userCode        = body.optString("user_code"),
                    verificationUrl = body.optString("verification_url",
                        "https://simkl.com/pin/${body.optString("user_code")}"),
                    expiresIn       = body.optInt("expires_in", 600),
                    interval        = body.optInt("interval", 5)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "requestDeviceCode failed", e)
            null
        }
    }

    // ── 2. Token için yokla ──────────────────────────────────────────────────

    /**
     * Kullanıcı PIN'i onaylayana kadar periyodik olarak SIMKL'i yoklar.
     * Onay alındığında token'ı SettingsDataStore'a kaydeder.
     *
     * @param userCode  requestDeviceCode()'dan gelen user_code
     * @param onSuccess Token başarıyla alındığında çağrılır
     * @param onError   Hata veya timeout durumunda çağrılır
     */
    suspend fun pollForToken(
        userCode: String,
        onSuccess: (token: String) -> Unit,
        onError: (reason: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var polled = 0

        Log.i(TAG, "pollForToken: started for code=$userCode")

        while (System.currentTimeMillis() - startTime < MAX_POLL_DURATION_MS) {
            delay(POLL_INTERVAL_MS)
            polled++

            val result = checkPinStatus(userCode)

            when (result) {
                is PinCheckResult.Success -> {
                    Log.i(TAG, "pollForToken: token received after $polled polls")
                    saveToken(result.token)
                    fetchAndSaveProfile(result.token)
                    withContext(Dispatchers.Main) { onSuccess(result.token) }
                    return@withContext
                }
                is PinCheckResult.Pending -> {
                    Log.d(TAG, "pollForToken: pending (poll #$polled)")
                    // Devam et
                }
                is PinCheckResult.Expired -> {
                    Log.w(TAG, "pollForToken: code expired")
                    withContext(Dispatchers.Main) { onError("Kod süresi doldu. Lütfen tekrar bağlanın.") }
                    return@withContext
                }
                is PinCheckResult.Error -> {
                    Log.e(TAG, "pollForToken: error — ${result.message}")
                    withContext(Dispatchers.Main) { onError(result.message) }
                    return@withContext
                }
            }
        }

        // Timeout
        Log.w(TAG, "pollForToken: timed out after $MAX_POLL_DURATION_MS ms")
        withContext(Dispatchers.Main) { onError("Doğrulama zaman aşımına uğradı.") }
    }

    private suspend fun checkPinStatus(userCode: String): PinCheckResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/oauth/pin/$userCode?client_id=$CLIENT_ID")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                val body = runCatching { JSONObject(bodyStr) }.getOrNull()

                when {
                    response.code == 200 && body != null -> {
                        val token = body.optString("access_token", "")
                        if (token.isNotBlank()) PinCheckResult.Success(token)
                        else PinCheckResult.Pending
                    }
                    response.code == 400 -> PinCheckResult.Pending    // henüz onaylanmadı
                    response.code == 404 -> PinCheckResult.Expired    // kod geçersiz/süresi doldu
                    else -> PinCheckResult.Error("HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            PinCheckResult.Error(e.message ?: "Ağ hatası")
        }
    }

    // ── 3. Token Kaydetme / Silme ────────────────────────────────────────────

    private suspend fun saveToken(token: String) {
        settingsDataStore.setSimklProfileInfo(
            username = "",      // profil çekildikten sonra güncellenir
            profileImageUri = "",
            bannerImageUri = ""
        )
        context.getSharedPreferences("MyWebViewPrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("simkl_access_token", token)
            .apply()
        Log.i(TAG, "saveToken: token saved in MyWebViewPrefs")
    }

    /**
     * Kayıtlı SIMKL access token'ı döner. Yoksa null.
     */
    fun getStoredToken(): String? {
        return context.getSharedPreferences("MyWebViewPrefs", Context.MODE_PRIVATE)
            .getString("simkl_access_token", null)
            ?.takeIf { it.isNotBlank() }
    }

    /**
     * Token ve profil bilgilerini temizler (disconnect).
     */
    suspend fun disconnect() {
        context.getSharedPreferences("MyWebViewPrefs", Context.MODE_PRIVATE)
            .edit()
            .remove("simkl_access_token")
            .apply()
        settingsDataStore.clearSimklProfileInfo()
        Log.i(TAG, "disconnect: token cleared")
    }

    /**
     * Bağlı olup olmadığını kontrol eder.
     */
    fun isConnected(): Boolean = !getStoredToken().isNullOrBlank()

    // ── 4. Profil çekme ──────────────────────────────────────────────────────

    private suspend fun fetchAndSaveProfile(token: String) {
        try {
            val profileJson = com.kitsugi.animelist.data.remote.SimklApiClient()
                .getUserProfile(token) ?: return

            val userObj = profileJson.optJSONObject("user") ?: profileJson
            val username = userObj.optString("username", "")
            val avatar   = userObj.optString("avatar", "")

            settingsDataStore.setSimklProfileInfo(
                username        = username,
                profileImageUri = if (avatar.isNotEmpty()) "https://simkl.in/avatars/${avatar}_m.jpg" else "",
                bannerImageUri  = ""
            )
            Log.i(TAG, "fetchAndSaveProfile: saved username=$username")
        } catch (e: Exception) {
            Log.e(TAG, "fetchAndSaveProfile failed", e)
        }
    }

    // ── Sealed types ─────────────────────────────────────────────────────────

    data class DeviceCodeResult(
        val userCode: String,
        val verificationUrl: String,
        val expiresIn: Int,
        val interval: Int
    )

    sealed class PinCheckResult {
        data class Success(val token: String) : PinCheckResult()
        data object Pending : PinCheckResult()
        data object Expired : PinCheckResult()
        data class Error(val message: String) : PinCheckResult()
    }
}
