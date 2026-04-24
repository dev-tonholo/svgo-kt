plugins {
    id("dev.tonholo.svgokt.conventions.common")
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
}

kotlin {
    val nativeTargets = listOf(
        macosArm64(),
        macosX64(),
        linuxX64(),
        mingwX64(),
    )
    nativeTargets.forEach { target ->
        target.binaries {
            executable {
                entryPoint = "main"
                baseName = "svgo-kt-sample"
                debuggable = true
            }
        }
    }

    js {
        outputModuleName.set("svgo-sample")
        binaries.executable()
        nodejs()
        browser {
            commonWebpackConfig {
                outputFileName = "svgo-sample.js"
            }
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.svgoKt)
        }
        nativeMain.dependencies { }
        jsMain.dependencies {
            implementation(libs.kotlinx.coroutines.js)
            implementation(npm("svgo", "3.2.0"))
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.jvm)
        }
    }
}
