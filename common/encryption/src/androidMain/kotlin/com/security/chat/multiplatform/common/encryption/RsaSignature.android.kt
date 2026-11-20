package com.security.chat.multiplatform.common.encryption

import java.security.KeyFactory
import java.security.Signature
import java.security.SignatureException
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.PSSParameterSpec
import java.security.spec.X509EncodedKeySpec

internal actual val rsaPss: RsaPss = AndroidRsaPss

private object AndroidRsaPss : RsaPss {

    private val pssParameters = PSSParameterSpec(
        "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1,
    )

    override suspend fun sign(data: ByteArray, privateKey: ByteArray): ByteArray {
        val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(privateKey))
        return newSignature().apply {
            initSign(key)
            update(data)
        }.sign()
    }

    override suspend fun verify(
        data: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray,
    ): Boolean {
        val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(publicKey))
        val verifier = newSignature().apply {
            initVerify(key)
            update(data)
        }
        return try {
            verifier.verify(signature)
        } catch (_: SignatureException) {
            false
        }
    }

    /** Android exposes RSA-PSS through the digest-specific JCA name. */
    private fun newSignature(): Signature {
        return Signature.getInstance("SHA256withRSA/PSS").apply { setParameter(pssParameters) }
    }
}
