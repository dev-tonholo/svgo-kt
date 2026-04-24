plugins {
    id("dev.tonholo.svgokt.conventions.kmp")
    id("dev.tonholo.svgokt.conventions.fixtures")
    id("dev.tonholo.svgokt.conventions.detekt")
    id("dev.tonholo.svgokt.conventions.publication")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(dependencies.platform(libs.kss.bom))
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

tasks.withType<Test>().configureEach {
    maxHeapSize = "4g"
}
