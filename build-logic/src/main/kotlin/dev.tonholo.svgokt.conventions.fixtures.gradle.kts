import dev.tonholo.svgokt.conventions.fixtures.FixturesGenerator
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    org.jetbrains.kotlin.multiplatform
}

// Resolve the SVGO fixture directory relative to the root project, since
// svgo-kt vendors the upstream svgo repository at `svgo/` alongside the
// module that consumes its fixtures.
val fixturesDir = rootProject.file("svgo/test/plugins")
val generatedFixturesDir = layout.buildDirectory.dir("generated/fixtures/commonTest/kotlin")

val generatePluginFixtureSources = tasks.register("generatePluginFixtureSources") {
    group = "build"
    description = "Embeds svgo plugin fixtures into a generated Kotlin source file so " +
        "commonTest can run on JVM, JS, and native targets."

    inputs.dir(fixturesDir).withPropertyName("fixturesDir")
    outputs.dir(generatedFixturesDir).withPropertyName("generatedDir")

    doLast {
        FixturesGenerator.write(
            fixturesDir = fixturesDir,
            outputDir = generatedFixturesDir.get().asFile,
        )
    }
}

extensions.configure<KotlinMultiplatformExtension> {
    sourceSets.getByName("commonTest").kotlin.srcDir(generatedFixturesDir)
}

// Force every Kotlin test compilation to wait on fixture generation so the
// embedded source file is always up to date before being compiled.
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    if (name.contains("Test", ignoreCase = true)) {
        dependsOn(generatePluginFixtureSources)
    }
}
