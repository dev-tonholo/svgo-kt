@file:Suppress("MagicNumber")

package svgokt.transform

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Decompose a matrix transform into a list of simple transforms using both
 * QRAB and QRCD decomposition, returning whichever produces the shortest
 * stringified form.
 *
 * Mirrors JS `matrixToTransform` in `_transforms.js`.
 */
fun matrixToTransform(
    origMatrix: TransformItem,
    params: TransformParams,
): List<TransformItem> {
    val decompositions = getDecompositions(matrix = origMatrix)

    var shortest: List<TransformItem>? = null
    var shortestLen = Int.MAX_VALUE

    for (decomposition in decompositions) {
        val roundedTransforms = decomposition.map { item ->
            roundTransform(
                transform = TransformItem(name = item.name, data = item.data.copyOf()),
                params = params,
            )
        }

        val optimized = optimize(roundedTransforms = roundedTransforms, rawTransforms = decomposition)
        val len = js2transform(transforms = optimized, params = params).length
        if (len < shortestLen) {
            shortest = optimized
            shortestLen = len
        }
    }

    return shortest ?: listOf(origMatrix)
}

private fun getDecompositions(matrix: TransformItem): List<List<TransformItem>> {
    val decompositions = mutableListOf<List<TransformItem>>()
    val qrab = decomposeQRAB(matrix = matrix)
    val qrcd = decomposeQRCD(matrix = matrix)
    if (qrab != null) decompositions.add(qrab)
    if (qrcd != null) decompositions.add(qrcd)
    return decompositions
}

/**
 * QRAB decomposition.
 * @see https://frederic-wang.fr/2013/12/01/decomposition-of-2d-transform-matrices/
 */
private fun decomposeQRAB(matrix: TransformItem): List<TransformItem>? {
    val data = matrix.data
    val a = data[0]
    val b = data[1]
    val c = data[2]
    val d = data[3]
    val e = data[4]
    val f = data[5]

    val delta = a * d - b * c
    if (delta == 0.0) return null

    val r = hypot(a, b)
    if (r == 0.0) return null

    val decomposition = mutableListOf<TransformItem>()
    val cosOfRotationAngle = a / r

    if (e != 0.0 || f != 0.0) {
        decomposition.add(TransformItem(name = "translate", data = doubleArrayOf(e, f)))
    }

    if (cosOfRotationAngle != 1.0) {
        val rotationAngleRads = acos(cosOfRotationAngle)
        val angle = radiansToDegrees(if (b < 0) -rotationAngleRads else rotationAngleRads)
        decomposition.add(TransformItem(name = "rotate", data = doubleArrayOf(angle, 0.0, 0.0)))
    }

    val sx = r
    val sy = delta / sx
    if (sx != 1.0 || sy != 1.0) {
        decomposition.add(TransformItem(name = "scale", data = doubleArrayOf(sx, sy)))
    }

    val acPlusBd = a * c + b * d
    if (acPlusBd != 0.0) {
        decomposition.add(
            TransformItem(
                name = "skewX",
                data = doubleArrayOf(radiansToDegrees(atan(acPlusBd / (a * a + b * b)))),
            ),
        )
    }

    return decomposition
}

/**
 * QRCD decomposition.
 * @see https://frederic-wang.fr/2013/12/01/decomposition-of-2d-transform-matrices/
 */
private fun decomposeQRCD(matrix: TransformItem): List<TransformItem>? {
    val data = matrix.data
    val a = data[0]
    val b = data[1]
    val c = data[2]
    val d = data[3]
    val e = data[4]
    val f = data[5]

    val delta = a * d - b * c
    if (delta == 0.0) return null

    val s = hypot(c, d)
    if (s == 0.0) return null

    val decomposition = mutableListOf<TransformItem>()

    if (e != 0.0 || f != 0.0) {
        decomposition.add(TransformItem(name = "translate", data = doubleArrayOf(e, f)))
    }

    val rotationAngleRads = PI / 2.0 - (if (d < 0) -1.0 else 1.0) * acos(-c / s)
    decomposition.add(
        TransformItem(
            name = "rotate",
            data = doubleArrayOf(radiansToDegrees(rotationAngleRads), 0.0, 0.0),
        ),
    )

    val sx = delta / s
    val sy = s
    if (sx != 1.0 || sy != 1.0) {
        decomposition.add(TransformItem(name = "scale", data = doubleArrayOf(sx, sy)))
    }

    val acPlusBd = a * c + b * d
    if (acPlusBd != 0.0) {
        decomposition.add(
            TransformItem(
                name = "skewY",
                data = doubleArrayOf(radiansToDegrees(atan(acPlusBd / (c * c + d * d)))),
            ),
        )
    }

    return decomposition
}

/**
 * Convert translate(tx,ty)rotate(a) to rotate(a,cx,cy).
 */
private fun mergeTranslateAndRotate(tx: Double, ty: Double, a: Double): TransformItem {
    val rotationAngleRads = degreesToRadians(a)
    val dVal = 1.0 - cos(rotationAngleRads)
    val eVal = sin(rotationAngleRads)
    val cy = (dVal * ty + eVal * tx) / (dVal * dVal + eVal * eVal)
    val cx = (tx - eVal * cy) / dVal
    return TransformItem(name = "rotate", data = doubleArrayOf(a, cx, cy))
}

private fun isIdentityTransformStrict(t: TransformItem): Boolean {
    return when (t.name) {
        "rotate", "skewX", "skewY" -> t.data[0] == 0.0
        "scale" -> t.data[0] == 1.0 && t.data[1] == 1.0
        "translate" -> t.data[0] == 0.0 && t.data[1] == 0.0
        else -> false
    }
}

/**
 * Optimize a list of simple transforms. Merges translate+rotate into rotate(a,cx,cy),
 * replaces rotate(180) with scale(-1), etc.
 *
 * Mirrors JS `optimize` in `_transforms.js`.
 */
private fun optimize(
    roundedTransforms: List<TransformItem>,
    rawTransforms: List<TransformItem>,
): List<TransformItem> {
    val optimized = mutableListOf<TransformItem>()
    var index = 0

    while (index < roundedTransforms.size) {
        val rt = roundedTransforms[index]

        if (isIdentityTransformStrict(rt)) {
            index++
            continue
        }

        when (rt.name) {
            "rotate" -> {
                when (rt.data[0]) {
                    180.0, -180.0 -> {
                        val next = roundedTransforms.getOrNull(index + 1)
                        if (next != null && next.name == "scale") {
                            optimized.add(createScaleTransform(data = next.data.map { -it }.toDoubleArray()))
                            index += 2
                        } else {
                            optimized.add(TransformItem(name = "scale", data = doubleArrayOf(-1.0)))
                            index++
                        }
                        continue
                    }
                }
                val data = rt.data
                val sliceEnd = if (data.getOrElse(index = 1) { 0.0 } != 0.0 ||
                    data.getOrElse(index = 2) { 0.0 } != 0.0
                ) {
                    3
                } else {
                    1
                }
                optimized.add(TransformItem(name = "rotate", data = data.sliceArray(indices = 0 until sliceEnd)))
            }

            "scale" -> optimized.add(createScaleTransform(data = rt.data))

            "skewX", "skewY" -> {
                optimized.add(TransformItem(name = rt.name, data = doubleArrayOf(rt.data[0])))
            }

            "translate" -> {
                val next = roundedTransforms.getOrNull(index + 1)
                if (next != null && next.name == "rotate" &&
                    next.data[0] != 180.0 && next.data[0] != -180.0 &&
                    next.data[0] != 0.0 &&
                    next.data[1] == 0.0 && next.data[2] == 0.0
                ) {
                    val rawData = rawTransforms[index].data
                    optimized.add(
                        mergeTranslateAndRotate(
                            tx = rawData[0],
                            ty = rawData[1],
                            a = rawTransforms[index + 1].data[0],
                        ),
                    )
                    index += 2
                    continue
                }
                val data = rt.data
                val sliceEnd = if (data.getOrElse(index = 1) { 0.0 } != 0.0) 2 else 1
                optimized.add(TransformItem(name = "translate", data = data.sliceArray(indices = 0 until sliceEnd)))
            }
        }
        index++
    }

    return if (optimized.isNotEmpty()) {
        optimized
    } else {
        listOf(TransformItem(name = "scale", data = doubleArrayOf(1.0)))
    }
}

private fun createScaleTransform(data: DoubleArray): TransformItem {
    val sliceEnd = if (data.size >= 2 && data[0] == data[1]) 1 else data.size.coerceAtMost(maximumValue = 2)
    return TransformItem(name = "scale", data = data.sliceArray(indices = 0 until sliceEnd))
}
