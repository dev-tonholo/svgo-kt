package svgokt.domain.builder

import svgokt.domain.Config
import svgokt.domain.DataUri
import svgokt.domain.StringifyOptions
import svgokt.domain.builder.plugins.PluginBuilder
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginConfig
import svgokt.domain.plugins.PluginParams
import kotlin.jvm.JvmName
import svgokt.domain.builder.plugins.plugin as pluginDsl

@DslMarker
annotation class ConfigDsl

@ConfigDsl
class ConfigBuilder {
    var path: String? = null
    var multipass: Boolean = false
    var floatPrecision: Int? = null
    private var plugins: MutableList<PluginConfig>? = null
    private var js2svg: StringifyOptions? = null
    var dataUri: DataUri? = null
    private val safePlugin: MutableList<PluginConfig>
        get() = plugins ?: mutableListOf<PluginConfig>().also {
            plugins = it
        }

    fun js2svg(block: StringifyOptionsBuilder.() -> Unit) {
        js2svg = stringifyOptions(block)
    }

    @JvmName("pluginFromString")
    fun plugin(name: String) {
        safePlugin += PluginConfig.BuiltinByName(name = name)
    }

    fun <T : PluginParams> plugin(block: PluginBuilder<T>.() -> Unit) {
        val built = pluginDsl(block)
        val fn = built.fn
        if (fn != null) {
            safePlugin += PluginConfig.Custom(
                name = built.name.orEmpty(),
                fn = fn,
                params = built.params,
            )
        } else {
            safePlugin += PluginConfig.BuiltinByName(name = built.name.orEmpty())
        }
    }

    fun <T : PluginParams> plugin(name: String) {
        safePlugin += PluginConfig.BuiltinByName(name = name)
    }

    fun <T : PluginParams> plugin(plugin: Plugin<T>) {
        val fn = plugin.fn
        if (fn != null) {
            safePlugin += PluginConfig.Custom(
                name = plugin.name.orEmpty(),
                fn = fn,
                params = plugin.params,
            )
        } else {
            safePlugin += PluginConfig.BuiltinByName(name = plugin.name.orEmpty())
        }
    }

    fun build(): Config = Config(
        path = path,
        multipass = multipass,
        floatPrecision = floatPrecision,
        plugins = plugins,
        js2svg = js2svg,
        dataUri = dataUri,
    )
}
