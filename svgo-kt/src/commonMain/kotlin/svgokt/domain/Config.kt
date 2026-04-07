package svgokt.domain

import svgokt.domain.plugins.PluginConfig

data class Config(
    /**
     * Can be used by plugins, for example, prefixids
     */
    val path: String? = null,
    /** Pass over SVGs multiple times to ensure all optimizations are applied. */
    val multipass: Boolean = false,
    /** Precision of floating point numbers. Will be passed to each plugin that supports this param. */
    val floatPrecision: Int? = null,
    /**
     * Plugins configuration
     * ['preset-default'] is default
     * Can also specify any builtin plugin
     * ['sortAttrs', { name: 'prefixIds', params: { prefix: 'my-prefix' } }]
     * Or custom
     * [{ name: 'myPlugin', fn: () => ({}) }]
     * TODO.
     */
    val plugins: List<PluginConfig>? = null,
    /** Options for rendering optimized SVG from AST. */
    val js2svg: StringifyOptions? = null,
    /** Output as Data URI string. */
    val dataUri: DataUri? = null,
)
