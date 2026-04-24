## Summary

<!-- Brief description of what this PR does and why. -->

## Changes

<!-- Bullet list of the key changes. -->

-

## Test Plan

<!-- How was this tested? Tick all that apply. -->

- [ ] `./gradlew :svgo-kt:jvmTest` — full 363-fixture suite + unit tests pass
- [ ] `./gradlew :svgo-kt:jsNodeTest` — JS/node tests pass
- [ ] `./gradlew :svgo-kt:linuxX64Test` (or another native target) passes
- [ ] `./gradlew :svgo-kt:detekt` passes
- [ ] New behavior is covered by tests

## Upstream Parity

<!--
  svgo-kt mirrors upstream svgo's plugin behavior. If this PR changes plugin
  output, please confirm parity.
-->

- [ ] Output matches upstream `svgo` for the same input + config (or this PR
      is intentionally Kotlin-side only -- DSL, multiplatform support, build,
      etc.)
- [ ] If upstream behavior changed, link the upstream commit / release:

## Related Issues

<!-- e.g. "Closes #123", "Tracks Epic #45" -->
