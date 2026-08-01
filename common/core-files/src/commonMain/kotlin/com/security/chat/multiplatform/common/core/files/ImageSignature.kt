package com.security.chat.multiplatform.common.core.files

/** Number of leading bytes required to recognize any of the signatures below. */
internal const val IMAGE_HEADER_SIZE: Int = 12

/**
 * Recognizes an image by its magic bytes instead of its name: desktop pickers cannot reliably
 * restrict the selection to images, so any file may otherwise end up sent as a chat image.
 */
internal fun ByteArray.hasImageSignature(): Boolean {
    return hasRenderableImageSignature() || isHeif()
}

/**
 * Tells whether every platform can draw the image as is. HEIF cannot: a message is rendered on
 * all of them, and Skia has no decoder for it on desktop, so such an image has to be transcoded
 * before it is sent.
 */
internal fun ByteArray.hasRenderableImageSignature(): Boolean {
    return startsWith(JPEG_SIGNATURE) ||
            startsWith(PNG_SIGNATURE) ||
            startsWith(GIF_SIGNATURE) ||
            startsWith(BMP_SIGNATURE) ||
            isWebP()
}

private fun ByteArray.isWebP(): Boolean {
    return startsWith(RIFF_SIGNATURE) &&
            startsWith(WEBP_SIGNATURE, offset = WEBP_SIGNATURE_OFFSET)
}

/**
 * HEIF and AVIF share the ISO base media container with video, so the brand that follows the
 * `ftyp` box is what tells an image apart from a clip.
 */
private fun ByteArray.isHeif(): Boolean {
    if (size < BRAND_OFFSET + BRAND_SIZE) return false
    if (!startsWith(FTYP_SIGNATURE, offset = FTYP_SIGNATURE_OFFSET)) return false

    val brand = decodeToString(
        startIndex = BRAND_OFFSET,
        endIndex = BRAND_OFFSET + BRAND_SIZE,
    )
    return brand in HEIF_IMAGE_BRANDS
}

private fun ByteArray.startsWith(signature: ByteArray, offset: Int = 0): Boolean {
    if (size < offset + signature.size) return false
    return signature.indices.all { index -> this[offset + index] == signature[index] }
}

private const val WEBP_SIGNATURE_OFFSET = 8
private const val FTYP_SIGNATURE_OFFSET = 4
private const val BRAND_OFFSET = 8
private const val BRAND_SIZE = 4

private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
private val GIF_SIGNATURE = byteArrayOf(0x47, 0x49, 0x46, 0x38)
private val BMP_SIGNATURE = byteArrayOf(0x42, 0x4D)
private val RIFF_SIGNATURE = byteArrayOf(0x52, 0x49, 0x46, 0x46)
private val WEBP_SIGNATURE = byteArrayOf(0x57, 0x45, 0x42, 0x50)
private val FTYP_SIGNATURE = byteArrayOf(0x66, 0x74, 0x79, 0x70)

private val HEIF_IMAGE_BRANDS = setOf(
    "heic", "heix", "heim", "heis",
    "hevc", "hevx", "hevm", "hevs",
    "mif1", "msf1",
    "avif", "avis",
)
