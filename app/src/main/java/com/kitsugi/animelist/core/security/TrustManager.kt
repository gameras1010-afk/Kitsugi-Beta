package com.kitsugi.animelist.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File

object TrustManager {
    private const val TAG = "TrustManager"

    // Keiyoushi and Tachiyomi official signing certificate SHA-256 hashes
    private val trustedSignatures = setOf(
        "55c304f5d95f606028f8bc9d79c8ca7356e9bc6a41b9e5d129994c379a2cf505", // Legacy Tachiyomi Extensions
        "c65538561d2d0b671a5a041f021703cf9203a7a9cc5f6d7be761c566fb5eb4d7"  // Keiyoushi Extensions
    )

    fun getApkSignatureHash(context: Context, file: File): String? {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            
            val packageInfo = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: return null

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) return null

            val signatureBytes = signatures[0].toByteArray()
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signatureBytes)
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting signature: ${e.message}", e)
            null
        }
    }

    fun isSignatureTrusted(context: Context, signatureHash: String): Boolean {
        if (trustedSignatures.contains(signatureHash.lowercase())) {
            return true
        }

        val prefs = context.getSharedPreferences("security_trust", Context.MODE_PRIVATE)
        val userTrusted = prefs.getStringSet("trusted_signatures", emptySet()) ?: emptySet()
        return userTrusted.contains(signatureHash.lowercase())
    }

    fun trustSignature(context: Context, signatureHash: String) {
        val prefs = context.getSharedPreferences("security_trust", Context.MODE_PRIVATE)
        val userTrusted = (prefs.getStringSet("trusted_signatures", emptySet()) ?: emptySet()).toMutableSet()
        userTrusted.add(signatureHash.lowercase())
        prefs.edit().putStringSet("trusted_signatures", userTrusted).apply()
    }
}
