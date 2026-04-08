package svgokt.plugins.builtin

import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams

data class MakeArcs(
    val threshold: Float,
    val tolerance: Float,
)

data class ConvertPathDataParams(
    val applyTransforms: Boolean = true,
    val applyTransformsStroked: Boolean = true,
    val makeArcs: MakeArcs = MakeArcs(
        threshold = 2.5f,
        tolerance = 0.5f,
    ),
    val straightCurves: Boolean = true,
    val convertToQ: Boolean = true,
    val lineShorthands: Boolean = true,
    val convertToZ: Boolean = true,
    val curveSmoothShorthands: Boolean = true,
    val floatPrecision: Int = 3,
    val transformPrecision: Int = 5,
    val smartArcRounding: Boolean = true,
    val removeUseless: Boolean = true,
    val collapseRepeated: Boolean = true,
    val utilizeAbsolute: Boolean = true,
    val leadingZero: Boolean = true,
    val negativeExtraSpace: Boolean = true,
    val noSpaceAfterFlags: Boolean = false, // a20 60 45 0 1 30 20 → a20 60 45 0130 20
    val forceAbsolutePath: Boolean = false,
) : PluginParams,
    Map<String, Any> by mapOf(
        "applyTransforms" to applyTransforms,
        "applyTransformsStroked" to applyTransformsStroked,
        "makeArcs" to makeArcs,
        "straightCurves" to straightCurves,
        "convertToQ" to convertToQ,
        "lineShorthands" to lineShorthands,
        "convertToZ" to convertToZ,
        "curveSmoothShorthands" to curveSmoothShorthands,
        "floatPrecision" to floatPrecision,
        "transformPrecision" to transformPrecision,
        "smartArcRounding" to smartArcRounding,
        "removeUseless" to removeUseless,
        "collapseRepeated" to collapseRepeated,
        "utilizeAbsolute" to utilizeAbsolute,
        "leadingZero" to leadingZero,
        "negativeExtraSpace" to negativeExtraSpace,
        "noSpaceAfterFlags" to noSpaceAfterFlags,
        "forceAbsolutePath" to forceAbsolutePath,
    )

class ConvertPathData(
    override val params: ConvertPathDataParams = ConvertPathDataParams(),
) : Plugin<ConvertPathDataParams> {
    override val name: String = "convertPathData"
    override val description: String = "optimizes path data: writes in shorter form, applies transformations"

    /**
     * Convert absolute Path to relative,
     * collapse repeated instructions,
     * detect and convert Lineto shorthands,
     * remove useless instructions like "l0,0",
     * trim useless delimiters and leading zeros,
     * decrease accuracy of floating-point numbers.
     *
     * @see https://www.w3.org/TR/SVG11/paths.html#PathData
     *
     * @author Kir Belevich / parsed to kotlin by Rafael Tonholo
     */
    override val fn: PluginFn = { root, params, info ->
        null // TODO.
    }
}
