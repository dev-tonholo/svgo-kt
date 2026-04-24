import dev.tonholo.svgokt.conventions.SvgoKtVersion
import dev.tonholo.svgokt.conventions.libs

// Every consuming module in svgo-kt applies this convention first so the
// Maven coordinate metadata is derived from a single catalog entry.
group = "dev.tonholo"
version = SvgoKtVersion.parse(libs.versions.svgokt.get()).toString()
