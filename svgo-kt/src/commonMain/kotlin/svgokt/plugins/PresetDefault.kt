package svgokt.plugins

import svgokt.GlobalOverrides
import svgokt.domain.builder.plugins.plugin
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.plugins.builtin.CleanupAttrs
import svgokt.plugins.builtin.CleanupEnableBackground
import svgokt.plugins.builtin.CleanupIds
import svgokt.plugins.builtin.CleanupNumericValues
import svgokt.plugins.builtin.CollapseGroups
import svgokt.plugins.builtin.ConvertEllipseToCircle
import svgokt.plugins.builtin.ConvertPathData
import svgokt.plugins.builtin.ConvertShapeToPath
import svgokt.plugins.builtin.ConvertTransform
import svgokt.plugins.builtin.InlineStyles
import svgokt.plugins.builtin.MergePaths
import svgokt.plugins.builtin.MergeStyles
import svgokt.plugins.builtin.MinifyStyles
import svgokt.plugins.builtin.MoveElemsAttrsToGroup
import svgokt.plugins.builtin.MoveGroupAttrsToElems
import svgokt.plugins.builtin.RemoveComments
import svgokt.plugins.builtin.RemoveDeprecatedAttrs
import svgokt.plugins.builtin.RemoveDesc
import svgokt.plugins.builtin.RemoveDoctype
import svgokt.plugins.builtin.RemoveEditorsNSData
import svgokt.plugins.builtin.RemoveEmptyAttrs
import svgokt.plugins.builtin.RemoveEmptyContainers
import svgokt.plugins.builtin.RemoveEmptyText
import svgokt.plugins.builtin.RemoveHiddenElems
import svgokt.plugins.builtin.RemoveMetadata
import svgokt.plugins.builtin.RemoveNonInheritableGroupAttrs
import svgokt.plugins.builtin.RemoveUnusedNS
import svgokt.plugins.builtin.RemoveUselessStrokeAndFill
import svgokt.plugins.builtin.RemoveXMLProcInst
import svgokt.plugins.builtin.SortAttrs
import svgokt.plugins.builtin.SortDefsChildren

/**
 * Preset-default plugin list, matching svgo 4.0.1 order.
 *
 * Not yet implemented (CSS-dependent):
 * - removeUselessDefs (position 12)
 * - convertColors (position 14)
 * - removeUnknownsAndDefaults (position 15)
 */
private val plugins: List<Plugin<*>> = listOf(
    RemoveDoctype, // 1
    RemoveXMLProcInst, // 2
    RemoveComments, // 3
    RemoveDeprecatedAttrs, // 4
    RemoveMetadata, // 5
    RemoveEditorsNSData, // 6
    CleanupAttrs, // 7
    MergeStyles, // 8
    InlineStyles(), // 9
    MinifyStyles(), // 10
    CleanupIds(), // 11
    // removeUselessDefs,            // 12 - not yet implemented
    CleanupNumericValues, // 13
    // convertColors,                // 14 - not yet implemented
    // removeUnknownsAndDefaults,    // 15 - not yet implemented
    RemoveNonInheritableGroupAttrs, // 16
    RemoveUselessStrokeAndFill, // 17
    CleanupEnableBackground, // 18
    RemoveHiddenElems, // 19
    RemoveEmptyText, // 20
    ConvertShapeToPath, // 21
    ConvertEllipseToCircle, // 22
    MoveElemsAttrsToGroup, // 23
    MoveGroupAttrsToElems, // 24
    CollapseGroups, // 25
    ConvertPathData(), // 26
    ConvertTransform, // 27
    RemoveEmptyAttrs, // 28
    RemoveEmptyContainers, // 29
    MergePaths, // 30
    RemoveUnusedNS, // 31
    SortAttrs, // 32
    SortDefsChildren, // 33
    RemoveDesc, // 34
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
