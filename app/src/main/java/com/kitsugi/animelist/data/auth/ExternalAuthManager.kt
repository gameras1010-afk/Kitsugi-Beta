package com.kitsugi.animelist.data.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject

object ExternalAuthManager {
    private const val PREFS_NAME = "MyWebViewPrefs"

    // ── T3-01: Kimlik bilgileri BuildConfig üzerinden gelir ──────────────────
    // Kaynak: local.properties → app/build.gradle.kts → BuildConfig
    // local.properties (git-ignored) yoksa build.gradle.kts'teki developer
    // fallback değerleri kullanılır; CI'da CI secret'lar geçirilir.
    private val ANILIST_CLIENT_ID    get() = com.kitsugi.animelist.BuildConfig.ANILIST_CLIENT_ID
    private val ANILIST_CLIENT_SECRET get() = com.kitsugi.animelist.BuildConfig.ANILIST_CLIENT_SECRET
    private const val KEY_ANILIST_TOKEN = "anilist_access_token"
    private const val KEY_ANILIST_CODE_VERIFIER = "anilist_code_verifier"

    private val MAL_CLIENT_ID get() = com.kitsugi.animelist.BuildConfig.MAL_CLIENT_ID
    private const val KEY_MAL_TOKEN = "mal_access_token"
    private const val KEY_MAL_REFRESH_TOKEN = "mal_refresh_token"
    private const val KEY_MAL_TOKEN_EXPIRES_AT = "mal_token_expires_at"
    private const val KEY_MAL_CODE_VERIFIER = "mal_code_verifier"

    private val SIMKL_CLIENT_ID    get() = com.kitsugi.animelist.BuildConfig.SIMKL_CLIENT_ID
    private val SIMKL_CLIENT_SECRET get() = com.kitsugi.animelist.BuildConfig.SIMKL_CLIENT_SECRET
    private const val KEY_SIMKL_TOKEN = "simkl_access_token"
    private const val KEY_SIMKL_CODE_VERIFIER = "simkl_code_verifier"

    private const val REDIRECT_URI = "malapp://auth"

    private val _authEvents = MutableSharedFlow<AuthEvent>(
        extraBufferCapacity = 4
    )

    val authEvents = _authEvents.asSharedFlow()

    data class AuthState(
        val isAniListConnected: Boolean,
        val isMalConnected: Boolean,
        val isSimklConnected: Boolean
    )

    sealed class AuthEvent {
        data class Success(
            val serviceName: String
        ) : AuthEvent()

        data class Error(
            val message: String
        ) : AuthEvent()

        data class SessionExpired(
            val serviceName: String
        ) : AuthEvent()
    }

    fun getAuthState(context: Context): AuthState {
        val prefs = prefs(context)

        return AuthState(
            isAniListConnected = prefs.getString(KEY_ANILIST_TOKEN, null) != null,
            isMalConnected = prefs.getString(KEY_MAL_TOKEN, null) != null,
            isSimklConnected = prefs.getString(KEY_SIMKL_TOKEN, null) != null
        )
    }

    fun getAniListToken(context: Context): String? {
        return prefs(context).getString(KEY_ANILIST_TOKEN, null)
    }

    fun getMalToken(context: Context): String? {
        return prefs(context).getString(KEY_MAL_TOKEN, null)
    }

    fun getSimklToken(context: Context): String? {
        return prefs(context).getString(KEY_SIMKL_TOKEN, null)
    }

    fun startAuthentication(
        context: Context,
        serviceName: String,
        onError: (String) -> Unit
    ) {
        try {
            val sharedPreferences = prefs(context)

            when (serviceName) {
                "anilist" -> {
                    val codeVerifier = generateCodeVerifier()
                    val codeChallenge = generateCodeChallengeS256(codeVerifier)
                    sharedPreferences.edit()
                        .putString(KEY_ANILIST_CODE_VERIFIER, codeVerifier)
                        .remove(KEY_MAL_CODE_VERIFIER)
                        .remove(KEY_SIMKL_CODE_VERIFIER)
                        .apply()
                    val authUrl = "https://anilist.co/api/v2/oauth/authorize" +
                            "?client_id=$ANILIST_CLIENT_ID" +
                            "&redirect_uri=$REDIRECT_URI" +
                            "&response_type=code" +
                            "&code_challenge=$codeChallenge" +
                            "&code_challenge_method=S256"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                "mal" -> {
                    val codeVerifier = generateCodeVerifier()
                    sharedPreferences.edit()
                        .putString(KEY_MAL_CODE_VERIFIER, codeVerifier)
                        .remove(KEY_ANILIST_CODE_VERIFIER)
                        .remove(KEY_SIMKL_CODE_VERIFIER)
                        .apply()
                    val authUrl = "https://myanimelist.net/v1/oauth2/authorize?" +
                            "response_type=code" +
                            "&client_id=$MAL_CLIENT_ID" +
                            "&code_challenge=$codeVerifier" +
                            "&code_challenge_method=plain" +
                            "&redirect_uri=${URLEncoder.encode(REDIRECT_URI, "UTF-8")}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                "simkl" -> {
                    // Public PKCE flow â€” no client_secret required
                    val codeVerifier = generateCodeVerifier()
                    val codeChallenge = generateCodeChallengeS256(codeVerifier)
                    sharedPreferences.edit()
                        .putString(KEY_SIMKL_CODE_VERIFIER, codeVerifier)
                        .remove(KEY_ANILIST_CODE_VERIFIER)
                        .remove(KEY_MAL_CODE_VERIFIER)
                        .apply()
                    val authUrl = "https://simkl.com/oauth/authorize" +
                            "?response_type=code" +
                            "&client_id=$SIMKL_CLIENT_ID" +
                            "&redirect_uri=${URLEncoder.encode(REDIRECT_URI, "UTF-8")}" +
                            "&code_challenge=$codeChallenge" +
                            "&code_challenge_method=S256"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                else -> onError("Bilinmeyen servis: $serviceName")
            }
        } catch (e: Exception) {
            onError("Yetkilendirme URL oluÅŸturma hatasÄ±: ${e.message}")
        }
    }

    fun handleAuthIntent(
        context: Context,
        intent: Intent?,
        onSuccess: (serviceName: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val uri = intent?.data
        if (intent?.action != Intent.ACTION_VIEW || uri == null ||
            uri.scheme != "malapp" || uri.host != "auth") return

        val code = uri.getQueryParameter("code")
        if (code == null) {
            val error = uri.getQueryParameter("error")
            val message = "Yetkilendirme hatasÄ±: $error"
            onError(message)
            _authEvents.tryEmit(AuthEvent.Error(message))
            return
        }

        val prefs = prefs(context)
        val malVerifier    = prefs.getString(KEY_MAL_CODE_VERIFIER, null)
        val anilistVerifier = prefs.getString(KEY_ANILIST_CODE_VERIFIER, null)
        val simklVerifier  = prefs.getString(KEY_SIMKL_CODE_VERIFIER, null)

        when {
            simklVerifier != null -> exchangeSimklCodeForToken(
                context = context, code = code, codeVerifier = simklVerifier,
                onSuccess = {
                    onSuccess("simkl")
                    _authEvents.tryEmit(AuthEvent.Success("simkl"))
                },
                onError = { msg ->
                    onError(msg)
                    _authEvents.tryEmit(AuthEvent.Error(msg))
                }
            )
            malVerifier != null -> exchangeMalCodeForToken(
                context = context, code = code, codeVerifier = malVerifier,
                onSuccess = {
                    onSuccess("mal")
                    _authEvents.tryEmit(AuthEvent.Success("mal"))
                },
                onError = { msg ->
                    onError(msg)
                    _authEvents.tryEmit(AuthEvent.Error(msg))
                }
            )
            anilistVerifier != null -> exchangeAniListCodeForToken(
                context = context, code = code, codeVerifier = anilistVerifier,
                onSuccess = {
                    onSuccess("anilist")
                    _authEvents.tryEmit(AuthEvent.Success("anilist"))
                },
                onError = { msg ->
                    onError(msg)
                    _authEvents.tryEmit(AuthEvent.Error(msg))
                }
            )
            else -> {
                val message = "Kod alÄ±ndÄ± ancak servis iÃ§in code_verifier bulunamadÄ±."
                onError(message)
                _authEvents.tryEmit(AuthEvent.Error(message))
            }
        }
    }

    private fun exchangeSimklCodeForToken(
        context: Context,
        code: String,
        codeVerifier: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        thread {
            try {
                val jsonPayload = """{"code":"$code","client_id":"$SIMKL_CLIENT_ID","code_verifier":"$codeVerifier","redirect_uri":"$REDIRECT_URI","grant_type":"authorization_code"}"""
                val request = Request.Builder()
                    .url("https://api.simkl.com/oauth/token")
                    .post(jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build()
                com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val accessToken = JSONObject(response.body?.string().orEmpty()).getString("access_token")
                        prefs(context).edit()
                            .putString(KEY_SIMKL_TOKEN, accessToken)
                            .remove(KEY_SIMKL_CODE_VERIFIER)
                            .remove("simkl_session_expired")
                            .apply()
                        onSuccess()
                    } else {
                        prefs(context).edit().remove(KEY_SIMKL_CODE_VERIFIER).apply()
                        onError("Simkl token deÄŸiÅŸimi baÅŸarÄ±sÄ±z: ${response.code} - ${response.body?.string().orEmpty()}")
                    }
                }
            } catch (e: Exception) {
                prefs(context).edit().remove(KEY_SIMKL_CODE_VERIFIER).apply()
                onError("Simkl token exception: ${e.message}")
            }
        }
    }

    fun disconnectAccount(
        context: Context,
        serviceName: String
    ) {
        val keyToRemove = when (serviceName) {
            "anilist" -> KEY_ANILIST_TOKEN
            "simkl" -> KEY_SIMKL_TOKEN
            else -> KEY_MAL_TOKEN
        }

        val editor = prefs(context).edit().remove(keyToRemove)
        if (serviceName == "simkl") {
            editor.remove("simkl_session_expired")
        }
        editor.apply()
    }

    private fun exchangeAniListCodeForToken(
        context: Context,
        code: String,
        codeVerifier: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        thread {
            try {
                val jsonPayload = """
                    {
                        "grant_type": "authorization_code",
                        "client_id": "$ANILIST_CLIENT_ID",
                        "client_secret": "$ANILIST_CLIENT_SECRET",
                        "redirect_uri": "$REDIRECT_URI",
                        "code": "$code",
                        "code_verifier": "$codeVerifier"
                    }
                """.trimIndent()

                val request = Request.Builder()
                    .url("https://anilist.co/api/v2/oauth/token")
                    .post(jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build()

                com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseText = response.body?.string() ?: ""
                        val accessToken = JSONObject(responseText).getString("access_token")

                        prefs(context).edit()
                            .putString(KEY_ANILIST_TOKEN, accessToken)
                            .remove(KEY_ANILIST_CODE_VERIFIER)
                            .apply()

                        onSuccess()
                    } else {
                        val errorResponse = response.body?.string().orEmpty()

                        prefs(context).edit()
                            .remove(KEY_ANILIST_CODE_VERIFIER)
                            .apply()

                        onError("AniList token deÄŸiÅŸimi baÅŸarÄ±sÄ±z: ${response.code} - $errorResponse")
                    }
                }
            } catch (e: Exception) {
                prefs(context).edit()
                    .remove(KEY_ANILIST_CODE_VERIFIER)
                    .apply()

                onError("AniList token deÄŸiÅŸimi exception: ${e.message}")
            }
        }
    }

    private fun exchangeMalCodeForToken(
        context: Context,
        code: String,
        codeVerifier: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        thread {
            try {
                val postData =
                    "client_id=$MAL_CLIENT_ID" +
                            "&code=$code" +
                            "&code_verifier=$codeVerifier" +
                            "&grant_type=authorization_code" +
                            "&redirect_uri=${URLEncoder.encode(REDIRECT_URI, "UTF-8")}"

                val request = Request.Builder()
                    .url("https://myanimelist.net/v1/oauth2/token")
                    .post(postData.toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaTypeOrNull()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build()

                com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseText = response.body?.string() ?: ""
                        val jsonObj = JSONObject(responseText)
                        val accessToken = jsonObj.getString("access_token")
                        val refreshToken = jsonObj.optString("refresh_token", "")
                        val expiresIn = jsonObj.optLong("expires_in", 2419200L)
                        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

                        prefs(context).edit()
                            .putString(KEY_MAL_TOKEN, accessToken)
                            .putString(KEY_MAL_REFRESH_TOKEN, refreshToken)
                            .putLong(KEY_MAL_TOKEN_EXPIRES_AT, expiresAt)
                            .remove(KEY_MAL_CODE_VERIFIER)
                            .apply()

                        onSuccess()
                    } else {
                        val errorResponse = response.body?.string().orEmpty()

                        prefs(context).edit()
                            .remove(KEY_MAL_CODE_VERIFIER)
                            .apply()

                        onError("MAL token deÄŸiÅŸimi baÅŸarÄ±sÄ±z: ${response.code} - $errorResponse")
                    }
                }
            } catch (e: Exception) {
                prefs(context).edit()
                    .remove(KEY_MAL_CODE_VERIFIER)
                    .apply()

                onError("MAL token deÄŸiÅŸimi exception: ${e.message}")
            }
        }
    }

    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val codeVerifier = ByteArray(32)
        secureRandom.nextBytes(codeVerifier)
        return Base64.encodeToString(
            codeVerifier,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun generateCodeChallengeS256(
        verifier: String
    ): String {
        val bytes = verifier.toByteArray(StandardCharsets.US_ASCII)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    @Synchronized
    fun refreshMalTokenSync(context: Context): String? {
        val prefs = prefs(context)
        val refreshToken = prefs.getString(KEY_MAL_REFRESH_TOKEN, null) ?: return null

        try {
            val postData =
                "client_id=$MAL_CLIENT_ID" +
                "&refresh_token=$refreshToken" +
                "&grant_type=refresh_token"

            val request = Request.Builder()
                .url("https://myanimelist.net/v1/oauth2/token")
                .post(postData.toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaTypeOrNull()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseText = response.body?.string() ?: ""
                    val jsonObj = JSONObject(responseText)
                    val newAccessToken = jsonObj.getString("access_token")
                    val newRefreshToken = jsonObj.optString("refresh_token", refreshToken)
                    val expiresIn = jsonObj.optLong("expires_in", 2419200L)
                    val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)

                    prefs.edit()
                        .putString(KEY_MAL_TOKEN, newAccessToken)
                        .putString(KEY_MAL_REFRESH_TOKEN, newRefreshToken)
                        .putLong(KEY_MAL_TOKEN_EXPIRES_AT, expiresAt)
                        .apply()

                    return newAccessToken
                } else {
                    if (response.code == 400 || response.code == 401) {
                        prefs.edit()
                            .remove(KEY_MAL_TOKEN)
                            .remove(KEY_MAL_REFRESH_TOKEN)
                            .remove(KEY_MAL_TOKEN_EXPIRES_AT)
                            .apply()
                    }
                    return null
                }
            }
        } catch (e: Exception) {
            // T3-05: printStackTrace → Log.e
            android.util.Log.e("ExternalAuthManager", "refreshMalTokenSync failed: ${e.message}", e)
            return null
        }
    }

    suspend fun getOrRefreshMalToken(context: Context): String? = withContext(Dispatchers.IO) {
        val prefs = prefs(context)
        val token = prefs.getString(KEY_MAL_TOKEN, null) ?: return@withContext null
        val expiresAt = prefs.getLong(KEY_MAL_TOKEN_EXPIRES_AT, 0L)

        if (System.currentTimeMillis() + 60_000 >= expiresAt) {
            refreshMalTokenSync(context)
        } else {
            token
        }
    }

    suspend fun getSimklPinSuspend(context: Context): Pair<String, String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.simkl.com/oauth/pin?client_id=$SIMKL_CLIENT_ID")
            .header("Accept", "application/json")
            .build()
        com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Simkl PIN alÄ±namadÄ±: ${response.code}")
            val json = JSONObject(response.body?.string().orEmpty())
            val userCode = json.getString("user_code")
            val verificationUrl = json.getString("verification_url")
            userCode to verificationUrl
        }
    }

    /**
     * Polls Simkl once for a token. Returns true if the token was acquired and
     * saved, false if the user hasn't approved yet (400/404). Throws on real errors.
     */
    suspend fun pollSimklTokenSuspend(context: Context, userCode: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.simkl.com/oauth/pin/$userCode?client_id=$SIMKL_CLIENT_ID")
            .header("Accept", "application/json")
            .build()
        com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
            when {
                response.isSuccessful -> {
                    val json = JSONObject(response.body?.string().orEmpty())
                    if (json.has("access_token")) {
                        prefs(context).edit()
                            .putString(KEY_SIMKL_TOKEN, json.getString("access_token"))
                            .remove("simkl_session_expired")
                            .apply()
                        true
                    } else false
                }
                response.code == 400 || response.code == 404 -> false  // Not yet authorized
                else -> throw Exception("Simkl polling HTTP ${response.code}")
            }
        }
    }

    // Legacy callback wrappers kept for any remaining call-sites
    fun getSimklPin(
        context: Context,
        onSuccess: (userCode: String, verificationUrl: String) -> Unit,
        onError: (String) -> Unit
    ) {
        thread {
            try {
                val request = Request.Builder()
                    .url("https://api.simkl.com/oauth/pin?client_id=$SIMKL_CLIENT_ID")
                    .header("Accept", "application/json")
                    .build()
                com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string().orEmpty())
                        onSuccess(json.getString("user_code"), json.getString("verification_url"))
                    } else onError("Simkl PIN alÄ±namadÄ±: ${response.code}")
                }
            } catch (e: Exception) { onError("Simkl PIN hatasÄ±: ${e.message}") }
        }
    }

    fun pollSimklToken(
        context: Context,
        userCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        thread {
            try {
                val request = Request.Builder()
                    .url("https://api.simkl.com/oauth/pin/$userCode?client_id=$SIMKL_CLIENT_ID")
                    .header("Accept", "application/json")
                    .build()
                com.kitsugi.animelist.core.network.KitsugiHttpClient.client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string().orEmpty())
                        if (json.has("access_token")) {
                            prefs(context).edit()
                                .putString(KEY_SIMKL_TOKEN, json.getString("access_token"))
                                .remove("simkl_session_expired")
                                .apply()
                            onSuccess()
                        } else onError("Token bulunamadÄ±.")
                    } else onError("DoÄŸrulama henÃ¼z tamamlanmadÄ±: ${response.code}")
                }
            } catch (e: Exception) { onError("DoÄŸrulama hatasÄ±: ${e.message}") }
        }
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    fun handleSimkl401(context: Context) {
        disconnectAccount(context, "simkl")
        val settings = com.kitsugi.animelist.data.settings.SettingsDataStore(context)
        CoroutineScope(Dispatchers.IO).launch {
            settings.clearSimklProfileInfo()
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("simkl_session_expired", true)
            .apply()
        _authEvents.tryEmit(AuthEvent.SessionExpired("simkl"))
    }
}