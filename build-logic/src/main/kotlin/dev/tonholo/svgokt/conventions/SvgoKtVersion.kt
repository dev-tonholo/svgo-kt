package dev.tonholo.svgokt.conventions

/**
 * svgo-kt uses the KSP-style compound version scheme `<svgo-upstream>-<svgo-kt>`:
 * the leading component tracks the upstream SVGO release this artifact mirrors,
 * and the trailing component is our own Kotlin-side semver.
 *
 * Parsing helpers are centralized here so every place that needs either half
 * (POM description, release notes, dependency version ranges, etc.) stays in
 * sync with the single `project.version` string.
 */
data class SvgoKtVersion(
    val svgoUpstream: String,
    val svgoKt: String,
) {
    override fun toString(): String = "$svgoUpstream-$svgoKt"

    companion object {
        private val REGEX = Regex("""^(\d+\.\d+\.\d+)-(\d+\.\d+\.\d+)$""")

        fun parse(version: String): SvgoKtVersion {
            val match = REGEX.matchEntire(version)
                ?: error(
                    "Version '$version' does not match the '<svgo-upstream>-<svgo-kt>' " +
                        "scheme (e.g. '4.0.1-0.1.0'). Update gradle/libs.versions.toml or " +
                        "the build script that sets `version`.",
                )
            return SvgoKtVersion(
                svgoUpstream = match.groupValues[1],
                svgoKt = match.groupValues[2],
            )
        }
    }
}
