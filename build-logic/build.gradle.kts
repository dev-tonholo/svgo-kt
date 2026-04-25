plugins {
    `kotlin-dsl`
}

// The `kotlin-dsl` plugin pins the embedded Kotlin compiler to Gradle's bundled
// version (currently 2.0.x). Third-party plugins like
// com.vanniktech:gradle-maven-publish-plugin are compiled with newer Kotlin
// metadata versions. Skip the metadata check so `build-logic` can depend on
// those plugin APIs without downgrading the project.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xskip-metadata-version-check")
}

dependencies {
    // Workaround for consuming the version catalog from precompiled script
    // plugins via the generated `LibrariesForLibs` accessor.
    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.vanniktech.maven.publish.plugin)
    implementation(libs.kover.gradle.plugin)
}
