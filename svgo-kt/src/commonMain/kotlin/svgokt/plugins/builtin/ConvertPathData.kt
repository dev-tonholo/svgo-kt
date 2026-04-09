package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastRoot
import svgokt.domain.css.ComputedStyles
import svgokt.domain.css.Stylesheet
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.path.PathDataItem
import svgokt.path.js2path
import svgokt.path.path2js
import svgokt.path.stringifyPathData
import svgokt.plugins.xast.collectStylesheet
import svgokt.plugins.xast.visit
import svgokt.style.computeStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private const val DEFAULT_FLOAT_PRECISION = 3
private const val ARC_ARGS = 7
private const val ARC_LARGE_ARC_INDEX = 3
private const val ARC_SWEEP_INDEX = 4
private const val ARC_END_X_INDEX = 5
private const val ARC_END_Y_INDEX = 6
private const val CUBIC_END_X_INDEX = 4
private const val CUBIC_END_Y_INDEX = 5
private const val SQ_END_X_INDEX = 2
private const val SQ_END_Y_INDEX = 3

/** Absolute tolerance for comparing floating-point coordinates. */
private const val EPSILON = 1e-9

/**
 * Round matching JS Math.round behavior (half-up), not Kotlin's banker's rounding.
 * JS: Math.round(2.5) = 3, Math.round(-0.5) = 0
 * Kotlin: round(2.5) = 2.0 (rounds to even)
 */
private fun jsRound(value: Double): Double = floor(value + 0.5)

data class MakeArcs(
    val threshold: Float,
    val tolerance: Float,
)

data class ConvertPathDataParams(
    val applyTransforms: Boolean = true,
    val applyTransformsStroked: Boolean = true,
    val makeArcs: MakeArcs = MakeArcs(
        threshold = 2.5f,
        tolerance = 0.5f,
    ),
    val straightCurves: Boolean = true,
    val convertToQ: Boolean = true,
    val lineShorthands: Boolean = true,
    val convertToZ: Boolean = true,
    val curveSmoothShorthands: Boolean = true,
    val floatPrecision: Int = DEFAULT_FLOAT_PRECISION,
    val transformPrecision: Int = 5,
    val smartArcRounding: Boolean = true,
    val removeUseless: Boolean = true,
    val collapseRepeated: Boolean = true,
    val utilizeAbsolute: Boolean = true,
    val leadingZero: Boolean = true,
    val negativeExtraSpace: Boolean = true,
    val noSpaceAfterFlags: Boolean = false,
    val forceAbsolutePath: Boolean = false,
) : PluginParams,
    Map<String, Any> by mapOf(
        "applyTransforms" to applyTransforms,
        "applyTransformsStroked" to applyTransformsStroked,
        "makeArcs" to makeArcs,
        "straightCurves" to straightCurves,
        "convertToQ" to convertToQ,
        "lineShorthands" to lineShorthands,
        "convertToZ" to convertToZ,
        "curveSmoothShorthands" to curveSmoothShorthands,
        "floatPrecision" to floatPrecision,
        "transformPrecision" to transformPrecision,
        "smartArcRounding" to smartArcRounding,
        "removeUseless" to removeUseless,
        "collapseRepeated" to collapseRepeated,
        "utilizeAbsolute" to utilizeAbsolute,
        "leadingZero" to leadingZero,
        "negativeExtraSpace" to negativeExtraSpace,
        "noSpaceAfterFlags" to noSpaceAfterFlags,
        "forceAbsolutePath" to forceAbsolutePath,
    )

/**
 * Path elements that have a 'd' attribute to optimize.
 */
private val PATH_ELEMS = setOf("path", "glyph", "missing-glyph")

class ConvertPathData(
    override val params: ConvertPathDataParams = ConvertPathDataParams(),
) : Plugin<ConvertPathDataParams> {
    override val name: String = "convertPathData"
    override val description: String = "optimizes path data: writes in shorter form, applies transformations"

    /**
     * Convert absolute Path to relative,
     * collapse repeated instructions,
     * detect and convert Lineto shorthands,
     * remove useless instructions like "l0,0",
     * trim useless delimiters and leading zeros,
     * decrease accuracy of floating-point numbers.
     *
     * @see https://www.w3.org/TR/SVG11/paths.html#PathData
     *
     * @author Kir Belevich / parsed to kotlin by Rafael Tonholo
     */
    override val fn: PluginFn = { root, pluginParams, _ ->
        val resolvedParams = resolveParams(pluginParams = pluginParams, defaults = params)

        if (resolvedParams.applyTransforms) {
            val visitor = applyTransforms(
                root = root,
                params = ApplyTransformsParams(
                    transformPrecision = resolvedParams.transformPrecision,
                    applyTransformsStroked = resolvedParams.applyTransformsStroked,
                ),
            )
            if (visitor != null) {
                root.visit(visitor = visitor)
            }
        }

        val stylesheet = collectStylesheet(root)

        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    processElement(
                        node = node,
                        effectiveParams = resolvedParams,
                        stylesheet = stylesheet,
                    )
                },
            ),
        )
    }

    companion object {
        /**
         * Resolve params by merging fixture/override params on top of defaults.
         * Handles the case where pluginParams is a generic map from the test harness.
         */
        @Suppress("CyclomaticComplexity")
        private fun resolveParams(
            pluginParams: PluginParams?,
            defaults: ConvertPathDataParams,
        ): ConvertPathDataParams {
            if (pluginParams is ConvertPathDataParams) return pluginParams
            if (pluginParams == null) return defaults
            val map = pluginParams as? Map<*, *> ?: return defaults
            return ConvertPathDataParams(
                applyTransforms = (map["applyTransforms"] as? Boolean) ?: defaults.applyTransforms,
                applyTransformsStroked = (map["applyTransformsStroked"] as? Boolean)
                    ?: defaults.applyTransformsStroked,
                straightCurves = (map["straightCurves"] as? Boolean) ?: defaults.straightCurves,
                convertToQ = (map["convertToQ"] as? Boolean) ?: defaults.convertToQ,
                lineShorthands = (map["lineShorthands"] as? Boolean) ?: defaults.lineShorthands,
                convertToZ = (map["convertToZ"] as? Boolean) ?: defaults.convertToZ,
                curveSmoothShorthands = (map["curveSmoothShorthands"] as? Boolean)
                    ?: defaults.curveSmoothShorthands,
                floatPrecision = (map["floatPrecision"] as? Number)?.toInt() ?: defaults.floatPrecision,
                transformPrecision = (map["transformPrecision"] as? Number)?.toInt()
                    ?: defaults.transformPrecision,
                smartArcRounding = (map["smartArcRounding"] as? Boolean) ?: defaults.smartArcRounding,
                removeUseless = (map["removeUseless"] as? Boolean) ?: defaults.removeUseless,
                collapseRepeated = (map["collapseRepeated"] as? Boolean) ?: defaults.collapseRepeated,
                utilizeAbsolute = (map["utilizeAbsolute"] as? Boolean) ?: defaults.utilizeAbsolute,
                leadingZero = (map["leadingZero"] as? Boolean) ?: defaults.leadingZero,
                negativeExtraSpace = (map["negativeExtraSpace"] as? Boolean) ?: defaults.negativeExtraSpace,
                noSpaceAfterFlags = (map["noSpaceAfterFlags"] as? Boolean) ?: defaults.noSpaceAfterFlags,
                forceAbsolutePath = (map["forceAbsolutePath"] as? Boolean) ?: defaults.forceAbsolutePath,
            )
        }

        @Suppress("ReturnCount")
        private fun processElement(
            node: XastElement,
            effectiveParams: ConvertPathDataParams,
            stylesheet: Stylesheet,
        ): VisitState {
            if (node.name !in PATH_ELEMS) {
                return VisitState.Continue
            }

            val originalD = node.attributes["d"]
            if (originalD.isNullOrBlank()) {
                return VisitState.Continue
            }

            val data = path2js(element = node).map { item ->
                PathDataItem(command = item.command, args = item.args.toMutableList())
            }
            if (data.isEmpty()) {
                return VisitState.Continue
            }

            val precision = effectiveParams.floatPrecision
            var optimized = data.toMutableList()

            val includesVertices = optimized.any { it.command != 'm' && it.command != 'M' }

            val computedStyleMap = computeStyle(stylesheet = stylesheet, node = node)
            val hasMarkerMid = computedStyleMap["marker-mid"] != null

            val strokeComputed = computedStyleMap["stroke"]
            val maybeHasStroke = strokeComputed != null &&
                (strokeComputed is ComputedStyles.DynamicStyle ||
                    (strokeComputed is ComputedStyles.StaticStyle && strokeComputed.value != "none"))

            val linecapComputed = computedStyleMap["stroke-linecap"]
            val maybeHasLinecap = linecapComputed != null &&
                (linecapComputed is ComputedStyles.DynamicStyle ||
                    (linecapComputed is ComputedStyles.StaticStyle && linecapComputed.value != "butt"))

            val maybeHasStrokeAndLinecap = maybeHasStroke && maybeHasLinecap

            val linejoinComputed = computedStyleMap["stroke-linejoin"]
            val isSafeToUseZ = if (maybeHasStroke) {
                linecapComputed is ComputedStyles.StaticStyle &&
                    linecapComputed.value == "round" &&
                    linejoinComputed is ComputedStyles.StaticStyle &&
                    linejoinComputed.value == "round"
            } else {
                true
            }

            // Phase 1: convert all to relative, populate base/coords
            convertToRelative(optimized)

            // Phase 2: apply filters (rounding, straight curves, line shorthands, etc.)
            optimized = applyFilters(
                path = optimized,
                params = effectiveParams,
                precision = precision,
                isSafeToUseZ = isSafeToUseZ,
                maybeHasStrokeAndLinecap = maybeHasStrokeAndLinecap,
                hasMarkerMid = hasMarkerMid,
            )

            // Phase 3: convert back to absolute where shorter
            if (effectiveParams.utilizeAbsolute) {
                optimized = convertToMixed(
                    path = optimized,
                    params = effectiveParams,
                    precision = precision,
                )
            }

            // Add z to markers-only paths that had vertices before optimization
            val hasMarker = node.attributes["marker-start"] != null ||
                node.attributes["marker-end"] != null
            val isMarkersOnlyPath = hasMarker && includesVertices &&
                optimized.all { it.command == 'm' || it.command == 'M' }
            if (isMarkersOnlyPath) {
                optimized.add(PathDataItem(command = 'z', args = mutableListOf()))
            }

            js2path(
                element = node,
                data = optimized,
                precision = precision,
                noSpaceAfterFlags = effectiveParams.noSpaceAfterFlags,
            )

            return VisitState.Continue
        }

        /**
         * Convert absolute path data coordinates to relative.
         * Populates [PathDataItem.base] and [PathDataItem.coords] on each item.
         */
        @Suppress("CyclomaticComplexity", "CyclomaticComplexMethod", "LongMethod")
        private fun convertToRelative(pathData: MutableList<PathDataItem>) {
            val start = doubleArrayOf(0.0, 0.0)
            val cursor = doubleArrayOf(0.0, 0.0)
            var prevCoords = doubleArrayOf(0.0, 0.0)

            for (i in pathData.indices) {
                val item = pathData[i]
                var command = item.command
                val args = item.args

                when {
                    command == 'm' -> {
                        cursor[0] += args[0]
                        cursor[1] += args[1]
                        start[0] = cursor[0]
                        start[1] = cursor[1]
                    }
                    command == 'M' -> {
                        // M -> m (skip first moveto)
                        if (i != 0) {
                            command = 'm'
                        }
                        args[0] -= cursor[0]
                        args[1] -= cursor[1]
                        cursor[0] += args[0]
                        cursor[1] += args[1]
                        start[0] = cursor[0]
                        start[1] = cursor[1]
                    }
                    command == 'l' -> {
                        cursor[0] += args[0]
                        cursor[1] += args[1]
                    }
                    command == 'L' -> {
                        command = 'l'
                        args[0] -= cursor[0]
                        args[1] -= cursor[1]
                        cursor[0] += args[0]
                        cursor[1] += args[1]
                    }
                    command == 'h' -> {
                        cursor[0] += args[0]
                    }
                    command == 'H' -> {
                        command = 'h'
                        args[0] -= cursor[0]
                        cursor[0] += args[0]
                    }
                    command == 'v' -> {
                        cursor[1] += args[0]
                    }
                    command == 'V' -> {
                        command = 'v'
                        args[0] -= cursor[1]
                        cursor[1] += args[0]
                    }
                    command == 'c' -> {
                        cursor[0] += args[CUBIC_END_X_INDEX]
                        cursor[1] += args[CUBIC_END_Y_INDEX]
                    }
                    command == 'C' -> {
                        command = 'c'
                        args[0] -= cursor[0]
                        args[1] -= cursor[1]
                        args[2] -= cursor[0]
                        args[3] -= cursor[1]
                        args[CUBIC_END_X_INDEX] -= cursor[0]
                        args[CUBIC_END_Y_INDEX] -= cursor[1]
                        cursor[0] += args[CUBIC_END_X_INDEX]
                        cursor[1] += args[CUBIC_END_Y_INDEX]
                    }
                    command == 's' -> {
                        cursor[0] += args[SQ_END_X_INDEX]
                        cursor[1] += args[SQ_END_Y_INDEX]
                    }
                    command == 'S' -> {
                        command = 's'
                        args[0] -= cursor[0]
                        args[1] -= cursor[1]
                        args[SQ_END_X_INDEX] -= cursor[0]
                        args[SQ_END_Y_INDEX] -= cursor[1]
                        cursor[0] += args[SQ_END_X_INDEX]
                        cursor[1] += args[SQ_END_Y_INDEX]
                    }
                    command == 'q' -> {
                        cursor[0] += args[SQ_END_X_INDEX]
                        cursor[1] += args[SQ_END_Y_INDEX]
                    }
                    command == 'Q' -> {
                        command = 'q'
                        args[0] -= cursor[0]
                        args[1] -= cursor[1]
                        args[SQ_END_X_INDEX] -= cursor[0]
                        args[SQ_END_Y_INDEX] -= cursor[1]
                        cursor[0] += args[SQ_END_X_INDEX]
                        cursor[1] += args[SQ_END_Y_INDEX]
                    }
                    command == 't' -> {
                        cursor[0] += args[0]
                        cursor[1] += args[1]
                    }
                    command == 'T' -> {
                        command = 't'
                        args[0] -= cursor[0]
                        args[1] -= cursor[1]
                        cursor[0] += args[0]
                        cursor[1] += args[1]
                    }
                    command == 'a' -> {
                        cursor[0] += args[ARC_END_X_INDEX]
                        cursor[1] += args[ARC_END_Y_INDEX]
                    }
                    command == 'A' -> {
                        command = 'a'
                        args[ARC_END_X_INDEX] -= cursor[0]
                        args[ARC_END_Y_INDEX] -= cursor[1]
                        cursor[0] += args[ARC_END_X_INDEX]
                        cursor[1] += args[ARC_END_Y_INDEX]
                    }
                    command == 'Z' || command == 'z' -> {
                        cursor[0] = start[0]
                        cursor[1] = start[1]
                    }
                }

                item.command = command
                item.base = prevCoords.copyOf()
                item.coords = cursor.copyOf()
                prevCoords = item.coords ?: cursor.copyOf()
            }
        }

        /**
         * Main optimization filters loop.
         * Applies rounding with accumulating error, straight curves to lines,
         * line shorthands, remove useless, collapse repeated, convert to z.
         */
        @Suppress(
            "CyclomaticComplexity",
            "CyclomaticComplexMethod",
            "LongMethod",
            "NestedBlockDepth",
            "LongParameterList",
        )
        private fun applyFilters(
            path: MutableList<PathDataItem>,
            params: ConvertPathDataParams,
            precision: Int,
            isSafeToUseZ: Boolean = true,
            maybeHasStrokeAndLinecap: Boolean = false,
            hasMarkerMid: Boolean = false,
        ): MutableList<PathDataItem> {
            val error = 10.0.pow(-precision)
            val arcThreshold = params.makeArcs.threshold.toDouble()
            val arcTolerance = params.makeArcs.tolerance.toDouble()
            val relSubpoint = doubleArrayOf(0.0, 0.0)
            val pathBase = doubleArrayOf(0.0, 0.0)
            var prev = PathDataItem(command = ' ', args = mutableListOf())
            var prevQControlPoint: DoubleArray? = null

            val result = mutableListOf<PathDataItem>()

            var index = 0
            while (index < path.size) {
                val item = path[index]
                var command = item.command
                var data = item.args
                val next = path.getOrNull(index + 1)
                var itemBase = item.base
                var itemCoords = item.coords

                if (command != 'Z' && command != 'z') {
                    // Build sdata for smooth curve shorthand (prepend reflected control point)
                    var sdata: MutableList<Double> = data
                    if (command == 's') {
                        sdata = mutableListOf(0.0, 0.0).apply { addAll(data) }
                        val pdata = prev.args
                        val n = pdata.size
                        if (n >= 4) {
                            sdata[0] = pdata[n - 2] - pdata[n - 4]
                            sdata[1] = pdata[n - 1] - pdata[n - 3]
                        }
                    }

                    // Convert curves to arcs if possible
                    val makeArcsResult = tryMakeArcs(
                        path = path,
                        index = index,
                        command = command,
                        sdata = sdata,
                        item = item,
                        prev = prev,
                        params = params,
                        precision = precision,
                        error = error,
                        arcThreshold = arcThreshold,
                        arcTolerance = arcTolerance,
                        relSubpoint = relSubpoint,
                    )
                    if (makeArcsResult != null) {
                        if (makeArcsResult.skip) {
                            index++
                            continue
                        }
                        command = makeArcsResult.command
                        data = makeArcsResult.data
                        // Re-read base/coords since tryMakeArcs may modify them
                        itemBase = item.base
                        itemCoords = item.coords
                    }

                    // Rounding relative coordinates with accumulating error correction
                    if (precision >= 0) {
                        applyAccumulatingRound(
                            command = command,
                            data = data,
                            itemBase = itemBase,
                            relSubpoint = relSubpoint,
                            precision = precision,
                        )
                        updateRelSubpoint(
                            command = command,
                            data = data,
                            relSubpoint = relSubpoint,
                            precision = precision,
                        )
                        if (command == 'M' || command == 'm') {
                            pathBase[0] = relSubpoint[0]
                            pathBase[1] = relSubpoint[1]
                        }
                    }

                    // Smart arc rounding
                    val sagitta = if (command == 'a') calculateSagitta(data) else null
                    if (params.smartArcRounding && sagitta != null && precision > 0) {
                        smartRoundArcRadius(
                            data = data,
                            sagitta = sagitta,
                            error = error,
                            precision = precision,
                        )
                    }

                    // Convert straight curves into line segments
                    if (params.straightCurves) {
                        val straightResult = tryStraightCurve(
                            command = command,
                            data = data,
                            sdata = sdata,
                            error = error,
                            prev = prev,
                            next = next,
                            sagitta = sagitta,
                        )
                        if (straightResult != null) {
                            command = straightResult.first
                            data = straightResult.second
                        }
                    }

                    // Degree-lower C to Q when possible
                    if (params.convertToQ && command == 'c' && itemBase != null) {
                        val cToQResult = tryConvertCToQ(
                            data = data,
                            itemBase = itemBase,
                            error = error,
                            precision = precision,
                            params = params,
                            next = next,
                        )
                        if (cToQResult != null) {
                            command = cToQResult.first
                            data = cToQResult.second
                        }
                    }

                    // Horizontal and vertical line shorthands
                    if (params.lineShorthands && command == 'l') {
                        if (data[1] == 0.0) {
                            command = 'h'
                            data = mutableListOf(data[0])
                        } else if (data[0] == 0.0) {
                            command = 'v'
                            data = mutableListOf(data[1])
                        }
                    }

                    // Collapse repeated commands
                    if (params.collapseRepeated &&
                        !hasMarkerMid &&
                        (command == 'm' || command == 'h' || command == 'v') &&
                        prev.command.lowercaseChar() == command &&
                        ((command != 'h' && command != 'v') ||
                            (prev.args[0] >= 0) == (data[0] >= 0))
                    ) {
                        prev.args[0] += data[0]
                        if (command != 'h' && command != 'v') {
                            prev.args[1] += data[1]
                        }
                        prev.coords = itemCoords?.copyOf()
                        index++
                        continue
                    }

                    // Convert curves into smooth shorthands
                    if (params.curveSmoothShorthands && prev.command != ' ') {
                        val smoothResult = trySmoothShorthand(
                            command = command,
                            data = data,
                            prev = prev,
                            error = error,
                            item = item,
                            qControlPoint = prevQControlPoint,
                        )
                        if (smoothResult != null) {
                            command = smoothResult.first
                            data = smoothResult.second
                        }
                    }

                    // Remove useless non-first path segments
                    if (params.removeUseless && !maybeHasStrokeAndLinecap) {
                        if ((command == 'l' || command == 'h' || command == 'v' ||
                                command == 'q' || command == 't' ||
                                command == 'c' || command == 's') &&
                            data.all { it == 0.0 }
                        ) {
                            index++
                            continue
                        }
                        if (command == 'a' && data[ARC_END_X_INDEX] == 0.0 && data[ARC_END_Y_INDEX] == 0.0) {
                            index++
                            continue
                        }
                    }

                    // Convert going home to z
                    if (params.convertToZ &&
                        (isSafeToUseZ || next?.command == 'Z' || next?.command == 'z') &&
                        (command == 'l' || command == 'h' || command == 'v') &&
                        itemCoords != null
                    ) {
                        if (abs(pathBase[0] - itemCoords[0]) < error &&
                            abs(pathBase[1] - itemCoords[1]) < error
                        ) {
                            command = 'z'
                            data = mutableListOf()
                        }
                    }

                    // Build the final item for this index
                    item.command = command
                    if (data !== item.args) {
                        item.args.clear()
                        item.args.addAll(data)
                    }
                } else {
                    // z resets coordinates
                    relSubpoint[0] = pathBase[0]
                    relSubpoint[1] = pathBase[1]
                    // Remove consecutive z commands
                    if (prev.command == 'Z' || prev.command == 'z') {
                        index++
                        continue
                    }
                }

                // Remove useless z when already at start
                if ((command == 'Z' || command == 'z') &&
                    params.removeUseless && isSafeToUseZ &&
                    itemBase != null && itemCoords != null
                ) {
                    if (abs(itemBase[0] - itemCoords[0]) < error / 10 &&
                        abs(itemBase[1] - itemCoords[1]) < error / 10
                    ) {
                        index++
                        continue
                    }
                }

                // Track Q control point for t+q->t+t conversion
                if (command == 'q') {
                    val qBase = item.base
                    if (qBase != null) {
                        prevQControlPoint = doubleArrayOf(
                            data[0] + qBase[0],
                            data[1] + qBase[1],
                        )
                    }
                } else if (command == 't') {
                    val qBase = item.base
                    prevQControlPoint = if (prevQControlPoint != null && qBase != null) {
                        reflectPoint(prevQControlPoint, qBase)
                    } else {
                        itemCoords?.copyOf()
                    }
                } else {
                    prevQControlPoint = null
                }

                result.add(item)
                prev = item
                index++
            }

            return result
        }

        /**
         * Apply accumulating error correction rounding.
         * Adjusts relative data so that the rounded sum stays close to the true absolute position.
         */
        private fun applyAccumulatingRound(
            command: Char,
            data: MutableList<Double>,
            itemBase: DoubleArray?,
            relSubpoint: DoubleArray,
            precision: Int,
        ) {
            if (itemBase == null) return
            when (command) {
                'm', 'l', 't', 'q', 's', 'c' -> {
                    for (i in data.indices) {
                        data[i] += itemBase[i % 2] - relSubpoint[i % 2]
                    }
                }
                'h' -> {
                    data[0] += itemBase[0] - relSubpoint[0]
                }
                'v' -> {
                    data[0] += itemBase[1] - relSubpoint[1]
                }
                'a' -> {
                    data[ARC_END_X_INDEX] += itemBase[0] - relSubpoint[0]
                    data[ARC_END_Y_INDEX] += itemBase[1] - relSubpoint[1]
                }
            }
            applyRound(data = data, precision = precision, command = command)
        }

        private fun updateRelSubpoint(
            command: Char,
            data: MutableList<Double>,
            relSubpoint: DoubleArray,
            precision: Int,
        ) {
            when (command) {
                'h' -> relSubpoint[0] += data[0]
                'v' -> relSubpoint[1] += data[0]
                else -> {
                    if (data.size >= 2) {
                        relSubpoint[0] += data[data.size - 2]
                        relSubpoint[1] += data[data.size - 1]
                    }
                }
            }
            applyRound(data = relSubpoint, precision = precision)
        }

        /**
         * Simple rounding function used when precision is 0.
         */
        private fun simpleRound(data: MutableList<Double>) {
            for (i in data.indices) {
                data[i] = jsRound(data[i])
            }
        }

        private fun simpleRound(data: DoubleArray) {
            for (i in data.indices) {
                data[i] = jsRound(data[i])
            }
        }

        /**
         * Dispatch to strongRound or simpleRound depending on precision.
         * When precision > 0, use strongRound (smart rounding).
         * When precision == 0, use simpleRound (Math.round).
         */
        private fun applyRound(data: MutableList<Double>, precision: Int, command: Char = ' ') {
            if (precision > 0) {
                strongRound(data = data, precision = precision, command = command)
            } else {
                simpleRound(data)
            }
        }

        private fun applyRound(data: DoubleArray, precision: Int) {
            if (precision > 0) {
                strongRound(data = data, precision = precision)
            } else {
                simpleRound(data)
            }
        }

        /**
         * Smart rounding that rounds aggressively when the number is close to the boundary.
         */
        private fun strongRound(data: MutableList<Double>, precision: Int, command: Char = ' ') {
            val error = 10.0.pow(-precision)
            val pow = 10.0.pow(precision)
            for (i in data.indices) {
                // Don't round arc flags
                if ((command == 'a' || command == 'A') &&
                    (i % ARC_ARGS == ARC_LARGE_ARC_INDEX || i % ARC_ARGS == ARC_SWEEP_INDEX)
                ) {
                    continue
                }
                val fixed = jsRound(data[i] * pow) / pow
                if (fixed != data[i]) {
                    val powLower = 10.0.pow(precision - 1)
                    val rounded = jsRound(data[i] * powLower) / powLower
                    val diff = jsRound(abs(rounded - data[i]) * 10.0.pow(precision + 1)) /
                        10.0.pow(precision + 1)
                    data[i] = if (diff >= error) fixed else rounded
                }
            }
        }

        private fun strongRound(data: DoubleArray, precision: Int) {
            val error = 10.0.pow(-precision)
            val pow = 10.0.pow(precision)
            for (i in data.indices) {
                val fixed = jsRound(data[i] * pow) / pow
                if (fixed != data[i]) {
                    val powLower = 10.0.pow(precision - 1)
                    val rounded = jsRound(data[i] * powLower) / powLower
                    val diff = jsRound(abs(rounded - data[i]) * 10.0.pow(precision + 1)) /
                        10.0.pow(precision + 1)
                    data[i] = if (diff >= error) fixed else rounded
                }
            }
        }

        /**
         * Try to convert a curve command to a line if it represents a straight line.
         */
        @Suppress("ReturnCount", "LongParameterList")
        private fun tryStraightCurve(
            command: Char,
            data: MutableList<Double>,
            sdata: MutableList<Double>,
            error: Double,
            prev: PathDataItem,
            next: PathDataItem?,
            sagitta: Double?,
        ): Pair<Char, MutableList<Double>>? {
            if ((command == 'c' && isCurveStraightLine(data, error)) ||
                (command == 's' && isCurveStraightLine(sdata, error))
            ) {
                if (next != null && next.command == 's') {
                    makeLonghand(item = next, prevData = data)
                }
                return 'l' to mutableListOf(data[data.size - 2], data[data.size - 1])
            }
            if (command == 'q' && isCurveStraightLine(data, error)) {
                if (next != null && next.command == 't') {
                    makeLonghand(item = next, prevData = data)
                }
                return 'l' to mutableListOf(data[data.size - 2], data[data.size - 1])
            }
            if (command == 't' && prev.command != 'q' && prev.command != 't') {
                return 'l' to mutableListOf(data[data.size - 2], data[data.size - 1])
            }
            if (command == 'a') {
                if (data[0] == 0.0 || data[1] == 0.0 ||
                    (sagitta != null && sagitta < error)
                ) {
                    return 'l' to mutableListOf(data[data.size - 2], data[data.size - 1])
                }
            }
            return null
        }

        /**
         * Try to convert C (cubic bezier) to Q (quadratic bezier) when possible.
         * m 0 12 C 4 4 8 4 12 12 -> M 0 12 Q 6 0 12 12
         */
        @Suppress("ReturnCount")
        private fun tryConvertCToQ(
            data: MutableList<Double>,
            itemBase: DoubleArray,
            error: Double,
            precision: Int,
            params: ConvertPathDataParams,
            next: PathDataItem?,
        ): Pair<Char, MutableList<Double>>? {
            val x1 = 0.75 * (itemBase[0] + data[0]) - 0.25 * itemBase[0]
            val x2 = 0.75 * (itemBase[0] + data[2]) - 0.25 * (itemBase[0] + data[4])
            if (abs(x1 - x2) >= error * 2) return null

            val y1 = 0.75 * (itemBase[1] + data[1]) - 0.25 * itemBase[1]
            val y2 = 0.75 * (itemBase[1] + data[3]) - 0.25 * (itemBase[1] + data[5])
            if (abs(y1 - y2) >= error * 2) return null

            val newData = mutableListOf(
                x1 + x2 - itemBase[0],
                y1 + y2 - itemBase[1],
                data[4],
                data[5],
            )
            strongRound(data = newData, precision = precision)

            val originalStr = stringifyArgs(data, precision, params)
            val newStr = stringifyArgs(newData, precision, params)
            if (newStr.length < originalStr.length) {
                if (next != null && next.command == 's') {
                    makeLonghand(item = next, prevData = newData)
                }
                return 'q' to newData
            }
            return null
        }

        /**
         * Round arc radius more accurately based on sagitta.
         */
        private fun smartRoundArcRadius(
            data: MutableList<Double>,
            sagitta: Double,
            error: Double,
            precision: Int,
        ) {
            for (precisionNew in precision downTo 0) {
                val pow = 10.0.pow(precisionNew)
                val radius = jsRound(data[0] * pow) / pow
                val sagittaNew = calculateSagitta(
                    mutableListOf(radius, radius, data[2], data[3], data[4], data[5], data[6]),
                )
                if (sagittaNew != null && abs(sagitta - sagittaNew) < error) {
                    data[0] = radius
                    data[1] = radius
                } else {
                    break
                }
            }
        }

        /**
         * Try to convert curves into smooth shorthand forms.
         */
        @Suppress("ReturnCount", "LongParameterList")
        private fun trySmoothShorthand(
            command: Char,
            data: MutableList<Double>,
            prev: PathDataItem,
            error: Double,
            item: PathDataItem,
            qControlPoint: DoubleArray?,
        ): Pair<Char, MutableList<Double>>? {
            if (command == 'c') {
                val pdata = prev.args
                val n = pdata.size
                when {
                    // c + c -> c + s
                    prev.command == 'c' && n >= 6 &&
                        abs(data[0] - -(pdata[2] - pdata[4])) < error &&
                        abs(data[1] - -(pdata[3] - pdata[5])) < error ->
                        return 's' to mutableListOf(data[2], data[3], data[4], data[5])
                    // s + c -> s + s
                    prev.command == 's' && n >= 4 &&
                        abs(data[0] - -(pdata[0] - pdata[2])) < error &&
                        abs(data[1] - -(pdata[1] - pdata[3])) < error ->
                        return 's' to mutableListOf(data[2], data[3], data[4], data[5])
                    // [^cs] + c -> [^cs] + s (when first control point is at origin)
                    prev.command != 'c' && prev.command != 's' &&
                        abs(data[0]) < error && abs(data[1]) < error ->
                        return 's' to mutableListOf(data[2], data[3], data[4], data[5])
                }
            }
            if (command == 'q') {
                val pdata = prev.args
                val n = pdata.size
                // q + q -> q + t
                if (prev.command == 'q' && n >= 4 &&
                    abs(data[0] - (pdata[2] - pdata[0])) < error &&
                    abs(data[1] - (pdata[3] - pdata[1])) < error
                ) {
                    return 't' to mutableListOf(data[2], data[3])
                }
                // t + q -> t + t
                if (prev.command == 't' && qControlPoint != null) {
                    val base = item.base
                    if (base != null) {
                        val predicted = reflectPoint(qControlPoint, base)
                        val realCP = doubleArrayOf(data[0] + base[0], data[1] + base[1])
                        if (abs(predicted[0] - realCP[0]) < error &&
                            abs(predicted[1] - realCP[1]) < error
                        ) {
                            return 't' to mutableListOf(data[2], data[3])
                        }
                    }
                }
            }
            return null
        }

        private fun reflectPoint(controlPoint: DoubleArray, base: DoubleArray): DoubleArray {
            return doubleArrayOf(2 * base[0] - controlPoint[0], 2 * base[1] - controlPoint[1])
        }

        /**
         * Checks if a curve is a straight line by measuring distance from control
         * points to the line formed by start (0,0) and end points.
         */
        private fun isCurveStraightLine(data: List<Double>, error: Double): Boolean {
            var i = data.size - 2
            val a = -data[i + 1]
            val b = data[i]
            val d = 1.0 / (a * a + b * b)
            if (i <= 1 || !d.isFinite()) return false
            i -= 2
            while (i >= 0) {
                if (sqrt((a * data[i] + b * data[i + 1]).pow(2) * d) > error) {
                    return false
                }
                i -= 2
            }
            return true
        }

        /**
         * Calculates the sagitta of an arc if possible.
         */
        private fun calculateSagitta(data: List<Double>): Double? {
            if (data[3] == 1.0) return null
            val rx = data[0]
            val ry = data[1]
            if (abs(rx - ry) > EPSILON) return null
            val chord = kotlin.math.hypot(data[ARC_END_X_INDEX], data[ARC_END_Y_INDEX])
            if (chord > rx * 2) return null
            return rx - sqrt(rx * rx - 0.25 * chord * chord)
        }

        /**
         * Converts a shorthand curve to its longhand form.
         */
        private fun makeLonghand(item: PathDataItem, prevData: List<Double>) {
            when (item.command) {
                's' -> item.command = 'c'
                't' -> item.command = 'q'
            }
            val n = prevData.size
            item.args.add(index = 0, element = prevData[n - 1] - prevData[n - 3])
            item.args.add(index = 0, element = prevData[n - 2] - prevData[n - 4])
        }

        /**
         * Result of tryMakeArcs: either skip the current item entirely, or
         * replace it with a new command/data pair.
         */
        private data class MakeArcsResult(
            val command: Char,
            val data: MutableList<Double>,
            val skip: Boolean = false,
        )

        /**
         * Compute the distance between two points.
         */
        private fun getDistance(point1: DoubleArray, point2: DoubleArray): Double =
            hypot(point1[0] - point2[0], point1[1] - point2[1])

        /**
         * Returns coordinates of the cubic bezier point for a given t.
         */
        private fun getCubicBezierPoint(curve: List<Double>, t: Double): DoubleArray {
            val sqrT = t * t
            val cubT = sqrT * t
            val mt = 1 - t
            val sqrMt = mt * mt
            return doubleArrayOf(
                3 * sqrMt * t * curve[0] + 3 * mt * sqrT * curve[2] + cubT * curve[4],
                3 * sqrMt * t * curve[1] + 3 * mt * sqrT * curve[3] + cubT * curve[5],
            )
        }

        /**
         * Computes line equations by two points and returns their intersection point.
         */
        private fun getIntersection(coords: DoubleArray): DoubleArray? {
            val a1 = coords[1] - coords[3]
            val b1 = coords[2] - coords[0]
            val c1 = coords[0] * coords[3] - coords[2] * coords[1]
            val a2 = coords[5] - coords[7]
            val b2 = coords[6] - coords[4]
            val c2 = coords[4] * coords[7] - coords[5] * coords[6]
            val denom = a1 * b2 - a2 * b1
            if (denom == 0.0) return null
            val cross = doubleArrayOf(
                (b1 * c2 - b2 * c1) / denom,
                (a1 * c2 - a2 * c1) / -denom,
            )
            if (cross[0].isNaN() || cross[1].isNaN() || !cross[0].isFinite() || !cross[1].isFinite()) {
                return null
            }
            return cross
        }

        /**
         * Checks if a curve is convex by verifying control points lie in expected region.
         */
        private fun isConvex(data: List<Double>): Boolean {
            val center = getIntersection(
                doubleArrayOf(0.0, 0.0, data[2], data[3], data[0], data[1], data[4], data[5])
            ) ?: return false
            return (data[2] < center[0]) == (center[0] < 0.0) &&
                (data[3] < center[1]) == (center[1] < 0.0) &&
                (data[4] < center[0]) == (center[0] < data[0]) &&
                (data[5] < center[1]) == (center[1] < data[1])
        }

        /**
         * Finds a circle by 3 points of the curve and checks if the curve fits it.
         */
        private fun findCircle(
            curve: List<Double>,
            arcThreshold: Double,
            arcTolerance: Double,
            error: Double,
        ): Circle? {
            val midPoint = getCubicBezierPoint(curve, t = 0.5)
            val m1 = doubleArrayOf(midPoint[0] / 2, midPoint[1] / 2)
            val m2 = doubleArrayOf((midPoint[0] + curve[4]) / 2, (midPoint[1] + curve[5]) / 2)
            val center = getIntersection(
                doubleArrayOf(
                    m1[0], m1[1],
                    m1[0] + m1[1], m1[1] - m1[0],
                    m2[0], m2[1],
                    m2[0] + (m2[1] - midPoint[1]), m2[1] - (m2[0] - midPoint[0]),
                )
            ) ?: return null
            val radius = getDistance(doubleArrayOf(0.0, 0.0), center)
            val tolerance = min(arcThreshold * error, arcTolerance * radius / 100)

            if (radius >= 1e15) return null
            val fits = doubleArrayOf(0.25, 0.75).all { t ->
                abs(getDistance(getCubicBezierPoint(curve, t), center) - radius) <= tolerance
            }
            if (!fits) return null
            return Circle(center = center, radius = radius)
        }

        /**
         * Checks if a curve fits the given circle.
         */
        private fun isArc(
            curve: List<Double>,
            circle: Circle,
            arcThreshold: Double,
            arcTolerance: Double,
            error: Double,
        ): Boolean {
            val tolerance = min(arcThreshold * error, arcTolerance * circle.radius / 100)
            return doubleArrayOf(0.0, 0.25, 0.5, 0.75, 1.0).all { t ->
                abs(getDistance(getCubicBezierPoint(curve, t), circle.center) - circle.radius) <= tolerance
            }
        }

        /**
         * Checks if a previous curve fits the given circle
         * (adjusted by the curve's endpoint offset).
         */
        private fun isArcPrev(
            curve: List<Double>,
            circle: Circle,
            arcThreshold: Double,
            arcTolerance: Double,
            error: Double,
        ): Boolean = isArc(
            curve = curve,
            circle = Circle(
                center = doubleArrayOf(circle.center[0] + curve[4], circle.center[1] + curve[5]),
                radius = circle.radius,
            ),
            arcThreshold = arcThreshold,
            arcTolerance = arcTolerance,
            error = error,
        )

        /**
         * Finds angle of a curve fitting the given arc.
         */
        private fun findArcAngle(curve: List<Double>, relCircle: Circle): Double {
            val x1 = -relCircle.center[0]
            val y1 = -relCircle.center[1]
            val x2 = curve[4] - relCircle.center[0]
            val y2 = curve[5] - relCircle.center[1]
            return acos(
                (x1 * x2 + y1 * y2) / sqrt((x1 * x1 + y1 * y1) * (x2 * x2 + y2 * y2))
            )
        }

        /**
         * Converts path data to string for length comparison during makeArcs.
         */
        private fun data2Path(params: ConvertPathDataParams, pathData: List<PathDataItem>, precision: Int): String {
            val sb = StringBuilder()
            for (item in pathData) {
                sb.append(item.command)
                if (item.args.isNotEmpty()) {
                    val rounded = item.args.toMutableList()
                    strongRound(data = rounded, precision = precision)
                    sb.append(stringifyArgs(data = rounded, precision = precision, params = params))
                }
            }
            return sb.toString()
        }

        /**
         * Try converting cubic curves to arcs.
         * Mirrors the makeArcs logic from the JS reference.
         */
        @Suppress(
            "CyclomaticComplexity",
            "CyclomaticComplexMethod",
            "LongMethod",
            "NestedBlockDepth",
            "LongParameterList",
            "ReturnCount",
        )
        private fun tryMakeArcs(
            path: MutableList<PathDataItem>,
            index: Int,
            command: Char,
            sdata: List<Double>,
            item: PathDataItem,
            prev: PathDataItem,
            params: ConvertPathDataParams,
            precision: Int,
            error: Double,
            arcThreshold: Double,
            arcTolerance: Double,
            relSubpoint: DoubleArray,
        ): MakeArcsResult? {
            if (command != 'c' && command != 's') return null
            if (!isConvex(sdata)) return null
            val circle = findCircle(
                curve = sdata,
                arcThreshold = arcThreshold,
                arcTolerance = arcTolerance,
                error = error,
            ) ?: return null

            val radiusList = mutableListOf(circle.radius)
            strongRound(data = radiusList, precision = precision)
            val r = radiusList.first()
            var angle = findArcAngle(sdata, circle)
            val sweep = if (sdata[5] * sdata[0] - sdata[4] * sdata[1] > 0) 1.0 else 0.0
            var arc = PathDataItem(
                command = 'a',
                args = mutableListOf(r, r, 0.0, 0.0, sweep, sdata[4], sdata[5]),
            )
            arc.coords = item.coords?.copyOf()
            arc.base = item.base

            val output = mutableListOf(arc)
            val relCenter = doubleArrayOf(circle.center[0] - sdata[4], circle.center[1] - sdata[5])
            val relCircle = Circle(center = relCenter, radius = circle.radius)
            val arcCurves = mutableListOf(item)
            var hasPrev = 0
            var suffix = ""

            // Check if previous curve also fits the same arc
            if ((prev.command == 'c' && isConvex(prev.args) &&
                    isArcPrev(prev.args, circle, arcThreshold, arcTolerance, error)) ||
                (prev.command == 'a' && prev.sdata != null &&
                    isArcPrev(requireNotNull(prev.sdata), circle, arcThreshold, arcTolerance, error))
            ) {
                arcCurves.add(index = 0, element = prev)
                arc.base = prev.base
                val arcBase = requireNotNull(arc.base)
                val arcCoords = requireNotNull(arc.coords)
                arc.args[ARC_END_X_INDEX] = arcCoords[0] - arcBase[0]
                arc.args[ARC_END_Y_INDEX] = arcCoords[1] - arcBase[1]
                val prevData = if (prev.command == 'a') requireNotNull(prev.sdata) else prev.args
                val prevAngle = findArcAngle(
                    prevData,
                    Circle(
                        center = doubleArrayOf(prevData[4] + circle.center[0], prevData[5] + circle.center[1]),
                        radius = circle.radius,
                    ),
                )
                angle += prevAngle
                if (angle > PI) {
                    arc.args[ARC_LARGE_ARC_INDEX] = 1.0
                }
                hasPrev = 1
            }

            // Check if next curves fit the arc
            var j = index
            while (true) {
                j++
                val nextItem = path.getOrNull(j) ?: break
                if (nextItem.command != 'c' && nextItem.command != 's') break
                var nextData = nextItem.args.toList()
                var nextLonghand: PathDataItem? = null
                if (nextItem.command == 's') {
                    nextLonghand = PathDataItem(
                        command = 's',
                        args = nextItem.args.toMutableList(),
                    )
                    makeLonghand(item = nextLonghand, prevData = path[j - 1].args)
                    nextData = nextLonghand.args.toList()
                    val firstTwo = nextData.subList(fromIndex = 0, toIndex = 2)
                    nextLonghand.args.clear()
                    nextLonghand.args.addAll(firstTwo)
                    suffix = data2Path(params, listOf(nextLonghand), precision)
                }
                if (isConvex(nextData) && isArc(nextData, relCircle, arcThreshold, arcTolerance, error)) {
                    angle += findArcAngle(nextData, relCircle)
                    if (angle - 2 * PI > 1e-3) break
                    if (angle > PI) {
                        arc.args[ARC_LARGE_ARC_INDEX] = 1.0
                    }
                    arcCurves.add(nextItem)
                    if (2 * PI - angle > 1e-3) {
                        // less than 360 degrees
                        arc.coords = nextItem.coords
                        val arcBase = requireNotNull(arc.base)
                        val arcCoords = requireNotNull(arc.coords)
                        arc.args[ARC_END_X_INDEX] = arcCoords[0] - arcBase[0]
                        arc.args[ARC_END_Y_INDEX] = arcCoords[1] - arcBase[1]
                    } else {
                        // full circle - make half-circle arc and add second one
                        arc.args[ARC_END_X_INDEX] = 2 * (relCircle.center[0] - nextData[4])
                        arc.args[ARC_END_Y_INDEX] = 2 * (relCircle.center[1] - nextData[5])
                        val arcBase = requireNotNull(arc.base)
                        arc.coords = doubleArrayOf(
                            arcBase[0] + arc.args[ARC_END_X_INDEX],
                            arcBase[1] + arc.args[ARC_END_Y_INDEX],
                        )
                        val nextCoords = requireNotNull(nextItem.coords)
                        val arcCoords = requireNotNull(arc.coords)
                        arc = PathDataItem(
                            command = 'a',
                            args = mutableListOf(
                                r, r, 0.0, 0.0, sweep,
                                nextCoords[0] - arcCoords[0],
                                nextCoords[1] - arcCoords[1],
                            ),
                        )
                        arc.coords = nextItem.coords
                        arc.base = arcCoords
                        output.add(arc)
                        j++
                        break
                    }
                    relCenter[0] -= nextData[4]
                    relCenter[1] -= nextData[5]
                } else {
                    break
                }
            }

            val stringify = { items: List<PathDataItem> -> data2Path(params, items, precision) }
            if ((stringify(output) + suffix).length >= stringify(arcCurves).length) {
                return null
            }

            // The arc is shorter - apply it
            if (path.getOrNull(j) != null && path[j].command == 's') {
                makeLonghand(item = path[j], prevData = path[j - 1].args)
            }
            if (hasPrev > 0) {
                val prevArc = output.removeFirst()
                strongRound(data = prevArc.args, precision = precision)
                relSubpoint[0] += prevArc.args[ARC_END_X_INDEX] - prev.args[prev.args.size - 2]
                relSubpoint[1] += prevArc.args[ARC_END_Y_INDEX] - prev.args[prev.args.size - 1]
                prev.command = 'a'
                prev.args.clear()
                prev.args.addAll(prevArc.args)
                item.base = prevArc.coords
                prev.coords = prevArc.coords
            }
            val finalArc = output.removeFirstOrNull()
            if (arcCurves.size == 1) {
                item.sdata = sdata.toList()
            } else if (arcCurves.size - 1 - hasPrev > 0) {
                // Remove consumed next items and insert remaining output arcs
                val removeCount = arcCurves.size - 1 - hasPrev
                for (k in 0 until removeCount) {
                    if (index + 1 < path.size) {
                        path.removeAt(index + 1)
                    }
                }
                for ((k, outArc) in output.withIndex()) {
                    path.add(index + 1 + k, outArc)
                }
            }
            if (finalArc == null) {
                return MakeArcsResult(command = ' ', data = mutableListOf(), skip = true)
            }
            item.command = 'a'
            item.args.clear()
            item.args.addAll(finalArc.args)
            item.coords = finalArc.coords
            return MakeArcsResult(command = 'a', data = item.args)
        }

        /**
         * Simple data class representing a circle found during makeArcs.
         */
        private data class Circle(
            val center: DoubleArray,
            val radius: Double,
        )

        /**
         * Writes data in shortest form using absolute or relative coordinates.
         * Converts relative commands back to absolute where shorter.
         */
        @Suppress("CyclomaticComplexity", "CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth")
        private fun convertToMixed(
            path: MutableList<PathDataItem>,
            params: ConvertPathDataParams,
            precision: Int,
        ): MutableList<PathDataItem> {
            if (path.isEmpty()) return path

            var prev = path[0]
            val result = mutableListOf(prev)

            for (index in 1 until path.size) {
                val item = path[index]
                if (item.command == 'Z' || item.command == 'z') {
                    prev = item
                    result.add(item)
                    continue
                }

                val command = item.command
                val data = item.args
                val itemBase = item.base

                // Compute absolute data
                val adata = data.toMutableList()
                if (itemBase != null) {
                    when (command) {
                        'm', 'l', 't', 'q', 's', 'c' -> {
                            for (i in adata.indices) {
                                adata[i] += itemBase[i % 2]
                            }
                        }
                        'h' -> adata[0] += itemBase[0]
                        'v' -> adata[0] += itemBase[1]
                        'a' -> {
                            adata[ARC_END_X_INDEX] += itemBase[0]
                            adata[ARC_END_Y_INDEX] += itemBase[1]
                        }
                    }
                }

                val rdata = data.toMutableList()
                applyRound(adata, precision)
                applyRound(rdata, precision)

                val absoluteStr = stringifyArgs(adata, precision, params)
                val relativeStr = stringifyArgs(rdata, precision, params)

                // Convert to absolute if shorter (or forced)
                if (params.forceAbsolutePath ||
                    (absoluteStr.length < relativeStr.length &&
                        !(params.negativeExtraSpace &&
                            command == prev.command &&
                            prev.command.code > 96 &&
                            absoluteStr.length == relativeStr.length - 1 &&
                            (data[0] < 0 ||
                                (kotlin.math.floor(data[0]) == 0.0 &&
                                    data[0] % 1.0 != 0.0 &&
                                    prev.args[prev.args.size - 1] % 1.0 != 0.0))))
                ) {
                    item.command = command.uppercaseChar()
                    item.args.clear()
                    item.args.addAll(adata)
                }

                prev = item
                result.add(item)
            }

            return result
        }

        /**
         * Stringify arguments for length comparison in convertToMixed.
         * Mirrors cleanupOutData from the JS reference.
         */
        private fun stringifyArgs(
            data: List<Double>,
            precision: Int,
            params: ConvertPathDataParams,
        ): String {
            val sb = StringBuilder()
            var prev: Double? = null
            for (i in data.indices) {
                val value = data[i]
                val itemStr = if (params.leadingZero) removeLeadingZero(value) else value.toString()

                val delimiter = when {
                    i == 0 -> ""
                    params.negativeExtraSpace && (value < 0 ||
                        (itemStr.startsWith('.') && prev != null && prev % 1.0 != 0.0)) -> ""
                    else -> " "
                }

                sb.append(delimiter)
                sb.append(itemStr)
                prev = value
            }
            return sb.toString()
        }

        private fun removeLeadingZero(value: Double): String {
            val isInt = value % 1.0 == 0.0
            val str = if (isInt && !value.toString().contains('E', ignoreCase = true)) {
                value.toLong().toString()
            } else {
                doubleToJsString(value)
            }
            return when {
                value > 0.0 && value < 1.0 && str.startsWith('0') -> str.substring(1)
                value > -1.0 && value < 0.0 && str.length > 1 && str[1] == '0' ->
                    str[0].toString() + str.substring(2)
                else -> str
            }
        }

        /**
         * Format a Double to match JS Number.toString() scientific notation.
         */
        private fun doubleToJsString(value: Double): String {
            val raw = value.toString()
            val eIdx = raw.indexOf('E', ignoreCase = true)
            if (eIdx >= 0) {
                var mantissa = raw.substring(startIndex = 0, endIndex = eIdx)
                val exponent = raw.substring(startIndex = eIdx + 1)
                if (mantissa.endsWith(".0")) {
                    mantissa = mantissa.dropLast(n = 2)
                }
                return mantissa + "e" + exponent
            }
            return raw
        }
    }
}
