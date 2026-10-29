package com.security.chat.multiplatform.testutil

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.security.chat.multiplatform.applifecycle.AppLifecycleChanger
import com.security.chat.multiplatform.common.core.component.SCOPE_ID_APP
import com.security.chat.multiplatform.common.core.db.NotSecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.settings.EncryptedSettings
import com.security.chat.multiplatform.common.settings.PublicSettings
import com.security.chat.multiplatform.di.initDI
import com.security.chat.multiplatform.fake.MemorySettings
import com.security.chat.multiplatform.features.push.domain.PushRepository
import com.security.chat.multiplatform.features.root.component.RootComponentImpl
import com.security.chat.multiplatform.features.root.ui.screens.root.RootContent
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
internal fun runAppUiTest(
    testModules: List<Module>,
    videoName: String? = null,
    block: suspend SkikoComposeUiTest.() -> Unit,
) {
    val dispatcher = StandardTestDispatcher()
    val lifecycle = LifecycleRegistry()
    val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
    var appScope: CoroutineScope? = null
    var recordedVideo: File? = null
    var testFailure: Throwable? = null
    val pushRepository = mock<PushRepository> {
        everySuspend { registerCurrentToken() } returns Unit
    }
    val testModule = module {
        single<EncryptedSettings> { MemorySettings() }
        single<PublicSettings> { MemorySettings() }
        single<SecuredDatabaseDriverFactory> { mock() }
        single<NotSecuredDatabaseDriverFactory> { mock() }
        single<DispatcherProviderInterface> {
            object : DispatcherProviderInterface {
                override val Default = dispatcher
                override val IO = dispatcher
                override val Unconfined = dispatcher
                override val Main = Dispatchers.Main
            }
        }
        single<PushRepository> { pushRepository }
    }

    Dispatchers.setMain(dispatcher)
    try {
        initDI()
        val koin = GlobalContext.get()
        koin.loadModules(listOf(testModule) + testModules)
        appScope = koin.get(named(SCOPE_ID_APP))
        runSkikoComposeUiTest(
            size = Size(width = 412f, height = 900f),
            effectContext = dispatcher,
            runTestContext = StandardTestDispatcher(TestCoroutineScheduler()),
            testTimeout = 30.seconds,
        ) {
            lateinit var rootComponent: RootComponentImpl
            runOnUiThread {
                rootComponent = RootComponentImpl(
                    onCreate = { koin.get<AppLifecycleChanger>().onAppStarted() },
                    initialDeepLink = null,
                    componentContext = DefaultComponentContext(lifecycle),
                )
                lifecycle.resume()
            }

            try {
                setContent { RootContent(rootComponent) }
                if (videoName == null) {
                    block()
                } else {
                    recordVideo(
                        name = videoName,
                        onVideoRecorded = { recordedVideo = it },
                        block = block,
                    )
                }
            } finally {
                runOnUiThread { lifecycle.destroy() }
            }
        }
    } catch (exception: Throwable) {
        testFailure = exception
        throw exception
    } finally {
        val cleanupFailures = listOfNotNull(
            runCatching { appScope?.cancel() }.exceptionOrNull(),
            runCatching { stopKoin() }.exceptionOrNull(),
            runCatching { Dispatchers.resetMain() }.exceptionOrNull(),
            runCatching {
                Thread.setDefaultUncaughtExceptionHandler(defaultExceptionHandler)
            }.exceptionOrNull(),
            runCatching {
                recordedVideo?.let { println("Video: ${it.toPath().toUri()}") }
            }.exceptionOrNull(),
        )
        val primaryFailure = testFailure ?: cleanupFailures.firstOrNull()
        cleanupFailures.forEach { cleanupFailure ->
            if (primaryFailure !== cleanupFailure) {
                primaryFailure?.addSuppressed(cleanupFailure)
            }
        }
        if (testFailure == null && primaryFailure != null) {
            throw primaryFailure
        }
    }
}

@OptIn(ExperimentalTestApi::class)
internal fun SkikoComposeUiTest.saveScreenshot(name: String) {
    val screenshot = onRoot().captureToImage()
    Image.makeFromBitmap(screenshot.asSkiaBitmap()).use { image ->
        val output = File("build/reports/ui-tests/$name.png")
        output.parentFile.mkdirs()
        checkNotNull(image.encodeToData(EncodedImageFormat.PNG)).use { data ->
            output.writeBytes(data.bytes)
        }
    }
}
