import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

fun KotlinMultiplatformExtension.createSvgoKtNativePlatforms(): List<KotlinNativeTarget> {
    val macosTargets = listOf(
        macosArm64(),
        macosX64(),
    )
    val linuxTargets = listOf(
        linuxX64(),
    )
    val windowsTargets = listOf(
        mingwX64(),
    )

    return macosTargets + linuxTargets + windowsTargets
}

fun KotlinMultiplatformExtension.createJsPlatform(moduleName: String): KotlinJsTargetDsl = js {
    this.outputModuleName.set(moduleName)
    binaries.executable()
    nodejs()
    browser {
        commonWebpackConfig {
            outputFileName = "$moduleName.js"
        }
    }
}

fun KotlinMultiplatformExtension.createJvmPlatform() = jvm()
