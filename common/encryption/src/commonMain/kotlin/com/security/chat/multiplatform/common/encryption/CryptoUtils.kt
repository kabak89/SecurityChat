package com.security.chat.multiplatform.common.encryption

import com.security.chat.multiplatform.common.encryption.entity.CryptoKeys
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA512
import dev.whyoleg.cryptography.operations.KeyGenerator
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(DelicateCryptographyApi::class, ExperimentalEncodingApi::class)
public suspend fun generateKeysPair(): CryptoKeys {
    val provider = CryptographyProvider.Default
    val rsa = provider.get(RSA.RAW)
    val keyPairGenerator: KeyGenerator<RSA.RAW.KeyPair> = rsa.keyPairGenerator()
    val keyPair = keyPairGenerator.generateKey()
    val publicKey = keyPair.publicKey.encodeToByteArray(RSA.PublicKey.Format.DER)
    val publicString = Base64.encode(publicKey)
    val privateKeyString = keyPair.privateKey.encodeToByteArray(RSA.PrivateKey.Format.DER)
    val privateKey = Base64.encode(privateKeyString)

    return CryptoKeys(
        publicKey = publicString,
        privateKey = privateKey,
    )
}

public fun sha256Hash(input: String): String {
    return SHA256().digest(input.encodeToByteArray()).toHexString()
}

@OptIn(ExperimentalEncodingApi::class)
public suspend fun derivePublicKey(privateKey: String): String {
    val privateKeyBytes = Base64.decode(privateKey)
    val publicKeyBytes = try {
        val provider = CryptographyProvider.Default
        val rsa = provider.get(RSA.OAEP)
        val decodedPrivateKey = rsa.privateKeyDecoder(digest = SHA512).decodeFromByteArray(
            format = RSA.PrivateKey.Format.DER,
            bytes = privateKeyBytes,
        )
        decodedPrivateKey.getPublicKey().encodeToByteArray(RSA.PublicKey.Format.DER)
    } catch (_: Exception) {
        /**
         * Android 8 (Conscrypt) decodes the PKCS#8 key into an [OpenSSLRSAPrivateKey] that does
         * not implement RSAPrivateCrtKey, so the provider cannot expose the public key and throws.
         * The public exponent and modulus are still present in the DER bytes, so we reconstruct
         * the SubjectPublicKeyInfo manually as a platform-independent fallback.
         */
        extractRsaPublicKeyDerFromPkcs8(privateKeyBytes)
    }
    return Base64.encode(publicKeyBytes)
}

/**
 * Reconstructs the RSA public key (SubjectPublicKeyInfo DER) from a PKCS#8 private key DER by
 * extracting the modulus and public exponent from the embedded RSAPrivateKey structure.
 */
private fun extractRsaPublicKeyDerFromPkcs8(pkcs8: ByteArray): ByteArray {
    val reader = DerReader(pkcs8)
    reader.openSequence() // PrivateKeyInfo
    reader.skipTlv() // version
    reader.skipTlv() // privateKeyAlgorithm
    val rsaPrivateKey = reader.readOctetStringContent() // privateKey

    val inner = DerReader(rsaPrivateKey)
    inner.openSequence() // RSAPrivateKey
    inner.skipTlv() // version
    val modulus = inner.readTlv() // INTEGER modulus
    val publicExponent = inner.readTlv() // INTEGER publicExponent

    val rsaPublicKey = derTlv(tag = 0x30, body = modulus + publicExponent)
    val subjectPublicKey = derTlv(tag = 0x03, body = byteArrayOf(0x00) + rsaPublicKey)
    return derTlv(tag = 0x30, body = RSA_ENCRYPTION_ALGORITHM_ID + subjectPublicKey)
}

/** AlgorithmIdentifier for rsaEncryption (OID 1.2.840.113549.1.1.1) with NULL parameters. */
private val RSA_ENCRYPTION_ALGORITHM_ID = byteArrayOf(
    0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(),
    0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00,
)

private fun derTlv(tag: Int, body: ByteArray): ByteArray {
    return byteArrayOf(tag.toByte()) + encodeDerLength(body.size) + body
}

private fun encodeDerLength(length: Int): ByteArray {
    if (length < 0x80) return byteArrayOf(length.toByte())
    val bytes = ArrayList<Byte>()
    var value = length
    while (value > 0) {
        bytes.add(0, (value and 0xFF).toByte())
        value = value ushr 8
    }
    return byteArrayOf((0x80 or bytes.size).toByte()) + bytes.toByteArray()
}

private class DerReader(private val bytes: ByteArray) {
    private var index = 0

    fun openSequence() {
        require((bytes[index++].toInt() and 0xFF) == 0x30) { "Expected DER SEQUENCE" }
        readLength()
    }

    fun skipTlv() {
        index++
        val length = readLength()
        index += length
    }

    fun readTlv(): ByteArray {
        val start = index
        index++
        val length = readLength()
        index += length
        return bytes.copyOfRange(start, index)
    }

    fun readOctetStringContent(): ByteArray {
        require((bytes[index++].toInt() and 0xFF) == 0x04) { "Expected DER OCTET STRING" }
        val length = readLength()
        val content = bytes.copyOfRange(index, index + length)
        index += length
        return content
    }

    private fun readLength(): Int {
        val first = bytes[index++].toInt() and 0xFF
        if (first < 0x80) return first
        var length = 0
        repeat(first and 0x7F) { length = (length shl 8) or (bytes[index++].toInt() and 0xFF) }
        return length
    }
}
