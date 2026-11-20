package com.security.chat.multiplatform.common.encryption

import dev.whyoleg.cryptography.BinarySize.Companion.bytes
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlin.io.encoding.Base64

public suspend fun signRsa(data: String, privateKey: String): String {
    return Base64.encode(rsaPss.sign(data.encodeToByteArray(), Base64.decode(privateKey)))
}

public suspend fun verifyRsa(data: String, signature: String, publicKey: String): Boolean {
    return rsaPss.verify(
        data = data.encodeToByteArray(),
        signature = Base64.decode(signature),
        publicKey = Base64.decode(publicKey),
    )
}

internal interface RsaPss {
    suspend fun sign(data: ByteArray, privateKey: ByteArray): ByteArray
    suspend fun verify(data: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean
}

internal expect val rsaPss: RsaPss

internal object ProviderRsaPss : RsaPss {

    override suspend fun sign(data: ByteArray, privateKey: ByteArray): ByteArray {
        val key = CryptographyProvider.Default.get(RSA.PSS).privateKeyDecoder(SHA256)
            .decodeFromByteArray(RSA.PrivateKey.Format.DER, privateKey)
        return key.signatureGenerator(saltSize = 32.bytes).generateSignature(data)
    }

    override suspend fun verify(
        data: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray,
    ): Boolean {
        val key = CryptographyProvider.Default.get(RSA.PSS).publicKeyDecoder(SHA256)
            .decodeFromByteArray(RSA.PublicKey.Format.DER, publicKey)
        return key.signatureVerifier(saltSize = 32.bytes).tryVerifySignature(data, signature)
    }
}
