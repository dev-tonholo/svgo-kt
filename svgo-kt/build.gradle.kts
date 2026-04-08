import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.io.gitlab.arturbosch.detekt)
}

kotlin {
    createSvgoKtNativePlatforms().forEach { target ->
        target.binaries {
            sharedLib {
                baseName = "svgo-kt"
            }
        }
    }

    createJsPlatform(moduleName = "svgo")
    createJvmPlatform()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kss.core)
            implementation(libs.kss.lexer)
            implementation(libs.kss.parser)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

detekt {
    autoCorrect = true
    buildUponDefaultConfig = true // preconfigure defaults
    allRules = false // activate all available (even unstable) rules.
    // point to your custom config defining rules to run, overwriting default behavior
    config.setFrom("${rootProject.rootDir}/config/detekt.yml")
    baseline = file("detekt-baseline.xml")
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = JavaVersion.VERSION_11.toString()
    exclude {
        it.file.absolutePath.contains("build/")
    }
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}

tasks.named("detekt") {
    dependsOn("detektMetadataCommonMain")
}
tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = JavaVersion.VERSION_11.toString()
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}
