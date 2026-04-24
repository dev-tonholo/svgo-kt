@file:Suppress(
    "CyclomaticComplexMethod",
    "LongMethod",
    "LoopWithTooManyJumpStatements",
    "MagicNumber",
    "NestedBlockDepth",
)

package svgokt.path

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Checks if two paths have an intersection by checking convex hulls
 * collision using the Gilbert-Johnson-Keerthi (GJK) distance algorithm.
 *
 * Port of svgo's `_path.js` intersects function.
 */
fun intersects(
    path1: List<PathDataItem>,
    path2: List<PathDataItem>,
): Boolean {
    val points1 = gatherPoints(convertRelativeToAbsolute(path1))
    val points2 = gatherPoints(convertRelativeToAbsolute(path2))

    // Axis-aligned bounding box check.
    if (
        points1.maxX <= points2.minX ||
        points2.maxX <= points1.minX ||
        points1.maxY <= points2.minY ||
        points2.maxY <= points1.minY ||
        points1.list.all { set1 ->
            points2.list.all { set2 ->
                set1.list[set1.maxX][0] <= set2.list[set2.minX][0] ||
                    set2.list[set2.maxX][0] <= set1.list[set1.minX][0] ||
                    set1.list[set1.maxY][1] <= set2.list[set2.minY][1] ||
                    set2.list[set2.maxY][1] <= set1.list[set1.minY][1]
            }
        }
    ) {
        return false
    }

    // Get convex hulls from points of each subpath.
    val hullNest1 = points1.list.map(::convexHull)
    val hullNest2 = points2.list.map(::convexHull)

    // Check intersection of every subpath pair using GJK.
    return hullNest1.any { hull1 ->
        if (hull1.list.size < 3) return@any false
        hullNest2.any { hull2 ->
            if (hull2.list.size < 3) return@any false
            gjkIntersects(hull1, hull2)
        }
    }
}

private fun gjkIntersects(hull1: ConvexPoint, hull2: ConvexPoint): Boolean {
    val simplex = mutableListOf(getSupport(hull1, hull2, doubleArrayOf(1.0, 0.0)))
    val direction = minus(simplex[0])
    var iterations = 10_000

    while (true) {
        if (iterations-- == 0) return true // safe fallback: don't merge
        simplex.add(getSupport(hull1, hull2, direction))
        if (dot(direction, simplex.last()) <= 0) return false
        if (processSimplex(simplex, direction)) return true
    }
}

private fun getSupport(
    a: ConvexPoint,
    b: ConvexPoint,
    direction: DoubleArray,
): DoubleArray = sub(supportPoint(a, direction), supportPoint(b, minus(direction)))

private fun supportPoint(polygon: ConvexPoint, direction: DoubleArray): DoubleArray {
    var index = when {
        direction[1] >= 0 -> if (direction[0] < 0) polygon.maxY else polygon.maxX
        else -> if (direction[0] < 0) polygon.minX else polygon.minY
    }
    var maxDot = Double.NEGATIVE_INFINITY
    var value: Double
    do {
        value = dot(polygon.list[index], direction)
        if (value <= maxDot) break
        maxDot = value
        index = (index + 1) % polygon.list.size
    } while (true)
    return polygon.list[(if (index == 0) polygon.list.size else index) - 1]
}

private fun processSimplex(simplex: MutableList<DoubleArray>, direction: DoubleArray): Boolean {
    if (simplex.size == 2) {
        val a = simplex[1]
        val b = simplex[0]
        val ao = minus(a)
        val ab = sub(b, a)
        if (dot(ao, ab) > 0) {
            val result = orth(ab, a)
            direction[0] = result[0]
            direction[1] = result[1]
        } else {
            direction[0] = ao[0]
            direction[1] = ao[1]
            simplex.removeAt(index = 0)
        }
    } else {
        val a = simplex[2]
        val b = simplex[1]
        val c = simplex[0]
        val ab = sub(b, a)
        val ac = sub(c, a)
        val ao = minus(a)
        val acb = orth(ab, ac)
        val abc = orth(ac, ab)

        if (dot(acb, ao) > 0) {
            if (dot(ab, ao) > 0) {
                val result = acb
                direction[0] = result[0]
                direction[1] = result[1]
                simplex.removeAt(index = 0)
            } else {
                direction[0] = ao[0]
                direction[1] = ao[1]
                simplex.removeAt(index = 0)
                simplex.removeAt(index = 0)
            }
        } else if (dot(abc, ao) > 0) {
            if (dot(ac, ao) > 0) {
                val result = abc
                direction[0] = result[0]
                direction[1] = result[1]
                simplex.removeAt(index = 1)
            } else {
                direction[0] = ao[0]
                direction[1] = ao[1]
                simplex.removeAt(index = 0)
                simplex.removeAt(index = 0)
            }
        } else {
            return true
        }
    }
    return false
}

private fun minus(v: DoubleArray): DoubleArray = doubleArrayOf(-v[0], -v[1])

private fun sub(v1: DoubleArray, v2: DoubleArray): DoubleArray =
    doubleArrayOf(v1[0] - v2[0], v1[1] - v2[1])

private fun dot(v1: DoubleArray, v2: DoubleArray): Double =
    v1[0] * v2[0] + v1[1] * v2[1]

private fun orth(v: DoubleArray, from: DoubleArray): DoubleArray {
    val o = doubleArrayOf(-v[1], v[0])
    return if (dot(o, minus(from)) < 0) minus(o) else o
}

private fun cross(o: DoubleArray, a: DoubleArray, b: DoubleArray): Double =
    (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])

// -- Data types for intersection --

private data class ConvexPoint(
    val list: List<DoubleArray>,
    var minX: Int,
    var minY: Int,
    var maxX: Int,
    var maxY: Int,
)

private data class SubPath(
    val list: MutableList<DoubleArray> = mutableListOf(),
    var minX: Int = 0,
    var minY: Int = 0,
    var maxX: Int = 0,
    var maxY: Int = 0,
)

private data class PointsResult(
    val list: MutableList<SubPath> = mutableListOf(),
    var minX: Double = 0.0,
    var minY: Double = 0.0,
    var maxX: Double = 0.0,
    var maxY: Double = 0.0,
)

// -- Convert relative path data to absolute --

internal fun convertRelativeToAbsolute(data: List<PathDataItem>): List<PathDataItem> {
    val result = mutableListOf<PathDataItem>()
    val start = doubleArrayOf(0.0, 0.0)
    val cursor = doubleArrayOf(0.0, 0.0)

    for (item in data) {
        var cmd = item.command
        val args = item.args.toMutableList()

        if (cmd == 'm') {
            args[0] += cursor[0]
            args[1] += cursor[1]
            cmd = 'M'
        }
        if (cmd == 'M') {
            cursor[0] = args[0]
            cursor[1] = args[1]
            start[0] = cursor[0]
            start[1] = cursor[1]
        }
        if (cmd == 'h') {
            args[0] += cursor[0]
            cmd = 'H'
        }
        if (cmd == 'H') {
            cursor[0] = args[0]
        }
        if (cmd == 'v') {
            args[0] += cursor[1]
            cmd = 'V'
        }
        if (cmd == 'V') {
            cursor[1] = args[0]
        }
        if (cmd == 'l') {
            args[0] += cursor[0]
            args[1] += cursor[1]
            cmd = 'L'
        }
        if (cmd == 'L') {
            cursor[0] = args[0]
            cursor[1] = args[1]
        }
        if (cmd == 'c') {
            args[0] += cursor[0]
            args[1] += cursor[1]
            args[2] += cursor[0]
            args[3] += cursor[1]
            args[4] += cursor[0]
            args[5] += cursor[1]
            cmd = 'C'
        }
        if (cmd == 'C') {
            cursor[0] = args[4]
            cursor[1] = args[5]
        }
        if (cmd == 's') {
            args[0] += cursor[0]
            args[1] += cursor[1]
            args[2] += cursor[0]
            args[3] += cursor[1]
            cmd = 'S'
        }
        if (cmd == 'S') {
            cursor[0] = args[2]
            cursor[1] = args[3]
        }
        if (cmd == 'q') {
            args[0] += cursor[0]
            args[1] += cursor[1]
            args[2] += cursor[0]
            args[3] += cursor[1]
            cmd = 'Q'
        }
        if (cmd == 'Q') {
            cursor[0] = args[2]
            cursor[1] = args[3]
        }
        if (cmd == 't') {
            args[0] += cursor[0]
            args[1] += cursor[1]
            cmd = 'T'
        }
        if (cmd == 'T') {
            cursor[0] = args[0]
            cursor[1] = args[1]
        }
        if (cmd == 'a') {
            args[5] += cursor[0]
            args[6] += cursor[1]
            cmd = 'A'
        }
        if (cmd == 'A') {
            cursor[0] = args[5]
            cursor[1] = args[6]
        }
        if (cmd == 'z' || cmd == 'Z') {
            cursor[0] = start[0]
            cursor[1] = start[1]
            cmd = 'z'
        }

        result.add(PathDataItem(command = cmd, args = args))
    }
    return result
}

// -- Gather points from absolute path data --

@Suppress("LargeClass")
private fun gatherPoints(pathData: List<PathDataItem>): PointsResult {
    val points = PointsResult()
    var prevCtrlPoint = doubleArrayOf(0.0, 0.0)

    fun addPoint(path: SubPath, point: DoubleArray) {
        if (path.list.isEmpty() || point[1] > path.list[path.maxY][1]) {
            path.maxY = path.list.size
            points.maxY = if (points.list.isNotEmpty()) max(point[1], points.maxY) else point[1]
        }
        if (path.list.isEmpty() || point[0] > path.list[path.maxX][0]) {
            path.maxX = path.list.size
            points.maxX = if (points.list.isNotEmpty()) max(point[0], points.maxX) else point[0]
        }
        if (path.list.isEmpty() || point[1] < path.list[path.minY][1]) {
            path.minY = path.list.size
            points.minY = if (points.list.isNotEmpty()) min(point[1], points.minY) else point[1]
        }
        if (path.list.isEmpty() || point[0] < path.list[path.minX][0]) {
            path.minX = path.list.size
            points.minX = if (points.list.isNotEmpty()) min(point[0], points.minX) else point[0]
        }
        path.list.add(point)
    }

    for (i in pathData.indices) {
        val item = pathData[i]
        var subPath = if (points.list.isEmpty()) {
            SubPath()
        } else {
            points.list.last()
        }
        val prev = if (i == 0) null else pathData[i - 1]
        var basePoint: DoubleArray? = if (subPath.list.isEmpty()) null else subPath.list.last()
        val data = item.args
        var ctrlPoint = basePoint

        when (item.command) {
            'M' -> {
                subPath = SubPath()
                points.list.add(subPath)
            }
            'H' -> {
                if (basePoint != null) {
                    addPoint(subPath, doubleArrayOf(data[0], basePoint[1]))
                }
            }
            'V' -> {
                if (basePoint != null) {
                    addPoint(subPath, doubleArrayOf(basePoint[0], data[0]))
                }
            }
            'Q' -> {
                addPoint(subPath, doubleArrayOf(data[0], data[1]))
                prevCtrlPoint = doubleArrayOf(data[2] - data[0], data[3] - data[1])
            }
            'T' -> {
                if (basePoint != null && prev != null &&
                    (prev.command == 'Q' || prev.command == 'T')
                ) {
                    ctrlPoint = doubleArrayOf(
                        basePoint[0] + prevCtrlPoint[0],
                        basePoint[1] + prevCtrlPoint[1],
                    )
                    addPoint(subPath, ctrlPoint)
                    prevCtrlPoint = doubleArrayOf(data[0] - ctrlPoint[0], data[1] - ctrlPoint[1])
                }
            }
            'C' -> {
                if (basePoint != null) {
                    addPoint(
                        subPath,
                        doubleArrayOf(
                            0.5 * (basePoint[0] + data[0]),
                            0.5 * (basePoint[1] + data[1]),
                        )
                    )
                }
                addPoint(subPath, doubleArrayOf(0.5 * (data[0] + data[2]), 0.5 * (data[1] + data[3])))
                addPoint(subPath, doubleArrayOf(0.5 * (data[2] + data[4]), 0.5 * (data[3] + data[5])))
                prevCtrlPoint = doubleArrayOf(data[4] - data[2], data[5] - data[3])
            }
            'S' -> {
                if (basePoint != null && prev != null &&
                    (prev.command == 'C' || prev.command == 'S')
                ) {
                    addPoint(
                        subPath,
                        doubleArrayOf(
                            basePoint[0] + 0.5 * prevCtrlPoint[0],
                            basePoint[1] + 0.5 * prevCtrlPoint[1],
                        )
                    )
                    ctrlPoint = doubleArrayOf(
                        basePoint[0] + prevCtrlPoint[0],
                        basePoint[1] + prevCtrlPoint[1],
                    )
                }
                if (ctrlPoint != null) {
                    addPoint(
                        subPath,
                        doubleArrayOf(
                            0.5 * (ctrlPoint[0] + data[0]),
                            0.5 * (ctrlPoint[1] + data[1]),
                        )
                    )
                }
                addPoint(subPath, doubleArrayOf(0.5 * (data[0] + data[2]), 0.5 * (data[1] + data[3])))
                prevCtrlPoint = doubleArrayOf(data[2] - data[0], data[3] - data[1])
            }
            'A' -> {
                if (basePoint != null) {
                    val curves = a2c(
                        x1 = basePoint[0], y1 = basePoint[1],
                        rx = data[0], ry = data[1],
                        angle = data[2],
                        largeArcFlag = data[3],
                        sweepFlag = data[4],
                        x2 = data[5], y2 = data[6],
                        recursive = null,
                    )
                    val curvesList = curves.toMutableList()
                    while (curvesList.size >= 6) {
                        val chunk = curvesList.subList(fromIndex = 0, toIndex = 6).toList()
                        curvesList.subList(fromIndex = 0, toIndex = 6).clear()
                        // toAbsolute: each value += basePoint[i % 2]
                        val cData = chunk.mapIndexed { idx, n ->
                            n + (basePoint?.get(idx % 2) ?: 0.0)
                        }.toDoubleArray()

                        if (basePoint != null) {
                            addPoint(
                                subPath,
                                doubleArrayOf(
                                    0.5 * (basePoint[0] + cData[0]),
                                    0.5 * (basePoint[1] + cData[1]),
                                )
                            )
                        }
                        addPoint(
                            subPath,
                            doubleArrayOf(
                                0.5 * (cData[0] + cData[2]),
                                0.5 * (cData[1] + cData[3]),
                            )
                        )
                        addPoint(
                            subPath,
                            doubleArrayOf(
                                0.5 * (cData[2] + cData[4]),
                                0.5 * (cData[3] + cData[5]),
                            )
                        )
                        if (curvesList.isNotEmpty()) {
                            basePoint = doubleArrayOf(cData[4], cData[5])
                            addPoint(subPath, basePoint)
                        }
                    }
                }
            }
        }

        // Save final command coordinates
        if (data.size >= 2) {
            addPoint(subPath, doubleArrayOf(data[data.size - 2], data[data.size - 1]))
        }
    }

    return points
}

// -- Convex hull --

private fun convexHull(points: SubPath): ConvexPoint {
    val sorted = points.list.sortedWith(compareBy({ it[0] }, { it[1] }))

    val lower = mutableListOf<DoubleArray>()
    var minY = 0
    var bottom = 0
    for (i in sorted.indices) {
        while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], sorted[i]) <= 0) {
            lower.removeLast()
        }
        if (sorted[i][1] < sorted[minY][1]) {
            minY = i
            bottom = lower.size
        }
        lower.add(sorted[i])
    }

    val upper = mutableListOf<DoubleArray>()
    var maxY = sorted.size - 1
    var top = 0
    for (i in sorted.indices.reversed()) {
        while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], sorted[i]) <= 0) {
            upper.removeLast()
        }
        if (sorted[i][1] > sorted[maxY][1]) {
            maxY = i
            top = upper.size
        }
        upper.add(sorted[i])
    }

    upper.removeLast()
    lower.removeLast()

    val hullList = lower + upper

    return ConvexPoint(
        list = hullList,
        minX = 0,
        maxX = lower.size,
        minY = bottom,
        maxY = (lower.size + top) % hullList.size,
    )
}

// -- Arc to cubic bezier (a2c) --

@Suppress("ReturnCount")
private fun a2c(
    x1: Double,
    y1: Double,
    rx: Double,
    ry: Double,
    angle: Double,
    largeArcFlag: Double,
    sweepFlag: Double,
    x2: Double,
    y2: Double,
    recursive: DoubleArray?,
): List<Double> {
    val angle120 = PI * 120 / 180
    val rad = PI / 180 * angle

    fun rotateX(px: Double, py: Double, r: Double): Double = px * cos(r) - py * sin(r)
    fun rotateY(px: Double, py: Double, r: Double): Double = px * sin(r) + py * cos(r)

    var ax1 = x1
    var ay1 = y1
    var ax2 = x2
    var ay2 = y2
    var arx = rx
    var ary = ry
    var f1: Double
    var f2: Double
    var cx: Double
    var cy: Double

    if (recursive == null) {
        ax1 = rotateX(x1, y1, -rad)
        ay1 = rotateY(ax1, y1, -rad)
        ax2 = rotateX(x2, y2, -rad)
        ay2 = rotateY(ax2, y2, -rad)
        val x = (ax1 - ax2) / 2
        val y = (ay1 - ay2) / 2
        var h = (x * x) / (arx * arx) + (y * y) / (ary * ary)
        if (h > 1) {
            h = sqrt(h)
            arx = h * arx
            ary = h * ary
        }
        val rx2 = arx * arx
        val ry2 = ary * ary
        val k = (if (largeArcFlag == sweepFlag) -1 else 1) *
            sqrt(abs((rx2 * ry2 - rx2 * y * y - ry2 * x * x) / (rx2 * y * y + ry2 * x * x)))
        cx = (k * arx * y) / ary + (ax1 + ax2) / 2
        cy = (k * -ary * x) / arx + (ay1 + ay2) / 2
        f1 = asin(((ay1 - cy) / ary).coerceIn(minimumValue = -1.0, maximumValue = 1.0))
        f2 = asin(((ay2 - cy) / ary).coerceIn(minimumValue = -1.0, maximumValue = 1.0))
        if (ax1 < cx) f1 = PI - f1
        if (ax2 < cx) f2 = PI - f2
        if (f1 < 0) f1 = PI * 2 + f1
        if (f2 < 0) f2 = PI * 2 + f2
        if (sweepFlag != 0.0 && f1 > f2) f1 -= PI * 2
        if (sweepFlag == 0.0 && f2 > f1) f2 -= PI * 2
    } else {
        f1 = recursive[0]
        f2 = recursive[1]
        cx = recursive[2]
        cy = recursive[3]
    }

    var df = f2 - f1
    var res = emptyList<Double>()
    if (abs(df) > angle120) {
        val f2old = f2
        val x2old = ax2
        val y2old = ay2
        f2 = f1 + angle120 * (if (sweepFlag != 0.0 && f2 > f1) 1 else -1)
        ax2 = cx + arx * cos(f2)
        ay2 = cy + ary * sin(f2)
        res = a2c(ax2, ay2, arx, ary, angle, 0.0, sweepFlag, x2old, y2old, doubleArrayOf(f2, f2old, cx, cy))
    }
    df = f2 - f1
    val c1 = cos(f1)
    val s1 = sin(f1)
    val c2 = cos(f2)
    val s2 = sin(f2)
    val t = tan(df / 4)
    val hx = (4.0 / 3) * arx * t
    val hy = (4.0 / 3) * ary * t
    val m = listOf(
        -hx * s1,
        hy * c1,
        ax2 + hx * s2 - ax1,
        ay2 - hy * c2 - ay1,
        ax2 - ax1,
        ay2 - ay1,
    )

    if (recursive != null) {
        return m + res
    }

    val fullRes = (m + res).toMutableList()
    val newRes = mutableListOf<Double>()
    for (i in fullRes.indices) {
        newRes.add(
            if (i % 2 != 0) {
                rotateY(fullRes[i - 1], fullRes[i], rad)
            } else {
                rotateX(fullRes[i], fullRes[i + 1], rad)
            },
        )
    }
    return newRes
}
