package com.security.chat.multiplatform.features.authorize.data.repoimpl

import com.security.chat.multiplatform.features.user.data.storage.entity.CryptoKeys
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.operations.KeyGenerator
import kotlin.io.encoding.Base64

@OptIn(DelicateCryptographyApi::class)
internal suspend fun getKeysPair(): CryptoKeys {
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