plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
}

kotlin {
    createSvgoKtNativePlatforms().forEach { target ->
        target.binaries {
            executable {
                entryPoint = "main"
                baseName = "svgo-kt-sample"
                debuggable = true
            }
        }
    }

    createJsPlatform("svgo-sample")

    createJvmPlatform()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.svgoKt)
        }
        nativeMain.dependencies {  }
        jsMain.dependencies {
            implementation(libs.kotlinx.coroutines.js)
            implementation(npm("svgo", "3.2.0"))
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.jvm)
        }
    }
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec> {
        download = false
    }
    // "true" for default behavior
}
