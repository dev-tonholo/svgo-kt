package svgokt.path

/** Number of coordinate arguments for moveto (M/m). */
private const val MOVETO_ARGS = 2

/** Number of coordinate arguments for lineto (L/l). */
private const val LINETO_ARGS = 2

/** Number of coordinate arguments for horizontal lineto (H/h). */
private const val HLINETO_ARGS = 1

/** Number of coordinate arguments for vertical lineto (V/v). */
private const val VLINETO_ARGS = 1

/** Number of coordinate arguments for curveto (C/c). */
private const val CURVETO_ARGS = 6

/** Number of coordinate arguments for smooth curveto (S/s). */
private const val SMOOTH_CURVETO_ARGS = 4

/** Number of coordinate arguments for quadratic curveto (Q/q). */
private const val QUAD_CURVETO_ARGS = 4

/** Number of coordinate arguments for smooth quadratic curveto (T/t). */
private const val SMOOTH_QUAD_ARGS = 2

/** Number of coordinate arguments for arc (A/a). */
private const val ARC_ARGS = 7

/** Arc argument index: x-axis rotation. */
private const val ARC_ROTATION_INDEX = 2

/** Arc argument index: large-arc flag. */
private const val ARC_LARGE_ARC_INDEX = 3

/** Arc argument index: sweep flag. */
private const val ARC_SWEEP_INDEX = 4

/** Arc argument index: end x. */
private const val ARC_END_X_INDEX = 5

/** Arc argument index: end y. */
private const val ARC_END_Y_INDEX = 6

private val argsCountPerCommand: Map<Char, Int> = mapOf(
    'M' to MOVETO_ARGS, 'm' to MOVETO_ARGS,
    'Z' to 0, 'z' to 0,
    'L' to LINETO_ARGS, 'l' to LINETO_ARGS,
    'H' to HLINETO_ARGS, 'h' to HLINETO_ARGS,
    'V' to VLINETO_ARGS, 'v' to VLINETO_ARGS,
    'C' to CURVETO_ARGS, 'c' to CURVETO_ARGS,
    'S' to SMOOTH_CURVETO_ARGS, 's' to SMOOTH_CURVETO_ARGS,
    'Q' to QUAD_CURVETO_ARGS, 'q' to QUAD_CURVETO_ARGS,
    'T' to SMOOTH_QUAD_ARGS, 't' to SMOOTH_QUAD_ARGS,
    'A' to ARC_ARGS, 'a' to ARC_ARGS,
)

private fun isCommand(c: Char): Boolean = c in argsCountPerCommand

private fun isWhiteSpace(c: Char): Boolean =
    c == ' ' || c == '\t' || c == '\r' || c == '\n'

private fun isDigit(c: Char): Boolean = c in '0'..'9'

private enum class ReadNumberState {
    NONE,
    SIGN,
    WHOLE,
    DECIMAL_POINT,
    DECIMAL,
    E,
    EXPONENT_SIGN,
    EXPONENT,
}

private data class ReadNumberResult(
    val cursor: Int,
    val number: Double?,
)

@Suppress("CyclomaticComplexity", "CyclomaticComplexMethod", "LoopWithTooManyJumpStatements")
private fun readNumber(string: String, cursor: Int): ReadNumberResult {
    var i = cursor
    val value = StringBuilder()
    var state = ReadNumberState.NONE

    while (i < string.length) {
        val c = string[i]
        if (c == '+' || c == '-') {
            if (state == ReadNumberState.NONE) {
                state = ReadNumberState.SIGN
                value.append(c)
                i++
                continue
            }
            if (state == ReadNumberState.E) {
                state = ReadNumberState.EXPONENT_SIGN
                value.append(c)
                i++
                continue
            }
        }
        if (isDigit(c)) {
            when (state) {
                ReadNumberState.NONE, ReadNumberState.SIGN, ReadNumberState.WHOLE -> {
                    state = ReadNumberState.WHOLE
                    value.append(c)
                    i++
                    continue
                }
                ReadNumberState.DECIMAL_POINT, ReadNumberState.DECIMAL -> {
                    state = ReadNumberState.DECIMAL
                    value.append(c)
                    i++
                    continue
                }
                ReadNumberState.E, ReadNumberState.EXPONENT_SIGN, ReadNumberState.EXPONENT -> {
                    state = ReadNumberState.EXPONENT
                    value.append(c)
                    i++
                    continue
                }
            }
        }
        if (c == '.') {
            if (state == ReadNumberState.NONE ||
                state == ReadNumberState.SIGN ||
                state == ReadNumberState.WHOLE
            ) {
                state = ReadNumberState.DECIMAL_POINT
                value.append(c)
                i++
                continue
            }
        }
        if (c == 'E' || c == 'e') {
            if (state == ReadNumberState.WHOLE ||
                state == ReadNumberState.DECIMAL_POINT ||
                state == ReadNumberState.DECIMAL
            ) {
                state = ReadNumberState.E
                value.append(c)
                i++
                continue
            }
        }
        break
    }

    val number = value.toString().toDoubleOrNull()
    return if (number == null) {
        ReadNumberResult(cursor = cursor, number = null)
    } else {
        // step back to delegate iteration to parent loop
        ReadNumberResult(cursor = i - 1, number = number)
    }
}

@Suppress(
    "CyclomaticComplexity",
    "CyclomaticComplexMethod",
    "LongMethod",
    "LoopWithTooManyJumpStatements",
    "NestedBlockDepth",
    "ReturnCount",
)
fun parsePathData(string: String): List<PathDataItem> {
    val pathData = mutableListOf<PathDataItem>()
    var command: Char? = null
    var args = mutableListOf<Double>()
    var argsCount = 0
    var canHaveComma = false
    var hadComma = false
    var i = 0

    while (i < string.length) {
        val c = string[i]

        if (isWhiteSpace(c)) {
            i++
            continue
        }

        // allow comma only between arguments
        if (canHaveComma && c == ',') {
            if (hadComma) {
                break
            }
            hadComma = true
            i++
            continue
        }

        if (isCommand(c)) {
            if (hadComma) {
                return pathData
            }
            if (command == null) {
                // moveto should be leading command
                if (c != 'M' && c != 'm') {
                    return pathData
                }
            } else if (args.isNotEmpty()) {
                // stop if previous command arguments are not flushed
                return pathData
            }
            command = c
            args = mutableListOf()
            argsCount = argsCountPerCommand.getValue(command)
            canHaveComma = false
            // flush command without arguments
            if (argsCount == 0) {
                pathData.add(PathDataItem(command = command, args = args))
            }
            i++
            continue
        }

        // avoid parsing arguments if no command detected
        if (command == null) {
            return pathData
        }

        // read next argument
        var newCursor = i
        var number: Double? = null

        if (command == 'A' || command == 'a') {
            val position = args.size
            if (position == 0 || position == 1) {
                // allow only positive number without sign as first two arguments
                if (c != '+' && c != '-') {
                    val result = readNumber(string, i)
                    newCursor = result.cursor
                    number = result.number
                }
            }
            if (position == ARC_ROTATION_INDEX ||
                position == ARC_END_X_INDEX ||
                position == ARC_END_Y_INDEX
            ) {
                val result = readNumber(string, i)
                newCursor = result.cursor
                number = result.number
            }
            if (position == ARC_LARGE_ARC_INDEX || position == ARC_SWEEP_INDEX) {
                // read flags
                if (c == '0') {
                    number = 0.0
                }
                if (c == '1') {
                    number = 1.0
                }
            }
        } else {
            val result = readNumber(string, i)
            newCursor = result.cursor
            number = result.number
        }

        if (number == null) {
            return pathData
        }

        args.add(number)
        canHaveComma = true
        hadComma = false
        i = newCursor

        // flush arguments when necessary count is reached
        if (args.size == argsCount) {
            pathData.add(PathDataItem(command = command, args = args))
            // subsequent moveto coordinates are treated as implicit lineto commands
            if (command == 'M') {
                command = 'L'
            }
            if (command == 'm') {
                command = 'l'
            }
            args = mutableListOf()
        }

        i++
    }

    return pathData
}
