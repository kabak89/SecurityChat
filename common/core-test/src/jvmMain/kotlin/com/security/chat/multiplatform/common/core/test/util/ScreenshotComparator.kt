package com.security.chat.multiplatform.common.core.test.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max

public object ScreenshotComparator {

    /** Tolerance for differences in channels (0-255) */
    private const val THRESHOLD: Int = 5

    public fun compare(expectedFile: File, actualBitmap: ImageBitmap, diffFile: File) {
        val actualImage = actualBitmap.toBufferedImage()
        val expectedImage = ImageIO.read(expectedFile)

        if (expectedImage.width != actualImage.width || expectedImage.height != actualImage.height) {
            saveDiff(expectedImage, actualImage, diffFile)
            val message = "Screenshot size mismatch!\n" +
                    "Expected: ${expectedImage.width}x${expectedImage.height}\n" +
                    "Actual:   ${actualImage.width}x${actualImage.height}\n" +
                    "Diff image saved to: file://${diffFile.absolutePath}"
            println(message)
            throw AssertionError(message)
        }

        var isDifferent = false
        val diffImage =
            BufferedImage(expectedImage.width, expectedImage.height, BufferedImage.TYPE_INT_ARGB)

        for (x in 0 until expectedImage.width) {
            for (y in 0 until expectedImage.height) {
                val expectedRgb = expectedImage.getRGB(x, y)
                val actualRgb = actualImage.getRGB(x, y)
                if (isColorDifferent(expectedRgb, actualRgb)) {
                    isDifferent = true
                    diffImage.setRGB(x, y, Color.RED.rgb)
                } else {
                    diffImage.setRGB(x, y, 0)
                }
            }
        }

        if (isDifferent) {
            saveDiff(expectedImage, actualImage, diffFile, diffImage)
            val message = "Screenshot pixel mismatch!\n" +
                    "Diff image saved to: file://${diffFile.absolutePath}"
            println(message)
            throw AssertionError(message)
        }
    }

    private fun isColorDifferent(rgb1: Int, rgb2: Int): Boolean {
        if (rgb1 == rgb2) return false
        val a1 = (rgb1 shr 24) and 0xff
        val r1 = (rgb1 shr 16) and 0xff
        val g1 = (rgb1 shr 8) and 0xff
        val b1 = rgb1 and 0xff

        val a2 = (rgb2 shr 24) and 0xff
        val r2 = (rgb2 shr 16) and 0xff
        val g2 = (rgb2 shr 8) and 0xff
        val b2 = rgb2 and 0xff

        return abs(a1 - a2) > THRESHOLD ||
                abs(r1 - r2) > THRESHOLD ||
                abs(g1 - g2) > THRESHOLD ||
                abs(b1 - b2) > THRESHOLD
    }

    private fun saveDiff(
        expected: BufferedImage,
        actual: BufferedImage,
        diffFile: File,
        diffOverlay: BufferedImage? = null,
    ) {
        val maxWidth = max(expected.width, actual.width)
        val maxHeight = max(expected.height, actual.height)

        /** Create a canvas for 3 images (Expected | Diff | Actual) */
        val combined = BufferedImage(maxWidth * 3, maxHeight, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = combined.createGraphics()

        /** Draw a checkerboard background for all parts */
        drawCheckerboard(g, maxWidth * 3, maxHeight)

        /** 1. Expected */
        g.drawImage(expected, 0, 0, null)

        /** 2. Diff (Overlay over expected) */
        g.drawImage(expected, maxWidth, 0, null)
        if (diffOverlay != null) {
            g.drawImage(diffOverlay, maxWidth, 0, null)
        } else {
            /** If sizes don't match and there's no overlay, fill the center part with semi-transparent red */
            g.color = Color(255, 0, 0, 100)
            g.fillRect(maxWidth, 0, maxWidth, maxHeight)
        }

        /** 3. Actual */
        g.drawImage(actual, maxWidth * 2, 0, null)

        g.dispose()
        diffFile.parentFile.mkdirs()
        ImageIO.write(combined, "png", diffFile)
    }

    private fun drawCheckerboard(g: Graphics2D, width: Int, height: Int) {
        val tileSize = 8
        val color1 = Color(250, 250, 250)
        val color2 = Color(220, 220, 220)
        for (y in 0 until height step tileSize) {
            for (x in 0 until width step tileSize) {
                g.color = if ((x / tileSize + y / tileSize) % 2 == 0) color1 else color2
                g.fillRect(x, y, tileSize, tileSize)
            }
        }
    }

    private fun ImageBitmap.toBufferedImage(): BufferedImage {
        val skiaBitmap = this.asSkiaBitmap()
        val bytes = Image.makeFromBitmap(skiaBitmap).encodeToData(EncodedImageFormat.PNG)?.bytes
            ?: throw IllegalStateException("Failed to encode bitmap")
        return ImageIO.read(bytes.inputStream())
    }
}
