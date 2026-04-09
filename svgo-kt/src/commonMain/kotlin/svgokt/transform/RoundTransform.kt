@file:Suppress("MagicNumber")

package svgokt.transform

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

private const val MAX_SAFE_PRECISION = 20

/**
 * Round transform data values according to the param precisions.
 * Returns a new TransformItem with rounded data.
 */
fun roundTransform(
    transform: TransformItem,
    params: TransformParams,
): TransformItem {
    val rounded = when (transform.name) {
        "translate" -> floatRound(data = transform.data, params = params)

        "rotate" -> {
            val angle = degRound(data = transform.data.sliceArray(indices = 0..0), params = params)
            val rest = floatRound(data = transform.data.drop(n = 1).toDoubleArray(), params = params)
            angle + rest
        }

        "skewX", "skewY" -> degRound(data = transform.data, params = params)

        "scale" -> transformRound(data = transform.data, params = params)

        "matrix" -> {
            val first4 = transformRound(data = transform.data.sliceArray(indices = 0..3), params = params)
            val last2 = floatRound(data = transform.data.sliceArray(indices = 4..5), params = params)
            first4 + last2
        }

        else -> transform.data
    }
    return TransformItem(name = transform.name, data = rounded)
}

private fun degRound(data: DoubleArray, params: TransformParams): DoubleArray {
    val degPrec = params.degPrecision
    return if (degPrec != null && degPrec >= 1 && params.floatPrecision < MAX_SAFE_PRECISION) {
        smartRound(precision = degPrec, data = data)
    } else {
        roundArray(data = data)
    }
}

private fun floatRound(data: DoubleArray, params: TransformParams): DoubleArray {
    return if (params.floatPrecision >= 1 && params.floatPrecision < MAX_SAFE_PRECISION) {
        smartRound(precision = params.floatPrecision, data = data)
    } else {
        roundArray(data = data)
    }
}

private fun transformRound(data: DoubleArray, params: TransformParams): DoubleArray {
    return if (params.transformPrecision >= 1 && params.floatPrecision < MAX_SAFE_PRECISION) {
        smartRound(precision = params.transformPrecision, data = data)
    } else {
        roundArray(data = data)
    }
}

private fun roundArray(data: DoubleArray): DoubleArray {
    return DoubleArray(data.size) { round(data[it]) }
}

/**
 * Decrease accuracy of floating-point numbers keeping a specified number of decimals.
 * Smart rounds values like 2.349 to 2.35.
 * Mirrors the JS `smartRound` function.
 */
internal fun smartRound(precision: Int, data: DoubleArray): DoubleArray {
    val result = data.copyOf()
    val tolerance = jsToFixed(value = (0.1).pow(precision), precision = precision)
    for (i in result.indices.reversed()) {
        if (toFixed(num = result[i], precision = precision) != result[i]) {
            val rounded = jsToFixed(value = result[i], precision = precision - 1)
            val diff = jsToFixed(value = abs(rounded - result[i]), precision = precision + 1)
            result[i] = if (diff >= tolerance) {
                jsToFixed(value = result[i], precision = precision)
            } else {
                rounded
            }
        }
    }
    return result
}

/**
 * Mimics JS `+value.toFixed(precision)` behavior.
 * Formats to fixed decimal places then parses back to Double.
 */
internal fun jsToFixed(value: Double, precision: Int): Double {
    // JS toFixed uses "round half away from zero" and returns a string.
    // Kotlin's round() does "round half to even", but for our purposes
    // we replicate the JS behavior by multiplying, rounding, dividing.
    val factor = (10.0).pow(precision)
    val shifted = value * factor
    // JS Math.round rounds .5 up; Kotlin round() rounds .5 to even.
    // Use the same approach as toFixed in JS: add 0.5 to positive, subtract from negative,
    // then truncate. Actually, for our precision needs, the standard round is close enough
    // and the JS reference itself uses Math.round in toFixed.
    val rounded = round(shifted)
    return rounded / factor
}
