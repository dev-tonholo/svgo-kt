rootProject.name = "build-logic"

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    // Workaround so convention plugin scripts can access the same version
    // catalog as the main build.
    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
