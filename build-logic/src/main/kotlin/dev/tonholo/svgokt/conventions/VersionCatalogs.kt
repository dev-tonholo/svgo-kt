package dev.tonholo.svgokt.conventions

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

/**
 * Workaround so convention plugin scripts written as `.gradle.kts` files can
 * access the version catalog through the generated `LibrariesForLibs`
 * accessor, just like regular build scripts.
 *
 * https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
 */
internal val Project.libs: LibrariesForLibs get() = extensions.getByType()
