import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    org.jetbrains.dokka
}

dokka {
    dokkaSourceSets.configureEach {
        // Public API only -- consumers don't need to read internal/private
        // implementation details, and exposing them would inflate the site.
        documentedVisibilities(
            VisibilityModifier.Public,
            VisibilityModifier.Protected,
        )

        // Surface a per-module overview when the module ships a `MODULE.md`
        // file (e.g. `svgo-kt/MODULE.md`). Optional -- absence is fine.
        val moduleDocFile = project.layout.projectDirectory.file("MODULE.md")
        if (moduleDocFile.asFile.exists()) {
            includes.from(moduleDocFile)
        }

        // Link "View source" in the generated docs back to the GitHub blob
        // for the corresponding line.
        sourceLink {
            localDirectory.set(rootProject.projectDir)
            remoteUrl("https://github.com/rafaeltonholo/svgo-kt/tree/main")
            remoteLineSuffix.set("#L")
        }
    }

    pluginsConfiguration.html {
        footerMessage.set("(c) Rafael Tonholo")
        homepageLink.set("https://github.com/rafaeltonholo/svgo-kt")
    }
}
