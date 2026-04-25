import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("dev.tonholo.svgokt.conventions.common")
    org.jetbrains.kotlin.multiplatform
}

kotlin {
    val nativeTargets: List<KotlinNativeTarget> = listOf(
        macosArm64(),
        macosX64(),
        linuxX64(),
        mingwX64(),
    )
    nativeTargets.forEach { target ->
        target.binaries {
            sharedLib {
                baseName = "svgo-kt"
            }
        }
    }

    js {
        outputModuleName.set("svgo")
        binaries.executable()
        nodejs()
        browser {
            commonWebpackConfig {
                outputFileName = "svgo.js"
            }
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("svgo-wasm")
        binaries.executable()
        nodejs()
        browser {
            commonWebpackConfig {
                outputFileName = "svgo-wasm.js"
            }
        }
    }

    jvm()
}
