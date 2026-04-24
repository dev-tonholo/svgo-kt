package svgokt.domain.plugins

import svgokt.domain.XastRoot

typealias PluginFn = (root: XastRoot, params: PluginParams, info: PluginInfo) -> Visitor?

interface PluginParams : Map<String, Any>

/**
 * Wraps a plain map as a [PluginParams].
 *
 * This used to be a `companion object operator fun invoke` on [PluginParams]
 * but the Kotlin/JS IR backend fails to resolve `PluginParams(map)` call sites
 * at runtime (`TypeError: tmp_0.invoke_d5ioxe_k$ is not a function`). A plain
 * top-level factory avoids the mangling issue and works on every KMP target.
 */
internal fun PluginParams(params: Map<String, Any>): PluginParams =
    object : PluginParams, Map<String, Any> by params {}

object NoPluginParam : PluginParams, Map<String, Any> by emptyMap()

interface Plugin<T : PluginParams> {
    val name: String?
    val description: String?
    val params: T?
    val fn: PluginFn?
}

sealed interface PluginConfig {
    data class BuiltinByName(val name: String) : PluginConfig

    data class BuiltinWithParams<T : PluginParams>(
        val name: String,
        val params: T,
    ) : PluginConfig

    data class Custom<T : PluginParams>(
        val name: String,
        val fn: PluginFn,
        val params: T? = null,
    ) : PluginConfig
}
