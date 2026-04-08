package svgokt.transform

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

private const val DEGREES_TO_RADIANS = PI / 180.0

/**
 * Convert degrees to radians.
 */
private fun degreesToRadians(degrees: Double): Double = degrees * DEGREES_TO_RADIANS

/**
 * Cosine of an angle given in degrees.
 */
private fun cosDeg(degrees: Double): Double = cos(degreesToRadians(degrees))

/**
 * Sine of an angle given in degrees.
 */
private fun sinDeg(degrees: Double): Double = sin(degreesToRadians(degrees))

/**
 * Tangent of an angle given in degrees.
 */
private fun tanDeg(degrees: Double): Double = tan(degreesToRadians(degrees))

/**
 * Convert a [TransformItem] to its 6-element affine matrix representation.
 *
 * Matrix format: `[a, b, c, d, e, f]` representing:
 * ```
 * | a  c  e |
 * | b  d  f |
 * | 0  0  1 |
 * ```
 *
 * @param transform the transform item to convert.
 * @return a 6-element [DoubleArray] representing the affine matrix.
 * @throws IllegalArgumentException if the transform name is unknown.
 */
@Suppress("MagicNumber")
fun transformToMatrix(transform: TransformItem): DoubleArray {
    if (transform.name == "matrix") {
        return transform.data
    }
    return when (transform.name) {
        "translate" -> doubleArrayOf(
            1.0,
            0.0,
            0.0,
            1.0,
            transform.data[0],
            transform.data.getOrElse(index = 1) { 0.0 },
        )

        "scale" -> {
            val sx = transform.data[0]
            val sy = transform.data.getOrElse(index = 1) { sx }
            doubleArrayOf(sx, 0.0, 0.0, sy, 0.0, 0.0)
        }

        "rotate" -> {
            val angleDeg = transform.data[0]
            val cosA = cosDeg(angleDeg)
            val sinA = sinDeg(angleDeg)
            val cx = transform.data.getOrElse(index = 1) { 0.0 }
            val cy = transform.data.getOrElse(index = 2) { 0.0 }
            doubleArrayOf(
                cosA,
                sinA,
                -sinA,
                cosA,
                (1 - cosA) * cx + sinA * cy,
                (1 - cosA) * cy - sinA * cx,
            )
        }

        "skewX" -> doubleArrayOf(
            1.0,
            0.0,
            tanDeg(transform.data[0]),
            1.0,
            0.0,
            0.0,
        )

        "skewY" -> doubleArrayOf(
            1.0,
            tanDeg(transform.data[0]),
            0.0,
            1.0,
            0.0,
            0.0,
        )

        else -> throw IllegalArgumentException("Unknown transform: ${transform.name}")
    }
}

/**
 * Multiply two 6-element affine transformation matrices.
 *
 * Each matrix is `[a, b, c, d, e, f]` representing:
 * ```
 * | a  c  e |
 * | b  d  f |
 * | 0  0  1 |
 * ```
 *
 * @param a the first (left) matrix.
 * @param b the second (right) matrix.
 * @return the product matrix as a 6-element [DoubleArray].
 */
@Suppress("MagicNumber")
fun multiplyTransformMatrices(a: DoubleArray, b: DoubleArray): DoubleArray {
    return doubleArrayOf(
        a[0] * b[0] + a[2] * b[1],
        a[1] * b[0] + a[3] * b[1],
        a[0] * b[2] + a[2] * b[3],
        a[1] * b[2] + a[3] * b[3],
        a[0] * b[4] + a[2] * b[5] + a[4],
        a[1] * b[4] + a[3] * b[5] + a[5],
    )
}

/**
 * Collapse a list of transforms into a single matrix transform
 * by converting each to a matrix and multiplying them together.
 *
 * @param transforms the list of transforms to multiply.
 * @return a single [TransformItem] with name "matrix" and the resulting data.
 */
fun transformsMultiply(transforms: List<TransformItem>): TransformItem {
    val matrices = transforms.map { transform ->
        if (transform.name == "matrix") {
            transform.data
        } else {
            transformToMatrix(transform = transform)
        }
    }

    val resultData = if (matrices.isNotEmpty()) {
        matrices.reduce { acc, matrix ->
            multiplyTransformMatrices(a = acc, b = matrix)
        }
    } else {
        doubleArrayOf()
    }

    return TransformItem(name = "matrix", data = resultData)
}
