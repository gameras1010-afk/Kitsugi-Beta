package com.kitsugi.animelist.data.manga.engines

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * CryptoPageDecryptor
 *
 * AES-CBC decryption utility for sites like SiyahMelek or others that serve encrypted images.
 */
object CryptoPageDecryptor {
    
    /**
     * Decrypts AES-CBC encrypted data.
     *
     * @param encryptedBase64 The base64-encoded encrypted string.
     * @param key 16, 24, or 32 byte secret key.
     * @param iv 16 byte IV.
     * @return Decrypted byte array (e.g. raw image bytes).
     */
    fun decrypt(encryptedBase64: String, key: ByteArray, iv: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
        return cipher.doFinal(encryptedBytes)
    }

    /**
     * Decrypts AES-CBC encrypted data using String key/iv (convenience helper).
     */
    fun decrypt(encryptedBase64: String, key: String, iv: String): ByteArray {
        return decrypt(encryptedBase64, key.toByteArray(Charsets.UTF_8), iv.toByteArray(Charsets.UTF_8))
    }
}
