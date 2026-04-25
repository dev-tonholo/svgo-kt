plugins {
    org.jetbrains.kotlinx.kover
}

// Kover instruments the JVM tests (which include the full 363-fixture
// upstream parity suite plus the per-plugin commonTest unit tests). The HTML
// and XML reports land under `build/reports/kover/` after running
// `./gradlew :svgo-kt:koverHtmlReport` or `:svgo-kt:koverXmlReport`.
//
// Generated and build artifacts are excluded so coverage reflects only the
// hand-written sources we actually maintain.
kover {
    reports {
        filters {
            excludes {
                packages(
                    // Build-generated fixture map -- not application code.
                    "svgokt.integration",
                )
                classes(
                    // Generated GeneratedFixtures.kt
                    "svgokt.integration.GeneratedFixturesKt",
                )
            }
        }

        verify {
            rule {
                groupBy = kotlinx.kover.gradle.plugin.dsl.GroupingEntityType.APPLICATION
                bound {
                    minValue = 0 // Ratchet upward over time.
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                }
            }
        }
    }
}
