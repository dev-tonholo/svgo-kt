import dev.tonholo.svgokt.conventions.libs
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    io.gitlab.arturbosch.detekt
}

detekt {
    autoCorrect = true
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("${rootProject.rootDir}/config/detekt.yml")
    baseline = file("detekt-baseline.xml")
}

dependencies {
    detektPlugins(libs.detekt.formatting)
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

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = JavaVersion.VERSION_11.toString()
}

// Match the original build's dependency so `./gradlew detekt` still triggers
// the shared metadata task graph when running from any module.
afterEvaluate {
    tasks.findByName("detekt")?.dependsOn(
        tasks.withType(Detekt::class.java).matching { it.name == "detektMetadataCommonMain" },
    )
}
