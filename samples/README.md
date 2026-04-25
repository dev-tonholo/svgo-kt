# svgo-kt samples

Tiny, runnable consumers of `:svgo-kt` for each supported Kotlin
Multiplatform target. They exist so you can:

- Verify that the published artifact behaves the same way on every host
  during a local build.
- See real `svgo {}` DSL usage outside of unit tests.
- Have a starting template if you're integrating svgo-kt into your own
  project.

## What's in here

```
samples/src/
├── commonMain/kotlin/Resources.kt     # Shared SVG fixtures (Simple, EntitySvg, Styles)
├── jvmMain/kotlin/Main.kt             # JVM entry point (uses runBlocking)
├── jsMain/kotlin/Main.kt              # Node.js entry point (suspend main)
├── jsMain/kotlin/svgojs/svgo.kt       # Optional comparison hooks for the upstream npm svgo
├── jsMain/resources/index.html        # Browser harness for the JS sample
└── nativeMain/kotlin/Main.kt          # Shared native entry point (linuxX64, mingwX64, macosArm64, macosX64)
```

All four entry points run the same scenario: load `SvgResource.Styles`,
optimize it through `svgo {}` with `floatPrecision = 2` and pretty-print
output, then compare the result to the `@@@`-separated expected payload.

## Running locally

> **Note:** the samples depend on `:svgo-kt` from the same build, so you
> don't need to publish anything to consume them.

### JVM
```bash
./gradlew :samples:runJvm
```

### Kotlin/JS (Node)
```bash
./gradlew :samples:jsNodeRun
```

### Kotlin/JS (browser)
```bash
./gradlew :samples:jsBrowserRun
# then open http://localhost:8080
```

### Native
Pick the executable for your host:
```bash
./gradlew :samples:runDebugExecutableMacosArm64   # Apple Silicon
./gradlew :samples:runDebugExecutableMacosX64     # Intel macOS
./gradlew :samples:runDebugExecutableLinuxX64     # Linux x86_64
./gradlew :samples:runDebugExecutableMingwX64     # Windows x86_64 (cross-compiled)
```

If you're not sure which task name corresponds to your host, run:
```bash
./gradlew :samples:tasks --group run
```

## Adding a new scenario

1. Add a new triple-quoted SVG / expected pair to
   `commonMain/kotlin/Resources.kt`, separated by `@@@`.
2. Reference it from any of the per-target `Main.kt` files via
   `SvgResource.YourName`.
3. Run the relevant target's task above and confirm `Expected == output`
   prints `true`.

If you want to compare svgo-kt's output against the upstream npm `svgo`
package on the JS target, see the helpers in
`jsMain/kotlin/svgojs/svgo.kt` -- the sample build pulls in
`npm("svgo", "3.2.0")` as a dev dependency for exactly that purpose.
