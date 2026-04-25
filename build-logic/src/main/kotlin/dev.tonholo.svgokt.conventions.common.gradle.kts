import dev.tonholo.svgokt.conventions.SvgoKtVersion
import dev.tonholo.svgokt.conventions.libs

// Every consuming module in svgo-kt applies this convention first so the
// Maven coordinate metadata is derived from a single set of catalog entries.
group = "dev.tonholo"
version = SvgoKtVersion.of(
    svgoUpstream = libs.versions.svgo.upstream.get(),
    svgoKt = libs.versions.svgokt.get(),
).toString()
