package svgokt.plugins.builtin

import svgokt.Tools
import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.css.ComputedStyles
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.path.PathDataItem
import svgokt.path.intersects
import svgokt.path.js2path
import svgokt.path.path2js
import svgokt.plugins.xast.collectStylesheet
import svgokt.style.computeStyle

/**
 * Parameters for the MergePaths plugin.
 *
 * @property force Whether to force merging without intersection checks.
 * @property floatPrecision Precision for floating point numbers in the output.
 * @property noSpaceAfterFlags Whether to disable space after arc flags.
 */
data class MergePathsParams(
    val force: Boolean = false,
    val floatPrecision: Int = DEFAULT_FLOAT_PRECISION,
    val noSpaceAfterFlags: Boolean = false,
) : PluginParams,
    Map<String, Any> by mapOf(
        "force" to force,
        "floatPrecision" to floatPrecision,
        "noSpaceAfterFlags" to noSpaceAfterFlags,
    )

/**
 * Merges multiple consecutive path elements with identical attributes
 * (except for the `d` attribute) into a single path element.
 */
object MergePaths : Plugin<MergePathsParams> {
    override val name: String = "mergePaths"
    override val description: String = "merges multiple paths in one if possible"
    override val params: MergePathsParams = MergePathsParams()
    override val fn: PluginFn = { root, params, _ ->
        val mergeParams = params as? MergePathsParams ?: MergePathsParams()
        val stylesheet = collectStylesheet(root)

        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    onEnter(
                        node = node,
                        params = mergeParams,
                        stylesheet = stylesheet,
                    )
                },
            ),
        )
    }

    private fun onEnter(
        node: XastElement,
        params: MergePathsParams,
        stylesheet: svgokt.domain.css.Stylesheet,
    ): VisitState {
        if (node.children.size <= 1) {
            return VisitState.Continue
        }

        val state = MergeState(params = params, stylesheet = stylesheet)
        state.prevChild = node.children[0]

        for (i in 1 until node.children.size) {
            state.processChild(child = node.children[i])
        }

        state.flushPending()
        node.children.removeAll(state.elementsToRemove.toSet())

        return VisitState.Continue
    }

    /**
     * Mutable state tracker for the merge iteration, extracted to reduce
     * cyclomatic complexity in the main loop.
     */
    private class MergeState(
        val params: MergePathsParams,
        val stylesheet: svgokt.domain.css.Stylesheet,
    ) {
        val elementsToRemove = mutableListOf<XastChild>()
        var prevChild: XastChild? = null
        var prevPathData: MutableList<PathDataItem>? = null

        fun processChild(child: XastChild) {
            val prev = prevChild

            // Check prevChild is a valid path
            if (!isEligiblePath(prev)) {
                flushAndAdvance(child)
                return
            }

            // Check child is a valid path
            if (!isEligiblePath(child)) {
                flushAndAdvance(child)
                return
            }

            val childElement = child as XastElement

            // Check computed style for URL references that prevent merging
            if (hasUnsafeMergeStyles(childElement)) {
                flushAndAdvance(child)
                return
            }

            val prevElement = prev as XastElement

            // Check attribute count match
            if (childElement.attributes.size != prevElement.attributes.size) {
                flushAndAdvance(child)
                return
            }

            // Check all attributes match (except d)
            val attrsDiffer = childElement.attributes.keys.any { attr ->
                attr != "d" && prevElement.attributes[attr] != childElement.attributes[attr]
            }
            if (attrsDiffer) {
                flushAndAdvance(child)
                return
            }

            val hasPrevPath = prevPathData != null
            val currentPathData = path2js(childElement)
            if (prevPathData == null) {
                prevPathData = path2js(prevElement).map { item ->
                    PathDataItem(command = item.command, args = item.args.toMutableList())
                }.toMutableList()
            }

            val prevData = checkNotNull(prevPathData)

            if (params.force || !intersects(prevData, currentPathData)) {
                prevData.addAll(currentPathData)
                elementsToRemove.add(child)
                return
            }

            // Paths intersect - flush and advance
            if (hasPrevPath) {
                val element = prevChild as? XastElement
                if (element != null) {
                    updatePreviousPath(
                        element = element,
                        pathData = prevData,
                        params = params,
                    )
                }
            }
            prevChild = child
            prevPathData = null
        }

        fun flushPending() {
            val data = prevPathData ?: return
            val element = prevChild as? XastElement ?: return
            updatePreviousPath(element = element, pathData = data, params = params)
            prevPathData = null
        }

        private fun hasUnsafeMergeStyles(child: XastElement): Boolean {
            val computed = computeStyle(stylesheet, child)

            // Check for markers, clip-path, mask, mask-image via attributes and inline styles
            if (hasMarkerOrClipMask(child)) return true

            // Check for URL references in fill, filter, stroke via attributes
            for (attrName in URL_REF_ATTRS) {
                val attrVal = child.attributes[attrName]
                if (attrVal != null && Tools.includesUrlReference(attrVal)) {
                    return true
                }
            }

            // Check inline style for URL references
            val style = child.attributes["style"]
            if (style != null && Tools.includesUrlReference(style)) {
                return true
            }

            // Check computed style (stub returns DynamicStyle, but check anyway)
            if (computed is ComputedStyles.StaticStyle) {
                if (Tools.includesUrlReference(computed.value)) return true
            }

            return false
        }

        private fun hasMarkerOrClipMask(child: XastElement): Boolean {
            for (attr in MARKER_AND_CLIP_ATTRS) {
                if (child.attributes.containsKey(attr)) return true
            }
            // Check inline style for these properties
            val style = child.attributes["style"] ?: return false
            return MARKER_AND_CLIP_STYLE_PATTERNS.any { it in style }
        }

        private fun flushAndAdvance(child: XastChild) {
            flushPending()
            prevChild = child
        }
    }

    private val URL_REF_ATTRS = listOf("fill", "filter", "stroke")
    private val MARKER_AND_CLIP_ATTRS = listOf(
        "marker-start", "marker-mid", "marker-end",
        "clip-path", "mask", "mask-image",
    )
    private val MARKER_AND_CLIP_STYLE_PATTERNS = listOf(
        "marker-start", "marker-mid", "marker-end",
        "clip-path", "mask-image", "mask:",
    )

    private fun isEligiblePath(child: XastChild?): Boolean {
        if (child !is XastElement) return false
        if (child.name != "path") return false
        if (child.children.isNotEmpty()) return false
        if (child.attributes["d"] == null) return false
        return true
    }

    private fun updatePreviousPath(
        element: XastElement,
        pathData: List<PathDataItem>,
        params: MergePathsParams,
    ) {
        js2path(
            element = element,
            data = pathData,
            precision = params.floatPrecision,
            noSpaceAfterFlags = params.noSpaceAfterFlags,
        )
    }
}

private const val DEFAULT_FLOAT_PRECISION = 3
