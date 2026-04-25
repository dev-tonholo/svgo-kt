package dev.tonholo.svgokt.conventions

/**
 * svgo-kt uses the KSP-style compound version scheme `<svgo-upstream>-<svgokt>`:
 * the leading component tracks the upstream SVGO release this artifact mirrors,
 * and the trailing component is our own Kotlin-side semver.
 *
 * The two halves live as separate entries in `gradle/libs.versions.toml`
 * (`svgo-upstream` and `svgokt`) so they can be bumped independently --
 * `svgo-upstream` manually when we sync to a new svgo release, and `svgokt`
 * automatically by release-please. This class is the canonical join point;
 * every place that needs either half (POM description, release notes,
 * `project.version`) goes through here so the two stay in sync.
 */
data class SvgoKtVersion(
    val svgoUpstream: String,
    val svgoKt: String,
) {
    override fun toString(): String = "$svgoUpstream-$svgoKt"

    companion object {
        private val PARTS_REGEX = Regex("""^(\d+\.\d+\.\d+)-(\d+\.\d+\.\d+)$""")
        private val PART_REGEX = Regex("""^\d+\.\d+\.\d+$""")

        /** Parse a single compound `<svgo-upstream>-<svgokt>` string. */
        fun parse(version: String): SvgoKtVersion {
            val match = PARTS_REGEX.matchEntire(version)
                ?: error(
                    "Version '$version' does not match the '<svgo-upstream>-<svgokt>' " +
                        "scheme (e.g. '4.0.1-0.1.0').",
                )
            return SvgoKtVersion(
                svgoUpstream = match.groupValues[1],
                svgoKt = match.groupValues[2],
            )
        }

        /**
         * Compose a [SvgoKtVersion] from the two catalog halves. Both
         * arguments must be plain semver strings; cross-validation here
         * catches typos in `gradle/libs.versions.toml` at configuration time
         * rather than producing a malformed Maven coordinate.
         */
        fun of(svgoUpstream: String, svgoKt: String): SvgoKtVersion {
            require(PART_REGEX.matches(svgoUpstream)) {
                "svgo-upstream version '$svgoUpstream' is not a plain semver (X.Y.Z)."
            }
            require(PART_REGEX.matches(svgoKt)) {
                "svgokt version '$svgoKt' is not a plain semver (X.Y.Z)."
            }
            return SvgoKtVersion(svgoUpstream = svgoUpstream, svgoKt = svgoKt)
        }
    }
}
