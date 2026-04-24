# Contributing to svgo-kt

Thanks for taking the time to contribute! svgo-kt is a Kotlin Multiplatform
port of [svg/svgo](https://github.com/svg/svgo) that aims to produce the
same optimized output as upstream while being usable from JVM, Kotlin/JS
(klib consumers) and Kotlin/Native projects.

This document is the short version of "what we expect when you open a PR".
Please read it before sending changes.

## Project goals (and non-goals)

We **do** want to:

- Match upstream svgo's output byte-for-byte for the same input + config.
- Stay idiomatic Kotlin (DSL ergonomics, null-safety, multiplatform-clean code).
- Track every released svgo version (the leading half of our compound
  version, `<svgo-upstream>-<svgo-kt>`, encodes which upstream we mirror).

We **do not** want to:

- Add plugins or behavior changes that don't exist in upstream svgo. Please
  request those at [svg/svgo](https://github.com/svg/svgo) first; we will
  port them once they ship.
- Ship a CLI or native binary as a primary deliverable (see the README's
  "Goals" section).

## Getting set up

You need:

- JDK 17 (Temurin recommended)
- The repository cloned with the upstream `svgo/` mirror present (used for
  the plugin fixture suite)

Common commands:

```bash
# Full JVM build + 363-fixture integration test
./gradlew :svgo-kt:jvmTest

# Cross-platform smoke (one fixture per plugin) on JS and native
./gradlew :svgo-kt:jsNodeTest
./gradlew :svgo-kt:linuxX64Test     # Linux host
./gradlew :svgo-kt:macosArm64Test   # macOS host
./gradlew :svgo-kt:mingwX64Test     # Windows host

# Lint
./gradlew :svgo-kt:detekt

# Publish to your local Maven cache for end-to-end testing
./gradlew :svgo-kt:publishToMavenLocal
```

CI runs JVM + JS + linuxX64 on Ubuntu, macosArm64 on macOS and mingwX64 on
Windows on every PR.

## Branching

- Cut feature branches off `main`.
- Use a meaningful name -- `fix/parser-shared-flow-race`,
  `feat/wasmjs-target`, etc.
- Keep PRs small and focused. Bundling unrelated cleanups makes review
  harder; do them in a separate PR.

## Commits

We use [Conventional Commits](https://www.conventionalcommits.org/). The
common types in this repo:

- `feat(scope):` -- new functionality
- `fix(scope):` -- bug fix
- `refactor(scope):` -- behavior-preserving cleanup
- `test(scope):` -- test-only changes
- `docs(scope):` -- README / KDoc / contributing docs
- `build:` -- Gradle, version catalog, build-logic conventions
- `ci:` -- GitHub Actions, dependabot
- `style:` -- formatting only (detekt auto-fixes, etc.)
- `chore:` -- everything else (templates, repo metadata)

Keep commits granular -- one logical change per commit -- so reviewers and
`git bisect` can navigate the history. Don't add `Co-Authored-By` trailers.

## Code style

- All files go through `./gradlew :svgo-kt:detekt`. CI fails on violations.
- Avoid `!!`. If the compiler thinks a value can be null, handle it
  (`?.`, `?:`, `requireNotNull`, redesign).
- Prefer named arguments for callsites with multiple primitives (helps avoid
  `MagicNumber` issues and improves readability).
- Use Kotlin 2.3 multi-dollar raw strings (`$$"${VERSION}"`) instead of
  backslash-escaping `\${...}`.
- Imports are explicit -- no wildcard imports.

## Testing

- New behavior needs a test. svgo-kt has good fixture coverage from
  upstream; add a unit test under
  `svgo-kt/src/commonTest/kotlin/svgokt/...` for plugin tweaks, and rely on
  the upstream fixture suite for end-to-end parity.
- Test naming follows AAA: ``given X - when Y - then Z``.
- TDD is preferred for new features and bug fixes -- write the failing test
  first, then the implementation.

## Upstream parity

If your PR changes plugin output, please confirm it still matches upstream
svgo for the same input + config. The PR template has a checkbox for this.
The full integration suite (`./gradlew :svgo-kt:jvmTest`) runs all 363
upstream fixtures and is the canonical signal.

## Releases

We follow the KSP-style compound version `<svgo-upstream>-<svgo-kt>` (e.g.
`4.0.1-0.1.0`). The single source of truth is the `svgokt` entry in
`gradle/libs.versions.toml`. To cut a release:

1. Bump `svgokt` in `gradle/libs.versions.toml`.
2. Open a PR, merge to `main`.
3. Tag the merge commit with the same version string
   (`git tag 4.0.1-0.1.1 && git push --tags`).
4. The `Publish` GitHub Actions workflow verifies the tag matches the
   catalog, re-runs the test suite and publishes to Maven Central.

## Code of conduct

Participation is governed by the [Code of Conduct](CODE_OF_CONDUCT.md).
Please read it.
