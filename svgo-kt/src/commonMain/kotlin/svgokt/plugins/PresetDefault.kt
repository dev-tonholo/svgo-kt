package svgokt.plugins

import svgokt.GlobalOverrides
import svgokt.domain.builder.plugins.plugin
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.plugins.builtin.MergeStyles

private val plugins: List<Plugin<*>> = listOf(
    MergeStyles,
)

val PresetDefault = plugin<NoPluginParam> {
    name = "preset-default"
    description = null
    params = NoPluginParam
    fn { root, params, info ->
        val floatPrecision = params["floatPrecision"] as? Int
        val overrides = (params["overrides"] as? Map<*, *>)
            ?.filterNot { it.value is Boolean && it.key is String }
            ?.mapKeys { it.value as String }
            ?.mapValues { it.value as Boolean }

        if (floatPrecision != null) {
            GlobalOverrides.floatPrecision = floatPrecision
        }

        if (overrides != null) {
            val pluginNames = plugins.mapNotNull { it.name }
            for ((pluginName, _) in overrides) {
                if (pluginNames.contains(pluginName).not()) {
                    println(
                        """
                        |You are trying to configure $pluginName which is not part of $name.
                        |Try to put it before or after, for example
                        |
                        |plugins: [
                        |  {
                        |    name: '$name',
                        |  },
                        |  '$pluginName'
                        |]
                        """.trimMargin()
                    )
                }
            }
        }
        invokePlugins(ast = root, info, plugins = plugins, overrides = overrides, globalOverrides = GlobalOverrides)
        null
    }
}
