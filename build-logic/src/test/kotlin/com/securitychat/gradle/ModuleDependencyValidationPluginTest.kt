package com.securitychat.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class ModuleDependencyValidationPluginTest {

    @TempDir
    lateinit var projectDir: Path

    @ParameterizedTest
    @ValueSource(
        strings = [
            "commonMainApi", "commonMainImplementation", "androidMainImplementation",
            "iosMainImplementation", "jvmMainImplementation", "commonTestImplementation",
            "jvmTestImplementation",
        ],
    )
    fun `forbidden dependencies fail during configuration for every source set`(
        configuration: String,
    ) {
        createBuild(
            sourcePath = ":features:chat:chat-domain",
            targetPaths = listOf(":features:profile:profile-domain"),
            script = """
                configurations { $configuration }
                dependencies {
                    $configuration(project(':features:profile:profile-domain'))
                }
            """.trimIndent(),
        )

        assertViolation(
            result = runner().buildAndFail(),
            configuration = configuration,
            sourcePath = ":features:chat:chat-domain",
            targetPath = ":features:profile:profile-domain",
        )
    }

    @Test
    fun `type safe project accessors are checked`() {
        createBuild(
            sourcePath = ":features:chat:chat-domain",
            targetPaths = listOf(":features:profile:profile-domain"),
            script = """
                val commonMainApi by configurations.creating
                dependencies {
                    add(commonMainApi.name, projects.features.profile.profileDomain)
                }
            """.trimIndent(),
            kotlin = true,
        )

        assertViolation(
            result = runner().buildAndFail(),
            configuration = "commonMainApi",
            sourcePath = ":features:chat:chat-domain",
            targetPath = ":features:profile:profile-domain",
        )
    }

    @Test
    fun `unused lazy configurations and provider backed dependencies are checked`() {
        createBuild(
            sourcePath = ":features:chat:chat-domain",
            targetPaths = listOf(":features:profile:profile-domain"),
            script = """
                configurations.register('unusedImplementation') {
                    dependencies.addLater(providers.provider {
                        project.dependencies.project(path: ':features:profile:profile-domain')
                    })
                }
            """.trimIndent(),
        )

        assertViolation(
            result = runner().buildAndFail(),
            configuration = "unusedImplementation",
            sourcePath = ":features:chat:chat-domain",
            targetPath = ":features:profile:profile-domain",
        )
    }

    @Test
    fun `configurations and dependencies added by later callbacks are checked`() {
        createBuild(
            sourcePath = ":features:chat:chat-domain",
            targetPaths = listOf(":features:profile:profile-domain"),
            script = """
                afterEvaluate {
                    configurations.create('lateImplementation')
                    dependencies.add(
                        'lateImplementation',
                        project(':features:profile:profile-domain')
                    )
                }
            """.trimIndent(),
        )

        assertViolation(
            result = runner().buildAndFail(),
            configuration = "lateImplementation",
            sourcePath = ":features:chat:chat-domain",
            targetPath = ":features:profile:profile-domain",
        )
    }

    @Test
    fun `allowed dependencies are cached without resolution and new violations are rejected`() {
        val script = createBuild(
            sourcePath = ":features:chat:chat-data",
            targetPaths = listOf(
                ":common:core-domain",
                ":features:chat:chat-domain",
                ":features:profile:profile-domain",
                ":features:profile:profile-data-network",
                ":features:profile:profile-data-storage",
                ":features:profile:profile-data-common",
                ":features:profile:profile-ui",
            ),
            script = """
                configurations { commonMainImplementation }
                dependencies {
                    commonMainImplementation(project(':common:core-domain'))
                    commonMainImplementation(project(':features:chat:chat-domain'))
                    commonMainImplementation(project(':features:profile:profile-domain'))
                    commonMainImplementation(project(':features:profile:profile-data-network'))
                    commonMainImplementation(project(':features:profile:profile-data-storage'))
                    commonMainImplementation(project(':features:profile:profile-data-common'))
                    commonMainImplementation('example:unused:1.0')
                }
            """.trimIndent(),
        )

        runner().build()
        val cached = runner().build()
        assertTrue(cached.output.contains("Configuration cache entry reused."), cached.output)

        script.appendText(
            """

                dependencies {
                    commonMainImplementation(project(':features:profile:profile-ui'))
                }
            """.trimIndent(),
        )
        assertViolation(
            result = runner().buildAndFail(),
            configuration = "commonMainImplementation",
            sourcePath = ":features:chat:chat-data",
            targetPath = ":features:profile:profile-ui",
        )
    }

    @Test
    fun `platform specific module keeps its special outgoing dependencies`() {
        createBuild(
            sourcePath = ":common:platform-specific",
            targetPaths = listOf(":features:root:root-ui", ":features:root:root-component"),
            script = """
                configurations { androidMainImplementation }
                dependencies {
                    androidMainImplementation(project(':features:root:root-ui'))
                    androidMainImplementation(project(':features:root:root-component'))
                }
            """.trimIndent(),
        )

        runner().build()
    }

    @Test
    fun `unknown source layers are rejected even without dependencies`() {
        createBuild(
            sourcePath = ":features:chat:chat-custom",
            targetPaths = emptyList(),
            script = "",
        )

        val result = runner().buildAndFail()
        assertTrue(result.output.contains("Unknown module layer for :features:chat:chat-custom"))
    }

    @Test
    fun `unknown target layers include the dependency context in the error`() {
        createBuild(
            sourcePath = ":features:chat:chat-component",
            targetPaths = listOf(":features:other:other-custom"),
            script = """
                configurations { commonMainImplementation }
                dependencies {
                    commonMainImplementation(project(':features:other:other-custom'))
                }
            """.trimIndent(),
        )

        assertViolation(
            result = runner().buildAndFail(),
            configuration = "commonMainImplementation",
            sourcePath = ":features:chat:chat-component",
            targetPath = ":features:other:other-custom",
        )
    }

    private fun createBuild(
        sourcePath: String,
        targetPaths: List<String>,
        script: String,
        kotlin: Boolean = false,
    ): Path {
        val paths = (targetPaths + sourcePath).distinct()
        projectDir.resolve("settings.gradle").writeText(
            "rootProject.name = 'module-dependency-rules'\n" +
                    (if (kotlin) "enableFeaturePreview('TYPESAFE_PROJECT_ACCESSORS')\n" else "") +
                    paths.joinToString(separator = "\n", postfix = "\n") { "include '$it'" },
        )
        projectDir.resolve("gradle.properties").writeText(
            "org.gradle.configuration-cache.parallel=true\norg.gradle.parallel=true\n",
        )
        for (path in paths) {
            val directory = projectDir.resolve(path.removePrefix(":").replace(':', '/'))
            directory.createDirectories()
            if (path != sourcePath) directory.resolve("build.gradle").writeText("")
        }
        val sourceDirectory = projectDir.resolve(sourcePath.removePrefix(":").replace(':', '/'))
        val buildFile = sourceDirectory.resolve(if (kotlin) "build.gradle.kts" else "build.gradle")
        val plugins = if (kotlin) {
            "plugins { id(\"securitychat.convention.dependencies\") }"
        } else {
            "plugins { id 'securitychat.convention.dependencies' }"
        }
        buildFile.writeText("$plugins\n$script\n")
        return buildFile
    }

    private fun runner(): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments(
                listOf(
                    "help", "--configuration-cache", "--offline", "--console=plain",
                    "-Dorg.gradle.jvmargs=-Xmx512m",
                ),
            )

    private fun assertViolation(
        result: BuildResult,
        configuration: String,
        sourcePath: String,
        targetPath: String,
    ) {
        for (expected in listOf(
            "Forbidden module dependency:", sourcePath, targetPath,
            "Configuration: $configuration", "Allowed target layers:",
        )) {
            assertTrue(result.output.contains(expected), "Missing '$expected':\n${result.output}")
        }
    }
}
