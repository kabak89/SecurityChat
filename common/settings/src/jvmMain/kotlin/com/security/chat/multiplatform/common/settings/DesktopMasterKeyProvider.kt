package com.security.chat.multiplatform.common.settings

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import kotlin.io.path.deleteExisting

internal object DesktopMasterKeyProvider {

    private val keyFile: Path =
        Paths.get(System.getProperty("user.home"), ".SecurityChat", ".master-key")

    @Synchronized
    fun getOrCreateAes256Key(): ByteArray {
        Files.createDirectories(keyFile.parent)
        if (Files.exists(keyFile)) {
            val bytes = Files.readAllBytes(keyFile)
            require(bytes.size == AES_KEY_LENGTH_BYTES) {
                "Invalid master key length at $keyFile"
            }
            return bytes
        }
        val key = ByteArray(AES_KEY_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val parent = keyFile.parent
        val tmp = Files.createTempFile(parent, ".master-key-", ".tmp")
        try {
            Files.write(tmp, key, StandardOpenOption.TRUNCATE_EXISTING)
            restrictToOwnerOnly(tmp)
            Files.move(
                tmp,
                keyFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } finally {
            kotlin.runCatching { tmp.deleteExisting() }
        }
        restrictToOwnerOnly(keyFile)
        return key
    }

    private fun restrictToOwnerOnly(path: Path) {
        kotlin.runCatching {
            Files.setPosixFilePermissions(
                path,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                ),
            )
        }
    }
}

private const val AES_KEY_LENGTH_BYTES = 32
