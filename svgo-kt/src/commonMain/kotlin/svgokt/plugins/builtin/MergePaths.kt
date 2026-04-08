package svgokt.plugins.builtin

import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.path.PathDataItem
import svgokt.path.js2path
import svgokt.path.path2js

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
    override val fn: PluginFn = { _, params, _ ->
        val mergeParams = params as? MergePathsParams ?: MergePathsParams()

        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    onEnter(node = node, params = mergeParams)
                },
            ),
        )
    }

    private fun onEnter(
        node: XastElement,
        params: MergePathsParams,
    ): VisitState {
        if (node.children.size <= 1) {
            return VisitState.Continue
        }

        val state = MergeState(params = params)
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
    private class MergeState(val params: MergePathsParams) {
        val elementsToRemove = mutableListOf<XastChild>()
        var prevChild: XastChild? = null
        var prevPathData: MutableList<PathDataItem>? = null

        fun processChild(child: XastChild) {
            val canMerge = canMergePair(child)
            if (!canMerge) {
                flushAndAdvance(child)
                return
            }

            val prevElement = prevChild as XastElement
            val childElement = child as XastElement
            val currentPathData = path2js(childElement)

            if (prevPathData == null) {
                prevPathData = path2js(prevElement).map { item ->
                    PathDataItem(command = item.command, args = item.args.toMutableList())
                }.toMutableList()
            }

            prevPathData?.addAll(currentPathData)
            elementsToRemove.add(child)
        }

        fun flushPending() {
            val data = prevPathData ?: return
            val element = prevChild as? XastElement ?: return
            updatePreviousPath(element = element, pathData = data, params = params)
            prevPathData = null
        }

        private fun canMergePair(child: XastChild): Boolean {
            if (!isEligiblePath(prevChild)) return false
            if (!isEligiblePath(child)) return false
            val prevElement = prevChild as XastElement
            val childElement = child as XastElement
            return haveMatchingAttributes(prev = prevElement, current = childElement)
        }

        private fun flushAndAdvance(child: XastChild) {
            flushPending()
            prevChild = child
        }
    }

    private fun isEligiblePath(child: XastChild?): Boolean {
        if (child !is XastElement) return false
        if (child.name != "path") return false
        if (child.children.isNotEmpty()) return false
        if (child.attributes["d"] == null) return false
        return true
    }

    private fun haveMatchingAttributes(
        prev: XastElement,
        current: XastElement,
    ): Boolean {
        val prevAttrs = prev.attributes
        val currentAttrs = current.attributes

        if (prevAttrs.size != currentAttrs.size) return false

        return currentAttrs.keys.none { attr ->
            attr != "d" && prevAttrs[attr] != currentAttrs[attr]
        }
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
