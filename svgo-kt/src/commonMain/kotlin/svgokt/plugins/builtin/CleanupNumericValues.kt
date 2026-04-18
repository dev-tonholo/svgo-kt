package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Rounds numeric attribute values to a fixed precision, removes default 'px'
 * units, strips leading zeros, and optionally converts absolute length units
 * to pixels.
 */
object CleanupNumericValues : Plugin<CleanupNumericValues.Params> {
    data class Params(
        val floatPrecision: Int = DEFAULT_FLOAT_PRECISION,
        val leadingZero: Boolean = true,
        val defaultPx: Boolean = true,
        val convertToPx: Boolean = true,
    ) : PluginParams,
        Map<String, Any> by mapOf(
            "floatPrecision" to floatPrecision,
            "leadingZero" to leadingZero,
            "defaultPx" to defaultPx,
            "convertToPx" to convertToPx,
        )

    private const val DEFAULT_FLOAT_PRECISION = 3

    // Conversion factors from absolute length units to pixels.
    private const val CM_TO_PX = 96.0 / 2.54
    private const val MM_TO_PX = 96.0 / 25.4
    private const val IN_TO_PX = 96.0
    private const val PT_TO_PX = 4.0 / 3.0
    private const val PC_TO_PX = 16.0
    private const val PX_TO_PX = 1.0

    private val numericValueRegex =
        Regex("""^([-+]?\d*\.?\d+([eE][-+]?\d+)?)(px|pt|pc|mm|cm|in|em|ex|%)?$""")

    private val absoluteLengths = mapOf(
        "cm" to CM_TO_PX,
        "mm" to MM_TO_PX,
        "in" to IN_TO_PX,
        "pt" to PT_TO_PX,
        "pc" to PC_TO_PX,
        "px" to PX_TO_PX,
    )

    override val name: String = "cleanupNumericValues"
    override val description: String =
        "rounds numeric values to the fixed precision, removes default 'px' units"
    override val params: Params = Params()
    override val fn: PluginFn = { _, pluginParams, _ ->
        val resolvedParams = pluginParams as? Params ?: Params()
        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    onEnter(node, parentNode, resolvedParams)
                },
            ),
        )
    }

    private fun onEnter(
        node: XastElement,
        @Suppress("UNUSED_PARAMETER") parentNode: XastParent?,
        params: Params,
    ): VisitState {
        cleanupViewBox(node, params.floatPrecision)
        cleanupAttributes(node, params)
        return VisitState.Continue
    }

    private fun cleanupViewBox(node: XastElement, floatPrecision: Int) {
        val viewBox = node.attributes["viewBox"] ?: return
        val numbers = viewBox.trim().split(Regex("""(?:\s,?|,)\s*"""))
        node.attributes["viewBox"] = numbers.joinToString(separator = " ") { value ->
            val num = value.toDoubleOrNull()
            if (num == null) {
                value
            } else {
                formatNumber(roundToFixed(num, floatPrecision))
            }
        }
    }

    private fun cleanupAttributes(node: XastElement, params: Params) {
        val entries = node.attributes.entries.toList()
        for ((attrName, value) in entries) {
            if (attrName == "version") continue
            val match = numericValueRegex.find(value)
            if (match != null) {
                processNumericAttribute(node, attrName, match, params)
            }
        }
    }

    private fun processNumericAttribute(
        node: XastElement,
        attrName: String,
        match: MatchResult,
        params: Params,
    ) {
        val numericStr = match.groupValues[1]
        var num = roundToFixed(numericStr.toDouble(), params.floatPrecision)
        var units = match.groupValues[3]

        if (params.convertToPx && units.isNotEmpty() && units in absoluteLengths) {
            val converted = tryConvertToPx(numericStr, units, params.floatPrecision, match.value)
            if (converted != null) {
                num = converted.first
                units = converted.second
            }
        }

        val str = if (params.leadingZero) {
            removeLeadingZero(num)
        } else {
            formatNumber(num)
        }

        val finalUnits = if (params.defaultPx && units == "px") "" else units
        node.attributes[attrName] = str + finalUnits
    }

    private fun tryConvertToPx(
        numericStr: String,
        units: String,
        floatPrecision: Int,
        originalValue: String,
    ): Pair<Double, String>? {
        val conversionFactor = absoluteLengths.getValue(units)
        val pxNum = roundToFixed(conversionFactor * numericStr.toDouble(), floatPrecision)
        val pxStr = formatNumber(pxNum)
        if (pxStr.length < originalValue.length) {
            return pxNum to "px"
        }
        return null
    }

    private fun roundToFixed(value: Double, precision: Int): Double {
        val factor = 10.0.pow(precision)
        return (value * factor).roundToLong() / factor
    }

    private fun formatNumber(value: Double): String {
        if (value % 1.0 == 0.0 && !value.toString().contains(char = 'E', ignoreCase = true)) {
            return value.toLong().toString()
        }
        return value.toString()
    }

    internal fun removeLeadingZero(value: Double): String {
        val strValue = formatNumber(value)
        return when {
            value > 0.0 && value < 1.0 && strValue.startsWith("0") ->
                strValue.substring(startIndex = 1)
            value > -1.0 && value < 0.0 && strValue.length > 1 && strValue[1] == '0' ->
                strValue[0].toString() + strValue.substring(startIndex = 2)
            else -> strValue
        }
    }
}
