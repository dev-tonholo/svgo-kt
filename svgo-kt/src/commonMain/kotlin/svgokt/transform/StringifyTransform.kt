package svgokt.transform

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToLong

private const val DEFAULT_PRECISION = 4

/**
 * Convert a list of [TransformItem] back to an SVG transform attribute string.
 *
 * Values are rounded to the given [precision]. Whole numbers are rendered without
 * a decimal point (e.g., `10` instead of `10.0`).
 *
 * @param transforms the list of transform items to stringify.
 * @param precision the number of decimal places for rounding (default 4).
 * @return the SVG transform string, e.g. `"translate(10 20) rotate(45)"`.
 */
fun stringifyTransform(
    transforms: List<TransformItem>,
    precision: Int = DEFAULT_PRECISION,
): String {
    return transforms.joinToString(separator = " ") { transform ->
        val values = transform.data.joinToString(separator = " ") { value ->
            formatNumber(value = value, precision = precision)
        }
        "${transform.name}($values)"
    }
}

/**
 * Format a double value with the given precision, removing trailing zeros
 * and unnecessary decimal points.
 */
private fun formatNumber(value: Double, precision: Int): String {
    val rounded = roundToPrecision(value = value, precision = precision)
    val longValue = rounded.roundToLong()
    // If the rounded value is effectively a whole number, render as integer.
    return if (rounded == longValue.toDouble() && abs(rounded) < Long.MAX_VALUE) {
        longValue.toString()
    } else {
        // Format with the given precision, then remove trailing zeros.
        rounded.toString()
            .let { str ->
                if ('.' in str) str.trimEnd('0').trimEnd('.') else str
            }
    }
}

/**
 * Round a double to the specified number of decimal places.
 */
private fun roundToPrecision(value: Double, precision: Int): Double {
    val factor = 10.0.pow(n = precision)
    return round(value * factor) / factor
}
