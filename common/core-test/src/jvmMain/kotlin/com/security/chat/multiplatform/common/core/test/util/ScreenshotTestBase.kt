package com.security.chat.multiplatform.common.core.test.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("screenshot")
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
public abstract class ScreenshotTestBase {

    @BeforeEach
    public fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    public fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Flag indicating whether the test should record new reference screenshots */
    private val isRecordMode: Boolean =
        System.getProperty("screenshot.record")?.toBoolean() ?: false

    private val screenshotsDir: File = File("src/jvmTest/resources/screenshots")
    private val diffsDir: File = File("build/reports/screenshots/diffs")

    public fun runScreenshotTest(
        screenshotName: String,
        width: Dp = 412.dp,
        height: Dp = 900.dp,
        content: @Composable () -> Unit,
    ) {
        runComposeUiTest {
            setContent {
                Box(
                    modifier = Modifier
                        .size(width, height)
                        .testTag(SCREENSHOT_TAG),
                ) {
                    content()
                }
            }

            val actualBitmap = onNodeWithTag(SCREENSHOT_TAG).captureToImage()
            val expectedFile = File(screenshotsDir, "$screenshotName.png")
            val diffFile = File(diffsDir, "$screenshotName.png")

            if (isRecordMode) {
                /** RECORD MODE: Only save the reference image */
                saveReference(actualBitmap, expectedFile)
            } else {
                /** VERIFY MODE: Compare actual result with the reference */
                verifyScreenshot(actualBitmap, expectedFile, diffFile)
            }
        }
    }

    private fun saveReference(bitmap: ImageBitmap, file: File) {
        file.parentFile.mkdirs()
        saveImageToFile(bitmap, file)
        println("Reference recorded: file://${file.absolutePath}")
    }

    private fun verifyScreenshot(actualBitmap: ImageBitmap, expectedFile: File, diffFile: File) {
        if (!expectedFile.exists()) {
            throw AssertionError(
                "Reference screenshot not found: file://${expectedFile.absolutePath}.\n" +
                        "Run :recordScreenshots to generate it.",
            )
        }
        ScreenshotComparator.compare(expectedFile, actualBitmap, diffFile)
    }

    private fun saveImageToFile(bitmap: ImageBitmap, file: File) {
        val skiaBitmap = bitmap.asSkiaBitmap()
        val bytes = Image.makeFromBitmap(skiaBitmap).encodeToData(EncodedImageFormat.PNG)?.bytes
            ?: throw IllegalStateException("Failed to encode bitmap")
        file.writeBytes(bytes)
    }
}

private const val SCREENSHOT_TAG: String = "screenshot_container"
