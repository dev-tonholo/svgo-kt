package svgokt.plugins

import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginParams
import svgokt.plugins.builtin.AddAttributesToSVGElement
import svgokt.plugins.builtin.AddClassesToSVGElement
import svgokt.plugins.builtin.CleanupAttrs
import svgokt.plugins.builtin.CleanupEnableBackground
import svgokt.plugins.builtin.CleanupIds
import svgokt.plugins.builtin.CleanupListOfValues
import svgokt.plugins.builtin.CleanupNumericValues
import svgokt.plugins.builtin.CollapseGroups
import svgokt.plugins.builtin.ConvertColors
import svgokt.plugins.builtin.ConvertEllipseToCircle
import svgokt.plugins.builtin.ConvertOneStopGradients
import svgokt.plugins.builtin.ConvertPathData
import svgokt.plugins.builtin.ConvertShapeToPath
import svgokt.plugins.builtin.ConvertStyleToAttrs
import svgokt.plugins.builtin.ConvertTransform
import svgokt.plugins.builtin.InlineStyles
import svgokt.plugins.builtin.MergePaths
import svgokt.plugins.builtin.MergeStyles
import svgokt.plugins.builtin.MinifyStyles
import svgokt.plugins.builtin.MoveElemsAttrsToGroup
import svgokt.plugins.builtin.MoveGroupAttrsToElems
import svgokt.plugins.builtin.PrefixIds
import svgokt.plugins.builtin.RemoveAttributesBySelector
import svgokt.plugins.builtin.RemoveAttrs
import svgokt.plugins.builtin.RemoveComments
import svgokt.plugins.builtin.RemoveDeprecatedAttrs
import svgokt.plugins.builtin.RemoveDesc
import svgokt.plugins.builtin.RemoveDimensions
import svgokt.plugins.builtin.RemoveDoctype
import svgokt.plugins.builtin.RemoveEditorsNSData
import svgokt.plugins.builtin.RemoveElementsByAttr
import svgokt.plugins.builtin.RemoveEmptyAttrs
import svgokt.plugins.builtin.RemoveEmptyContainers
import svgokt.plugins.builtin.RemoveEmptyText
import svgokt.plugins.builtin.RemoveHiddenElems
import svgokt.plugins.builtin.RemoveMetadata
import svgokt.plugins.builtin.RemoveNonInheritableGroupAttrs
import svgokt.plugins.builtin.RemoveOffCanvasPaths
import svgokt.plugins.builtin.RemoveRasterImages
import svgokt.plugins.builtin.RemoveScripts
import svgokt.plugins.builtin.RemoveStyleElement
import svgokt.plugins.builtin.RemoveTitle
import svgokt.plugins.builtin.RemoveUnknownsAndDefaults
import svgokt.plugins.builtin.RemoveUnusedNS
import svgokt.plugins.builtin.RemoveUselessDefs
import svgokt.plugins.builtin.RemoveUselessStrokeAndFill
import svgokt.plugins.builtin.RemoveViewBox
import svgokt.plugins.builtin.RemoveXMLNS
import svgokt.plugins.builtin.RemoveXMLProcInst
import svgokt.plugins.builtin.RemoveXlink
import svgokt.plugins.builtin.ReusePaths
import svgokt.plugins.builtin.SortAttrs
import svgokt.plugins.builtin.SortDefsChildren

/**
 * All builtin plugins, matching the JS svgo builtin.js list.
 *
 * Plugins that require constructor arguments use default parameters.
 * The [PresetDefault] preset is not included here since it is a preset,
 * not a standalone plugin.
 */
val builtinPlugins: List<Plugin<out PluginParams>> = listOf(
    AddAttributesToSVGElement,
    AddClassesToSVGElement,
    CleanupAttrs,
    CleanupEnableBackground,
    CleanupIds(),
    CleanupListOfValues,
    CleanupNumericValues,
    CollapseGroups,
    ConvertColors,
    ConvertEllipseToCircle,
    ConvertOneStopGradients,
    ConvertPathData(),
    ConvertShapeToPath,
    ConvertStyleToAttrs,
    ConvertTransform,
    InlineStyles(),
    MergePaths,
    MergeStyles,
    MinifyStyles(),
    MoveElemsAttrsToGroup,
    MoveGroupAttrsToElems,
    PrefixIds,
    RemoveAttributesBySelector,
    RemoveAttrs,
    RemoveComments,
    RemoveDeprecatedAttrs,
    RemoveDesc,
    RemoveDimensions,
    RemoveDoctype,
    RemoveEditorsNSData,
    RemoveElementsByAttr,
    RemoveEmptyAttrs,
    RemoveEmptyContainers,
    RemoveEmptyText,
    RemoveHiddenElems,
    RemoveMetadata,
    RemoveNonInheritableGroupAttrs,
    RemoveOffCanvasPaths,
    RemoveRasterImages,
    RemoveScripts,
    RemoveStyleElement,
    RemoveTitle,
    RemoveUnknownsAndDefaults,
    RemoveUnusedNS,
    RemoveUselessDefs,
    RemoveUselessStrokeAndFill,
    RemoveViewBox,
    RemoveXlink,
    RemoveXMLNS,
    RemoveXMLProcInst,
    ReusePaths,
    SortAttrs,
    SortDefsChildren,
)
