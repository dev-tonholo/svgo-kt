@file:Suppress("MagicNumber")

package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.transform.TransformItem
import svgokt.transform.TransformParams
import svgokt.transform.js2transform
import svgokt.transform.matrixToTransform
import svgokt.transform.parseTransform
import svgokt.transform.roundTransform
import svgokt.transform.transformsMultiply

/**
 * Transform attribute names that this plugin processes.
 */
private val transformAttributes = listOf(
    "transform",
    "gradientTransform",
    "patternTransform",
)

/**
 * Collapses multiple transformations and optimizes them.
 *
 * Converts matrices to short aliases, converts long translate/scale/rotate
 * notations to short ones, multiplies transforms into one matrix,
 * decomposes back to short forms, and removes useless transforms.
 *
 * @see https://www.w3.org/TR/SVG11/coords.html#TransformMatrixDefined
 */
object ConvertTransform : Plugin<ConvertTransform.Params> {

    data class Params(
        val convertToShorts: Boolean = true,
        val degPrecision: Int? = null,
        val floatPrecision: Int = TransformParams.DEFAULT_FLOAT_PRECISION,
        val transformPrecision: Int = TransformParams.DEFAULT_TRANSFORM_PRECISION,
        val matrixToTransform: Boolean = true,
        val shortTranslate: Boolean = true,
        val shortScale: Boolean = true,
        val shortRotate: Boolean = true,
        val removeUseless: Boolean = true,
        val collapseIntoOne: Boolean = true,
        val leadingZero: Boolean = true,
        val negativeExtraSpace: Boolean = false,
    ) : PluginParams,
        Map<String, Any> by mapOf(
            "convertToShorts" to convertToShorts,
            "degPrecision" to (degPrecision ?: -1),
            "floatPrecision" to floatPrecision,
            "transformPrecision" to transformPrecision,
            "matrixToTransform" to matrixToTransform,
            "shortTranslate" to shortTranslate,
            "shortScale" to shortScale,
            "shortRotate" to shortRotate,
            "removeUseless" to removeUseless,
            "collapseIntoOne" to collapseIntoOne,
            "leadingZero" to leadingZero,
            "negativeExtraSpace" to negativeExtraSpace,
        )

    override val name: String = "convertTransform"
    override val description: String = "collapses multiple transformations and optimizes it"
    override val params: Params = Params()

    override val fn: PluginFn = { _, pluginParams, _ ->
        val p = resolveParams(pluginParams = pluginParams)
        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    onEnter(node = node, params = p)
                },
            ),
        )
    }
}

private fun resolveParams(pluginParams: PluginParams): TransformParams {
    // If it's already our Params type, convert directly
    if (pluginParams is ConvertTransform.Params) {
        return pluginParams.toTransformParams()
    }

    // Otherwise, merge from map (used by fixture test with JSON overrides)
    val defaults = ConvertTransform.Params()
    return TransformParams(
        convertToShorts = (pluginParams["convertToShorts"] as? Boolean) ?: defaults.convertToShorts,
        degPrecision = resolveDegPrecision(pluginParams = pluginParams, default = defaults.degPrecision),
        floatPrecision = (pluginParams["floatPrecision"] as? Number)?.toInt() ?: defaults.floatPrecision,
        transformPrecision = (pluginParams["transformPrecision"] as? Number)?.toInt()
            ?: defaults.transformPrecision,
        matrixToTransform = (pluginParams["matrixToTransform"] as? Boolean) ?: defaults.matrixToTransform,
        shortTranslate = (pluginParams["shortTranslate"] as? Boolean) ?: defaults.shortTranslate,
        shortScale = (pluginParams["shortScale"] as? Boolean) ?: defaults.shortScale,
        shortRotate = (pluginParams["shortRotate"] as? Boolean) ?: defaults.shortRotate,
        removeUseless = (pluginParams["removeUseless"] as? Boolean) ?: defaults.removeUseless,
        collapseIntoOne = (pluginParams["collapseIntoOne"] as? Boolean) ?: defaults.collapseIntoOne,
        leadingZero = (pluginParams["leadingZero"] as? Boolean) ?: defaults.leadingZero,
        negativeExtraSpace = (pluginParams["negativeExtraSpace"] as? Boolean) ?: defaults.negativeExtraSpace,
    )
}

private fun resolveDegPrecision(pluginParams: PluginParams, default: Int?): Int? {
    val value = pluginParams["degPrecision"] ?: return default
    return when {
        value is Number && value.toInt() >= 0 -> value.toInt()
        else -> default
    }
}

private fun ConvertTransform.Params.toTransformParams(): TransformParams = TransformParams(
    convertToShorts = convertToShorts,
    degPrecision = degPrecision,
    floatPrecision = floatPrecision,
    transformPrecision = transformPrecision,
    matrixToTransform = matrixToTransform,
    shortTranslate = shortTranslate,
    shortScale = shortScale,
    shortRotate = shortRotate,
    removeUseless = removeUseless,
    collapseIntoOne = collapseIntoOne,
    leadingZero = leadingZero,
    negativeExtraSpace = negativeExtraSpace,
)

private fun onEnter(
    node: XastElement,
    params: TransformParams,
): VisitState {
    for (attrName in transformAttributes) {
        val transformValue = node.attributes[attrName] ?: continue
        convertTransform(node = node, attrName = attrName, transformValue = transformValue, params = params)
    }
    return VisitState.Continue
}

private fun convertTransform(
    node: XastElement,
    attrName: String,
    transformValue: String,
    params: TransformParams,
) {
    var data = parseTransform(transformString = transformValue)
    if (data.isEmpty()) {
        node.attributes.remove(attrName)
        return
    }

    val resolvedParams = definePrecision(data = data, params = params)

    if (resolvedParams.collapseIntoOne && data.size > 1) {
        data = listOf(transformsMultiply(transforms = data))
    }

    val processed = if (resolvedParams.convertToShorts) {
        convertToShorts(transforms = data.toMutableList(), params = resolvedParams)
    } else {
        data.map { roundTransform(transform = it, params = resolvedParams) }
    }

    val filtered = if (resolvedParams.removeUseless) {
        removeUseless(transforms = processed)
    } else {
        processed
    }

    if (filtered.isNotEmpty()) {
        node.attributes[attrName] = js2transform(transforms = filtered, params = resolvedParams)
    } else {
        node.attributes.remove(attrName)
    }
}

/**
 * Defines precision to work with certain parts.
 * Mirrors JS `definePrecision`.
 */
private fun definePrecision(
    data: List<TransformItem>,
    params: TransformParams,
): TransformParams {
    val matrixData = mutableListOf<Double>()
    for (item in data) {
        if (item.name == "matrix") {
            matrixData.addAll(item.data.take(n = 4).toList())
        }
    }

    var transformPrecision = params.transformPrecision
    var numberOfDigits = params.transformPrecision

    if (matrixData.isNotEmpty()) {
        val maxFloatDigits = matrixData.maxOf { floatDigits(n = it) }
        transformPrecision = minOf(
            params.transformPrecision,
            if (maxFloatDigits > 0) maxFloatDigits else params.transformPrecision,
        )

        numberOfDigits = matrixData.maxOf { n ->
            n.toString().replace(Regex("\\D+"), "").length
        }
    }

    val degPrecision = if (params.degPrecision == null) {
        maxOf(0, minOf(params.floatPrecision, numberOfDigits - 2))
    } else {
        params.degPrecision
    }

    return params.copy(
        transformPrecision = transformPrecision,
        degPrecision = degPrecision,
    )
}

/**
 * Returns number of digits after the decimal point.
 * e.g. 0.125 -> 3
 */
private fun floatDigits(n: Double): Int {
    val str = n.toString()
    val dotIndex = str.indexOf('.')
    return if (dotIndex < 0) 0 else str.length - dotIndex - 1
}

/**
 * Convert transforms to the shorthand alternatives.
 * Mirrors JS `convertToShorts`.
 */
private fun convertToShorts(
    transforms: MutableList<TransformItem>,
    params: TransformParams,
): List<TransformItem> {
    var i = 0
    while (i < transforms.size) {
        var transform = transforms[i]

        // convert matrix to short aliases
        if (params.matrixToTransform && transform.name == "matrix") {
            val decomposed = matrixToTransform(origMatrix = transform, params = params)
            val decomposedStr = js2transform(transforms = decomposed, params = params)
            val matrixStr = js2transform(transforms = listOf(transform), params = params)
            if (decomposedStr.length <= matrixStr.length) {
                transforms.removeAt(index = i)
                transforms.addAll(index = i, elements = decomposed)
            }
            transform = transforms[i]
        }

        // round
        transforms[i] = roundTransform(transform = transform, params = params)
        transform = transforms[i]

        // convert long translate to short: translate(10 0) -> translate(10)
        if (params.shortTranslate && transform.name == "translate" &&
            transform.data.size == 2 && transform.data[1] == 0.0
        ) {
            transforms[i] = TransformItem(name = "translate", data = doubleArrayOf(transform.data[0]))
        }

        // convert long scale to short: scale(2 2) -> scale(2)
        if (params.shortScale && transform.name == "scale" &&
            transform.data.size == 2 && transform.data[0] == transform.data[1]
        ) {
            transforms[i] = TransformItem(name = "scale", data = doubleArrayOf(transform.data[0]))
        }

        // convert translate(cx cy) rotate(a) translate(-cx -cy) -> rotate(a cx cy)
        if (params.shortRotate && i >= 2 &&
            transforms[i - 2].name == "translate" &&
            transforms[i - 1].name == "rotate" &&
            transforms[i].name == "translate" &&
            transforms[i - 2].data[0] == -transforms[i].data[0] &&
            transforms[i - 2].data.getOrElse(index = 1) { 0.0 } ==
            -transforms[i].data.getOrElse(index = 1) { 0.0 }
        ) {
            val rotateAngle = transforms[i - 1].data[0]
            val cx = transforms[i - 2].data[0]
            val cy = transforms[i - 2].data.getOrElse(index = 1) { 0.0 }
            val merged = TransformItem(name = "rotate", data = doubleArrayOf(rotateAngle, cx, cy))
            transforms.removeAt(index = i)
            transforms.removeAt(index = i - 1)
            transforms.removeAt(index = i - 2)
            transforms.add(index = i - 2, element = merged)
            i -= 2
        }

        i++
    }
    return transforms
}

/**
 * Remove useless transforms.
 * Mirrors JS `removeUseless`.
 */
private fun removeUseless(transforms: List<TransformItem>): List<TransformItem> {
    return transforms.filter { transform ->
        val data = transform.data
        when {
            // translate(0), rotate(0[, cx, cy]), skewX(0), skewY(0)
            transform.name in listOf("translate", "rotate", "skewX", "skewY") &&
                (data.size == 1 || transform.name == "rotate") &&
                data[0] == 0.0 -> false

            // translate(0, 0)
            transform.name == "translate" &&
                data.getOrElse(index = 0) { 1.0 } == 0.0 &&
                data.getOrElse(index = 1) { 1.0 } == 0.0 -> false

            // scale(1) or scale(1,1)
            transform.name == "scale" &&
                data[0] == 1.0 &&
                (data.size < 2 || data[1] == 1.0) -> false

            // matrix(1 0 0 1 0 0)
            transform.name == "matrix" &&
                data[0] == 1.0 && data[3] == 1.0 &&
                data[1] == 0.0 && data[2] == 0.0 &&
                data[4] == 0.0 && data[5] == 0.0 -> false

            else -> true
        }
    }
}
