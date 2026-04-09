package svgokt.transform

/**
 * Convert transforms JS representation to string.
 * Mirrors JS `js2transform` in `_transforms.js`.
 *
 * Rounds each transform before stringifying, then joins with no separator.
 */
fun js2transform(
    transforms: List<TransformItem>,
    params: TransformParams,
): String {
    return transforms.joinToString(separator = "") { transform ->
        val rounded = roundTransform(transform = transform, params = params)
        "${rounded.name}(${cleanupOutData(data = rounded.data, params = params)})"
    }
}

/**
 * Convert a row of numbers to an optimized string view.
 * Mirrors JS `cleanupOutData` from `tools.js` (for transform context, no arc flags).
 */
internal fun cleanupOutData(
    data: DoubleArray,
    params: TransformParams,
): String {
    val sb = StringBuilder()
    var prev = Double.NaN

    for (i in data.indices) {
        val item = data[i]
        var delimiter = if (i == 0) "" else " "

        val itemStr = if (params.leadingZero) {
            removeLeadingZero(value = item)
        } else {
            formatDouble(value = item)
        }

        if (params.negativeExtraSpace && delimiter.isNotEmpty() &&
            (item < 0 || (itemStr.startsWith(".") && prev % 1.0 != 0.0))
        ) {
            delimiter = ""
        }

        prev = item
        sb.append(delimiter)
        sb.append(itemStr)
    }
    return sb.toString()
}

/**
 * Remove floating-point numbers leading zero.
 * 0.5 -> .5, -0.5 -> -.5
 */
internal fun removeLeadingZero(value: Double): String {
    val str = formatDouble(value = value)
    if (value > 0.0 && value < 1.0 && str.startsWith("0")) {
        return str.substring(startIndex = 1)
    }
    if (value > -1.0 && value < 0.0 && str.length > 1 && str[1] == '0') {
        return str[0].toString() + str.substring(startIndex = 2)
    }
    return str
}

/**
 * Format a double to string matching JS Number.toString() behavior.
 * Whole numbers render without decimal point. Small decimals use standard notation.
 */
internal fun formatDouble(value: Double): String {
    // If value is a whole number, render as integer
    if (value == value.toLong().toDouble() && value in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) {
        return value.toLong().toString()
    }
    // Otherwise use Kotlin's default toString which matches JS for most cases
    return value.toString()
}
