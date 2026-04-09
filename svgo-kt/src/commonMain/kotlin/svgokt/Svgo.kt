package svgokt

import svgokt.domain.Config
import svgokt.domain.Output
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginConfig
import svgokt.domain.plugins.PluginInfo
import svgokt.domain.plugins.PluginParams
import svgokt.encoding.encodeSvgDataUri
import svgokt.parser.SvgoParser
import svgokt.plugins.PresetDefault
import svgokt.plugins.builtinPlugins
import svgokt.plugins.invokePlugins
import svgokt.stringfier.stringifySvg

interface Svgo {
    suspend fun optimize(input: String, config: Config? = null): Output
}

internal class SvgoImpl(
    private val defaultConfig: Config?,
) : Svgo {
    private val pluginMap: Map<String, Plugin<*>> = buildMap {
        for (plugin in builtinPlugins) {
            val name = plugin.name ?: continue
            put(name, plugin)
        }
        val presetName = PresetDefault.name.orEmpty()
        put(presetName, PresetDefault)
    }

    override suspend fun optimize(input: String, config: Config?): Output {
        var currentInput = input
        val overrideConfig = config ?: defaultConfig ?: Config()
        val maxPassCount = if (overrideConfig.multipass) 10 else 1
        var prevResultSize = Int.MAX_VALUE
        var output = ""
        var info = PluginInfo(
            path = overrideConfig.path,
            multipassCount = 0,
        )

        for (i in 0 until maxPassCount) {
            info = info.copy(multipassCount = i)
            val ast = SvgoParser().parseSvg(data = currentInput, from = overrideConfig.path)
            val plugins = overrideConfig.plugins ?: listOf(PluginConfig.BuiltinByName(name = "preset-default"))
            val resolvedPlugins = plugins.mapNotNull(::resolvePluginConfig)
            if (resolvedPlugins.size < plugins.size) {
                println(
                    "Warning: plugins list includes null or undefined elements, these will be ignored."
                )
            }
            if (overrideConfig.floatPrecision != null) {
                GlobalOverrides.floatPrecision = overrideConfig.floatPrecision
            }
            invokePlugins(
                ast = ast,
                info = info,
                plugins = resolvedPlugins,
                overrides = null,
                globalOverrides = GlobalOverrides,
            )
            output = stringifySvg(data = ast, userOptions = overrideConfig.js2svg)
            if (output.length < prevResultSize) {
                currentInput = output
                prevResultSize = output.length
            } else {
                break
            }
        }

        overrideConfig.dataUri?.let { dataUri ->
            output = encodeSvgDataUri(svg = output, type = dataUri)
        }

        return Output(
            data = output,
        )
    }

    private fun resolvePluginConfig(pluginConfig: PluginConfig): Plugin<*>? {
        return when (pluginConfig) {
            is PluginConfig.BuiltinByName -> {
                val builtin = pluginMap[pluginConfig.name]
                    ?: error("Unknown builtin plugin \"${pluginConfig.name}\" specified.")
                object : Plugin<PluginParams> {
                    override val name = builtin.name
                    override val description = builtin.description
                    override val params = builtin.params
                    override val fn = builtin.fn
                }
            }
            is PluginConfig.BuiltinWithParams<*> -> {
                val builtin = pluginMap[pluginConfig.name]
                    ?: error("Unknown builtin plugin \"${pluginConfig.name}\" specified.")
                object : Plugin<PluginParams> {
                    override val name = builtin.name
                    override val description = builtin.description
                    override val params = pluginConfig.params
                    override val fn = builtin.fn
                }
            }
            is PluginConfig.Custom<*> -> {
                if (pluginConfig.name.isEmpty()) {
                    error("Plugin name should be specified")
                }
                object : Plugin<PluginParams> {
                    override val name = pluginConfig.name
                    override val description = null
                    override val params = pluginConfig.params
                    override val fn = pluginConfig.fn
                }
            }
        }
    }

    override fun toString(): String = "Svgo(defaultConfig = $defaultConfig)"
}
