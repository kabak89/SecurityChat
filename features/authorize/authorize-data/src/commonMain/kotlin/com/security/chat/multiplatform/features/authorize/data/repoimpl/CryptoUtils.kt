package com.security.chat.multiplatform.features.authorize.data.repoimpl

import com.security.chat.multiplatform.features.user.data.storage.entity.CryptoKeys
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA512
import dev.whyoleg.cryptography.operations.KeyGenerator
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.io.encoding.Base64

@OptIn(DelicateCryptographyApi::class)
internal suspend fun generateKeysPair(): CryptoKeys {
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

internal fun sha256Hash(input: String): String {
    return SHA256().digest(input.encodeToByteArray()).toHexString()
}

internal suspend fun derivePublicKey(privateKey: String): String {
    val provider = CryptographyProvider.Default
    val rsa = provider.get(RSA.OAEP)
    val decodedPrivateKey = rsa.privateKeyDecoder(digest = SHA512).decodeFromByteArray(
        format = RSA.PrivateKey.Format.DER,
        bytes = Base64.decode(privateKey),
    )
    val publicKey = decodedPrivateKey.getPublicKey().encodeToByteArray(RSA.PublicKey.Format.DER)
    return Base64.encode(publicKey)
}