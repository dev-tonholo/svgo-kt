package svgokt.plugins.builtin

import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Rounds numeric values in list-type attribute values to a fixed precision.
 *
 * Affected attributes: `points`, `enable-background`, `viewBox`,
 * `stroke-dasharray`, `dx`, `dy`, `x`, `y`.
 *
 * Also collapses whitespace, removes trailing zeros, optionally strips
 * leading zeros, removes default `px` units, and converts absolute
 * length units to pixels when the result is shorter.
 */
object CleanupListOfValues : Plugin<CleanupListOfValues.Params> {

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

    private val NUMERIC_VALUE_REGEX =
        Regex("""^([-+]?\d*\.?\d+(?:[eE][-+]?\d+)?)(px|pt|pc|mm|cm|in|em|ex|%)?$""")
    private val SEPARATOR_REGEX = Regex("""\s+,?\s*|,\s*""")

    private const val CM_TO_PX = 96.0 / 2.54
    private const val MM_TO_PX = 96.0 / 25.4
    private const val IN_TO_PX = 96.0
    private const val PT_TO_PX = 4.0 / 3.0
    private const val PC_TO_PX = 16.0
    private const val PX_TO_PX = 1.0

    private val ABSOLUTE_LENGTHS = mapOf(
        "cm" to CM_TO_PX,
        "mm" to MM_TO_PX,
        "in" to IN_TO_PX,
        "pt" to PT_TO_PX,
        "pc" to PC_TO_PX,
        "px" to PX_TO_PX,
    )

    private val LIST_ATTRIBUTES = listOf(
        "points",
        "enable-background",
        "viewBox",
        "stroke-dasharray",
        "dx",
        "dy",
        "x",
        "y",
    )

    override val name: String = "cleanupListOfValues"
    override val description: String = "rounds list of values to the fixed precision"
    override val params: Params = Params()

    override val fn: PluginFn = { _, pluginParams, _ ->
        val resolved = pluginParams as? Params ?: Params()
        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    for (attr in LIST_ATTRIBUTES) {
                        val value = node.attributes[attr]
                        if (value != null) {
                            node.attributes[attr] = roundValues(value, resolved)
                        }
                    }
                    VisitState.Continue
                },
            ),
        )
    }

    private fun roundValues(input: String, params: Params): String {
        val roundedList = mutableListOf<String>()
        for (elem in input.split(SEPARATOR_REGEX)) {
            if (elem.isEmpty()) continue
            val match = NUMERIC_VALUE_REGEX.find(elem)
            if (match != null) {
                roundedList.add(processNumericValue(match, params))
            } else if (elem == "new") {
                roundedList.add("new")
            } else {
                roundedList.add(elem)
            }
        }
        return roundedList.joinToString(separator = " ")
    }

    private fun processNumericValue(match: MatchResult, params: Params): String {
        val numStr = match.groupValues[1]
        var num = roundToFixed(numStr.toDouble(), params.floatPrecision)
        var units = match.groupValues[2]

        if (params.convertToPx && units.isNotEmpty() && units in ABSOLUTE_LENGTHS) {
            val factor = ABSOLUTE_LENGTHS.getValue(units)
            val pxNum = roundToFixed(factor * numStr.toDouble(), params.floatPrecision)
            val pxStr = formatNumber(pxNum)
            if (pxStr.length < match.value.length) {
                num = pxNum
                units = "px"
            }
        }

        val str = if (params.leadingZero) removeLeadingZero(num) else formatNumber(num)
        val finalUnits = if (params.defaultPx && units == "px") "" else units
        return str + finalUnits
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

    private fun removeLeadingZero(value: Double): String {
        val str = formatNumber(value)
        return when {
            value > 0.0 && value < 1.0 && str.startsWith("0") ->
                str.substring(startIndex = 1)
            value > -1.0 && value < 0.0 && str.length > 1 && str[1] == '0' ->
                str[0].toString() + str.substring(startIndex = 2)
            else -> str
        }
    }
}
