package com.securitychat.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import javax.inject.Inject

abstract class PrepareScreenshotsTask : DefaultTask() {
    @get:OutputDirectory
    abstract val screenshotsDir: DirectoryProperty

    @get:OutputDirectory
    abstract val diffsDir: DirectoryProperty

    @get:Inject
    protected abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun run() {
        listOf(screenshotsDir, diffsDir).forEach { dirProperty ->
            val dir = dirProperty.get().asFile
            if (dir.exists()) {
                fileSystemOperations.delete { delete(dir) }
            }
            dir.mkdirs()
        }
    }
}

class ScreenshotTestPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val screenshotsDir = target.layout.projectDirectory.dir("src/jvmTest/resources/screenshots")
        val diffsDir = target.layout.buildDirectory.dir("reports/screenshots/diffs")

        val recordTask = target.tasks.register<PrepareScreenshotsTask>("recordScreenshots") {
            group = "verification"
            description = "Records new screenshots for all UI components"
            this.screenshotsDir.set(screenshotsDir)
            this.diffsDir.set(diffsDir)
            outputs.upToDateWhen { false } // Always run to ensure clean state
            finalizedBy("jvmTest")
        }

        target.tasks.register("checkScreenshots") {
            group = "verification"
            description = "Checks screenshots for all UI components"
            dependsOn("jvmTest")
        }

        target.tasks.named<ProcessResources>("jvmTestProcessResources") {
            mustRunAfter(recordTask)
        }

        target.tasks.withType<Test>().configureEach {
            val isRecord = target.hasProperty("screenshot.record") ||
                    target.gradle.startParameter.taskNames.any { it.contains("recordScreenshots") }
            val isCheck = target.hasProperty("screenshot.check") ||
                    target.gradle.startParameter.taskNames.any { it.contains("checkScreenshots") }
            val isScreenshotRun = isRecord || isCheck

            useJUnitPlatform {
                if (isScreenshotRun) {
                    includeTags("screenshot")
                } else {
                    excludeTags("screenshot")
                }
            }

            systemProperty("screenshot.record", isRecord.toString())

            /**
             * If we are recording or checking screenshots, we want to ensure the task runs
             * and isn't skipped due to configuration cache or up-to-date checks from a normal test run.
             */
            inputs.property("screenshot.record", isRecord)
            inputs.property("screenshot.check", isCheck)

            if (isRecord) {
                // Ensure the task runs when recording, even if nothing changed
                outputs.upToDateWhen { false }
            }

            /** Set a large window size for Compose Desktop tests to avoid clipping */
            systemProperty("compose.tests.width", "2000")
            systemProperty("compose.tests.height", "2000")

            testLogging {
                events("failed")
                exceptionFormat = TestExceptionFormat.FULL
                showExceptions = true
                showCauses = true
                showStackTraces = true
            }

            /** Ensure resources are up to date for tests */
            dependsOn(target.tasks.withType<ProcessResources>())
        }
    }
}
