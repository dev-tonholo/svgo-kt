import dev.tonholo.svgokt.conventions.SvgoKtVersion

plugins {
    id("dev.tonholo.svgokt.conventions.common")
    com.vanniktech.maven.publish
}

val svgoKtVersion = SvgoKtVersion.parse(project.version.toString())

mavenPublishing {
    publishToMavenCentral()

    val signingKey = findProperty("signingInMemoryKey") as String?
        ?: System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
    if (!signingKey.isNullOrBlank()) {
        signAllPublications()
    }

    coordinates(
        groupId = group.toString(),
        artifactId = project.name,
        version = svgoKtVersion.toString(),
    )

    pom {
        name.set(project.name)
        description.set(
            "Kotlin Multiplatform port of SVGO (SVG Optimizer). Targets SVGO " +
                "${svgoKtVersion.svgoUpstream} and runs the same plugin pipeline on JVM, " +
                "JS (klib consumers), and native.",
        )
        url.set("https://github.com/rafaeltonholo/svgo-kt")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("rafaeltonholo")
                name.set("Rafael Tonholo")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/rafaeltonholo/svgo-kt.git")
            developerConnection.set("scm:git:ssh://github.com:rafaeltonholo/svgo-kt.git")
            url.set("https://github.com/rafaeltonholo/svgo-kt")
        }
    }
}

// GitHub Packages is a secondary publishing target. Vanniktech's
// `mavenPublishing` block only configures Maven Central; the GitHub Packages
// repository is added through the regular `publishing` extension so the
// `publishAllPublicationsToGitHubPackagesRepository` task is generated.
publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/rafaeltonholo/svgo-kt")
            credentials {
                username = findProperty("github.username") as String?
                    ?: System.getenv("GITHUB_ACTOR")
                password = findProperty("github.token") as String?
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
