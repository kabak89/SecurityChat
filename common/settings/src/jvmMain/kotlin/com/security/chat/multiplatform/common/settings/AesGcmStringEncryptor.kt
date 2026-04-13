package com.security.chat.multiplatform.common.settings

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class AesGcmStringEncryptor(
    private val aesKey: ByteArray,
) {

    private val random = SecureRandom()

    fun encrypt(plaintext: String): String {
        val iv = ByteArray(GCM_IV_LENGTH).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(aesKey, AES),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + ciphertext
        return ENCRYPTED_SETTINGS_VALUE_PREFIX + Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(wrapped: String): String {
        require(wrapped.startsWith(ENCRYPTED_SETTINGS_VALUE_PREFIX)) { "Value is not encrypted" }
        val combined =
            Base64.getDecoder().decode(wrapped.removePrefix(ENCRYPTED_SETTINGS_VALUE_PREFIX))
        require(combined.size > GCM_IV_LENGTH) { "Invalid ciphertext" }
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(aesKey, AES),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        val plain = cipher.doFinal(ciphertext)
        return plain.toString(Charsets.UTF_8)
    }
}

private const val AES = "AES"
private const val AES_GCM = "AES/GCM/NoPadding"
private const val ENCRYPTED_SETTINGS_VALUE_PREFIX = "ENC1:"
private const val GCM_IV_LENGTH = 12
private const val GCM_TAG_LENGTH_BITS = 128
