package svgokt.plugins.builtin

import svgokt.Tools
import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections
import kotlin.math.roundToInt

/**
 * Converts color values in SVG attributes to shorter forms.
 *
 * Key conversions performed:
 * - Named colors to hex when shorter (e.g. fuchsia -> #f0f)
 * - rgb(r,g,b) to hex (e.g. rgb(255,0,0) -> #FF0000 -> #f00)
 * - Long hex to short hex (e.g. #ff0000 -> #f00)
 * - Hex to short named color when shorter (e.g. #f00 -> red)
 * - Optional case conversion (lower/upper)
 */
object ConvertColors : Plugin<ConvertColors.Params> {
    data class Params(
        val names2hex: Boolean = true,
        val rgb2hex: Boolean = true,
        val convertCase: ConvertCase = ConvertCase.LOWER,
        val shorthex: Boolean = true,
        val shortname: Boolean = true,
    ) : PluginParams,
        Map<String, Any> by mapOf(
            "names2hex" to names2hex,
            "rgb2hex" to rgb2hex,
            "convertCase" to convertCase,
            "shorthex" to shorthex,
            "shortname" to shortname,
        )

    enum class ConvertCase { LOWER, UPPER, NONE }

    override val name: String = "convertColors"
    override val description: String =
        "converts colors: rgb() to #rrggbb and #rrggbb to #rgb"
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

    private const val R_NUMBER = "([+-]?(?:\\d*\\.\\d+|\\d+\\.?)%?)"
    private const val R_COMMA = "(?:\\s*,\\s*|\\s+)"
    private val regRGB = Regex(
        "^rgb\\(\\s*$R_NUMBER$R_COMMA$R_NUMBER$R_COMMA$R_NUMBER\\s*\\)$",
    )
    private val regHEX = Regex("^#(([a-fA-F0-9])\\2){3}$")

    private const val PERCENT_TO_BYTE = 2.55f
    private const val MAX_COLOR_VALUE = 255
    private const val HEX_BASE_OFFSET = 256
    private const val BYTE_SHIFT = 8

    /** Indices for the R, G, B hex digits in a #RRGGBB string. */
    private const val SHORT_HEX_R_INDEX = 1
    private const val SHORT_HEX_G_INDEX = 3
    private const val SHORT_HEX_B_INDEX = 5

    @Suppress("UnusedParameter")
    private fun onEnter(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
    ): VisitState {
        for ((attrName, attrValue) in node.attributes) {
            if (attrName !in Collections.colorsProps) continue

            var value = attrValue
            value = convertNameToHex(value, params)
            value = convertRgbToHex(value, params)
            value = applyCase(value, params)
            value = shortenHex(value, params)
            value = shortenToName(value, params)

            node.attributes[attrName] = value
        }
        return VisitState.Continue
    }

    private fun convertNameToHex(value: String, params: Params): String {
        if (!params.names2hex) return value
        val colorName = value.lowercase()
        return Collections.colorsNames[colorName] ?: value
    }

    private fun convertRgbToHex(value: String, params: Params): String {
        if (!params.rgb2hex) return value
        val match = regRGB.matchEntire(value) ?: return value
        val numbers = (1..3).map { index ->
            val raw = match.groupValues[index]
            val n = if (raw.contains('%')) {
                (raw.removeSuffix("%").toFloat() * PERCENT_TO_BYTE).roundToInt()
            } else {
                raw.toInt()
            }
            n.coerceIn(minimumValue = 0, maximumValue = MAX_COLOR_VALUE)
        }
        return rgbToHex(numbers)
    }

    private fun rgbToHex(rgb: List<Int>): String {
        val hexNumber = ((HEX_BASE_OFFSET + rgb[0]) shl BYTE_SHIFT or rgb[1]) shl BYTE_SHIFT or rgb[2]
        return "#" + hexNumber.toString(radix = 16).substring(startIndex = 1).uppercase()
    }

    private fun applyCase(value: String, params: Params): String {
        if (params.convertCase == ConvertCase.NONE) return value
        if (Tools.includesUrlReference(value)) return value
        if (value == "currentColor") return value
        return when (params.convertCase) {
            ConvertCase.LOWER -> value.lowercase()
            ConvertCase.UPPER -> value.uppercase()
            ConvertCase.NONE -> value
        }
    }

    private fun shortenHex(value: String, params: Params): String {
        if (!params.shorthex) return value
        val match = regHEX.matchEntire(value) ?: return value
        return "#" + match.value[SHORT_HEX_R_INDEX].toString() +
            match.value[SHORT_HEX_G_INDEX].toString() +
            match.value[SHORT_HEX_B_INDEX].toString()
    }

    private fun shortenToName(value: String, params: Params): String {
        if (!params.shortname) return value
        val colorName = value.lowercase()
        return Collections.colorsShortNames[colorName] ?: value
    }
}
