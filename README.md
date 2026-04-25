# SVGO Kotlin
This is an interpretation of the awesome tool [SVGO](https://github.com/svg/svgo), 
written in Kotlin.

## Why?
Very often we find a great tool that would help us in our code, however, 
it is written in a code language incompatible with the one we are using.

[SVGO](https://github.com/svg/svgo), short for **SVG O**ptimizer, is a 
**Node.js library** and command-line application for optimizing SVG files.

As compiled languages cannot use JavaScript libraries without embedding a 
JavaScript engine, this project serves as an interpretation of the library/tool.

This project was created to enable the use of the [SVGO](https://github.com/svg/svgo) 
in compiled languages by leveraging KMP.

## Goals
It is crucial to say that this library does not target to be a replacement
/superset/subset of [SVGO](https://github.com/svg/svgo). The main target is 
to enable usage of it as a library on compiled languages.

Having that said, we have the following goals:

1. Create a kotlin library that enables the usage of the logic inside 
[SVGO](https://github.com/svg/svgo) on languages such as Kotlin and Java.
2. Not to be a 1:1 parse of the [SVGO](https://github.com/svg/svgo) logic, 
but also apply changes in the code by using all available features inside 
the Kotlin language.
3. Keep on track of changes that happened on each released version of 
[SVGO](https://github.com/svg/svgo).
4. Get the same output result as [SVGO](https://github.com/svg/svgo).

Producing a CLI tool or native binary is not a goal for this project. However, 
we may create a native binary for testing purposes on CI, to keep the integrity 
of our code with the [SVGO](https://github.com/svg/svgo).

# Reporting bugs
If you find any bug by using this library, it is our responsibility to identify 
if it was a bug introduced by us or if it is something related to 
[SVGO](https://github.com/svg/svgo), so please **report in our repository first**.

In case we identify that was on our side, we are going to address it as soon 
as possible, and in case we understand it is on [SVGO](https://github.com/svg/svgo)
side, we are going to link an open issue, in case it exists, or ask to create an
issue in the [SVGO](https://github.com/svg/svgo) repository.

> [!IMPORTANT]
> We do not plan to fix issues within the [SVGO](https://github.com/svg/svgo) logic 
> on our side before they address it.

# Supported targets
svgo-kt is a Kotlin Multiplatform library and is usable from any KMP project
that targets one of:

- JVM (including Android)
- Kotlin/JS — **klib consumers only**; this artifact is not a drop-in
  replacement for the npm `svgo` package. If you need `svgo` in a pure
  JavaScript/Node project, use the original
  [svgo](https://github.com/svg/svgo).
- Kotlin/Native: `linuxX64`, `mingwX64`, `macosArm64`, `macosX64`

# Versioning
Releases use the KSP-style compound version `<svgo-upstream>-<svgo-kt>`:

```
4.0.1-0.1.0
└─┬─┘ └─┬─┘
  │     └── svgo-kt's own semver (bumped for Kotlin-side changes)
  └──────── upstream SVGO version this release mirrors
```

The upstream SVGO version leads so consumers can see at a glance which SVGO
release a given artifact targets. Our own semver increments independently for
bug fixes, features, and breaking API changes on the Kotlin side.

The two halves live as separate entries in `gradle/libs.versions.toml`
(`svgo-upstream` and `svgokt`). The right half is automatically bumped by
[release-please](https://github.com/googleapis/release-please) based on
conventional commits (`fix:`, `feat:`, `feat!:`); the left half is bumped
manually when we sync against a new upstream svgo release.

# Installation
svgo-kt is published to Maven Central under `dev.tonholo:svgo-kt`.

Add it to your Kotlin Multiplatform project:

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("dev.tonholo:svgo-kt:4.0.1-0.1.0")
        }
    }
}
```

For a JVM-only project:

```kotlin
dependencies {
    implementation("dev.tonholo:svgo-kt:4.0.1-0.1.0")
}
```

# Usage
svgo-kt exposes a small DSL around a suspend `optimize` function that mirrors
SVGO's `optimize(svgString, config)` entry point. Because it uses `suspend`,
all examples below assume a coroutine scope (`runBlocking`, `runTest`,
`viewModelScope`, etc.).

## Minimal: run `preset-default` on an SVG string
```kotlin
import svgokt.domain.builder.svgo

val optimizer = svgo {} // empty config == preset-default (matches `svgo` CLI)
val result = optimizer.optimize(input = rawSvgString)
println(result.data)
```

## Configure plugins explicitly
Pick individual plugins, mix in your own, or override plugin parameters via
the `config { plugin { ... } }` DSL:

```kotlin
import svgokt.domain.builder.svgo
import svgokt.plugins.builtin.CleanupIds

val optimizer = svgo {
    config {
        multipass = true
        floatPrecision = 2
        js2svg {
            pretty = true
            indent = 2
        }
        // By builtin name (default parameters).
        plugin(name = "removeComments")
        plugin(name = "removeDimensions")
        // Pass a plugin instance to customize parameters via its typed API.
        plugin(
            CleanupIds(
                params = CleanupIds.Params(
                    minify = false,
                    preserve = setOf("logo", "brand"),
                ),
            ),
        )
    }
}

val result = optimizer.optimize(input = rawSvgString)
```

You can also pass a one-off `Config` per call; it overrides the default
config the optimizer was built with:

```kotlin
import svgokt.domain.Config
import svgokt.domain.builder.stringifyOptions
import svgokt.domain.plugins.PluginConfig

val optimizer = svgo {}
val result = optimizer.optimize(
    input = rawSvgString,
    config = Config(
        plugins = listOf(PluginConfig.BuiltinByName(name = "removeMetadata")),
        js2svg = stringifyOptions { pretty = true },
    ),
)
```

## Built-in plugins
All 54 SVGO builtins are implemented. A few of the most common:

- `preset-default` (runs the standard safe set, like the `svgo` CLI default)
- `removeComments`, `removeMetadata`, `removeDimensions`
- `cleanupIds`, `cleanupAttrs`, `cleanupNumericValues`
- `convertColors`, `convertPathData`, `convertShapeToPath`
- `inlineStyles`, `minifyStyles`
- `mergePaths`, `mergeStyles`
- `sortAttrs`, `sortDefsChildren`

See `svgo-kt/src/commonMain/kotlin/svgokt/plugins/builtinPlugins.kt` for the
full list and `config/` in [SVGO](https://github.com/svg/svgo) for parameter
reference.

# License and Copyright
This software is released under the terms of the [MIT license](LICENSE).
