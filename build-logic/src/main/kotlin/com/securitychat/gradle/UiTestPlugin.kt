package com.securitychat.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestOutputListener
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import java.io.Serializable

internal const val UI_TEST_TASK_NAME = "uiTest"
internal const val UI_TEST_TAG = "ui"

class UiTestPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val jvmTest = target.tasks.named<Test>("jvmTest")

        target.tasks.register<Test>(UI_TEST_TASK_NAME) {
            group = "verification"
            description = "Runs UI scenarios tagged with '$UI_TEST_TAG'"
            testClassesDirs = jvmTest.get().testClassesDirs
            classpath = jvmTest.get().classpath
            javaLauncher.set(jvmTest.flatMap { it.javaLauncher })

            useJUnitPlatform {
                includeTags(UI_TEST_TAG)
                excludeTags("screenshot")
            }

            addTestOutputListener(VideoLinkOutputListener())

            outputs.dir(target.layout.buildDirectory.dir("reports/ui-tests"))
                .withPropertyName("uiTestArtifacts")
        }
    }
}

private class VideoLinkOutputListener : TestOutputListener, Serializable {
    override fun onOutput(testDescriptor: TestDescriptor, outputEvent: TestOutputEvent) {
        if (outputEvent.destination != TestOutputEvent.Destination.StdOut) return

        outputEvent.message.lineSequence()
            .filter { it.startsWith("Video: file:///") }
            .forEach { Logging.getLogger(VideoLinkOutputListener::class.java).lifecycle(it) }
    }
}
