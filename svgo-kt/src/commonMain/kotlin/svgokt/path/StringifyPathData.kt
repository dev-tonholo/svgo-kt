package svgokt.path

import kotlin.math.pow
import kotlin.math.round

private fun toFixed(num: Double, precision: Int): Double {
    val pow = 10.0.pow(precision)
    return round(num * pow) / pow
}

private fun isInteger(value: Double): Boolean = value % 1.0 == 0.0

/**
 * Converts a [Double] to string matching JS Number.toString() behavior,
 * then removes leading zeros for values between -1 and 1.
 *
 * In JS, integer-valued doubles print without the ".0" suffix (e.g. 5 not 5.0).
 */
private fun removeLeadingZero(value: Double): String {
    // Match JS Number.toString(): integers print without ".0"
    val strValue = if (isInteger(value) && !value.toString().contains('E', ignoreCase = true)) {
        value.toLong().toString()
    } else {
        value.toString()
    }

    return when {
        value > 0.0 && value < 1.0 && strValue.startsWith('0') ->
            strValue.substring(startIndex = 1)

        value > -1.0 && value < 0.0 && strValue.length > 1 && strValue[1] == '0' ->
            strValue[0].toString() + strValue.substring(startIndex = 2)

        else -> strValue
    }
}

private data class RoundResult(
    val roundedStr: String,
    val rounded: Double,
)

private fun roundAndStringify(number: Double, precision: Int?): RoundResult {
    val rounded = if (precision != null) toFixed(number, precision) else number
    return RoundResult(
        roundedStr = removeLeadingZero(rounded),
        rounded = rounded,
    )
}

private fun isDigitChar(c: Char): Boolean = c in '0'..'9'

/** Number of arguments per arc command. */
private const val ARC_ARGS_COUNT = 7

/** Index of the large-arc flag within an arc command's arguments. */
private const val ARC_LARGE_ARC_FLAG_INDEX = 4

/** Index of the sweep flag within an arc command's arguments. */
private const val ARC_SWEEP_FLAG_INDEX = 5

@Suppress("CyclomaticComplexity")
private fun stringifyArgs(
    command: Char,
    args: List<Double>,
    precision: Int?,
    disableSpaceAfterFlags: Boolean,
): String {
    val result = StringBuilder()
    var previous: Double? = null

    for (i in args.indices) {
        val (roundedStr, rounded) = roundAndStringify(args[i], precision)

        if (disableSpaceAfterFlags &&
            (command == 'A' || command == 'a') &&
            // consider combined arcs
            (i % ARC_ARGS_COUNT == ARC_LARGE_ARC_FLAG_INDEX || i % ARC_ARGS_COUNT == ARC_SWEEP_FLAG_INDEX)
        ) {
            result.append(roundedStr)
        } else if (i == 0 || rounded < 0) {
            // avoid space before first and negative numbers
            result.append(roundedStr)
        } else if (previous != null && !isInteger(previous) && roundedStr.isNotEmpty() && !isDigitChar(roundedStr[0])) {
            // remove space before decimal with zero whole
            // only when previous number is also decimal
            result.append(roundedStr)
        } else {
            result.append(' ')
            result.append(roundedStr)
        }
        previous = rounded
    }

    return result.toString()
}

@Suppress("CyclomaticComplexity", "CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth")
fun stringifyPathData(
    pathData: List<PathDataItem>,
    precision: Int? = null,
    disableSpaceAfterFlags: Boolean = false,
): String {
    if (pathData.isEmpty()) {
        return ""
    }

    if (pathData.size == 1) {
        val item = pathData[0]
        return item.command.toString() + stringifyArgs(
            command = item.command,
            args = item.args,
            precision = precision,
            disableSpaceAfterFlags = disableSpaceAfterFlags,
        )
    }

    var result = ""
    var prevCommand = pathData[0].command
    var prevArgs = pathData[0].args.toMutableList()

    // match leading moveto with following lineto
    if (pathData[1].command == 'L') {
        prevCommand = 'M'
    } else if (pathData[1].command == 'l') {
        prevCommand = 'm'
    }

    for (i in 1 until pathData.size) {
        val command = pathData[i].command
        val args = pathData[i].args

        val canCombine = (prevCommand == command && prevCommand != 'M' && prevCommand != 'm') ||
            // combine matching moveto and lineto sequences
            (prevCommand == 'M' && command == 'L') ||
            (prevCommand == 'm' && command == 'l')

        if (canCombine) {
            prevArgs.addAll(args)
            if (i == pathData.size - 1) {
                result += prevCommand.toString() + stringifyArgs(
                    command = prevCommand,
                    args = prevArgs,
                    precision = precision,
                    disableSpaceAfterFlags = disableSpaceAfterFlags,
                )
            }
        } else {
            result += prevCommand.toString() + stringifyArgs(
                command = prevCommand,
                args = prevArgs,
                precision = precision,
                disableSpaceAfterFlags = disableSpaceAfterFlags,
            )

            if (i == pathData.size - 1) {
                result += command.toString() + stringifyArgs(
                    command = command,
                    args = args,
                    precision = precision,
                    disableSpaceAfterFlags = disableSpaceAfterFlags,
                )
            } else {
                prevCommand = command
                prevArgs = args.toMutableList()
            }
        }
    }

    return result
}
