plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.org.jetbrains.kotlinx.benchmark)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.svgoKt)
            implementation(libs.kotlinx.benchmark.runtime)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

benchmark {
    targets {
        register("jvm")
    }

    configurations {
        named("main") {
            warmups = 3
            iterations = 5
            iterationTime = 2
            iterationTimeUnit = "s"
            outputTimeUnit = "ms"
            mode = "avgt"
            reportFormat = "json"
        }

        // A faster sanity-check configuration for local iteration.
        register("smoke") {
            warmups = 1
            iterations = 2
            iterationTime = 1
            iterationTimeUnit = "s"
            outputTimeUnit = "ms"
            mode = "avgt"
            reportFormat = "text"
        }
    }
}
