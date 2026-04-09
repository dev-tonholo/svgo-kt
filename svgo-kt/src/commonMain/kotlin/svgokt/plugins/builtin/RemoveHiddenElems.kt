@file:Suppress("TooManyFunctions")

package svgokt.plugins.builtin

import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.XastRoot
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.domain.plugins.VisitorRoot
import svgokt.path.parsePathData
import svgokt.plugins.Collections
import svgokt.plugins.xast.collectStylesheet
import svgokt.plugins.xast.detachFromParent
import svgokt.style.computeStyle
import svgokt.xast.querySelector

/**
 * Removes hidden elements that are not rendered:
 * - Elements with display="none"
 * - Elements with visibility="hidden"
 * - Elements with opacity="0"
 * - Circles with r="0"
 * - Ellipses with rx="0" or ry="0"
 * - Rectangles with width="0" or height="0"
 * - Paths with empty d attribute
 * - Images with width="0" or height="0"
 * - Patterns with width="0" or height="0"
 * - Polylines/polygons with missing points
 *
 * Also removes non-rendering elements that are unreferenced,
 * tracks removed defs to clean up <use> references,
 * and removes empty <defs> elements.
 */
object RemoveHiddenElems : Plugin<RemoveHiddenElems.Params> {
    data class Params(
        val isHidden: Boolean = true,
        val displayNone: Boolean = true,
        val opacity0: Boolean = true,
        val circleR0: Boolean = true,
        val ellipseRX0: Boolean = true,
        val ellipseRY0: Boolean = true,
        val rectWidth0: Boolean = true,
        val rectHeight0: Boolean = true,
        val patternWidth0: Boolean = true,
        val patternHeight0: Boolean = true,
        val imageWidth0: Boolean = true,
        val imageHeight0: Boolean = true,
        val pathEmptyD: Boolean = true,
        val polylineEmptyPoints: Boolean = true,
        val polygonEmptyPoints: Boolean = true,
    ) : PluginParams,
        Map<String, Any> by mapOf(
            "isHidden" to isHidden,
            "displayNone" to displayNone,
            "opacity0" to opacity0,
            "circleR0" to circleR0,
            "ellipseRX0" to ellipseRX0,
            "ellipseRY0" to ellipseRY0,
            "rectWidth0" to rectWidth0,
            "rectHeight0" to rectHeight0,
            "patternWidth0" to patternWidth0,
            "patternHeight0" to patternHeight0,
            "imageWidth0" to imageWidth0,
            "imageHeight0" to imageHeight0,
            "pathEmptyD" to pathEmptyD,
            "polylineEmptyPoints" to polylineEmptyPoints,
            "polygonEmptyPoints" to polygonEmptyPoints,
        )

    override val name: String = "removeHiddenElems"
    override val description: String =
        "removes hidden elements (zero sized, with absent attributes)"
    override val params: Params = Params()
    override val fn: PluginFn = { root, pluginParams, _ ->
        val resolvedParams = pluginParams as? Params ?: Params()
        val stylesheet = collectStylesheet(root)
        val state = PluginState()

        // Pre-pass: handle opacity=0 and non-rendering elements
        preVisit(root = root, params = resolvedParams, state = state, stylesheet = stylesheet)

        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    onEnter(
                        node = node,
                        parentNode = parentNode,
                        params = resolvedParams,
                        state = state,
                        stylesheet = stylesheet,
                    )
                },
            ),
            root = VisitorRoot(
                onExit = { _, _ ->
                    onRootExit(state = state)
                },
            ),
        )
    }

    /**
     * Pre-visit pass to handle non-rendering elements and opacity=0 nodes.
     * Mirrors the JS `visit(root, ...)` call at the top of the plugin.
     *
     * Also detects `<style>` elements to set the deoptimized flag early,
     * so that CSS-class-overridden opacity attributes are not removed.
     */
    private fun preVisit(
        root: XastRoot,
        params: Params,
        state: PluginState,
        stylesheet: svgokt.domain.css.Stylesheet,
    ) {
        // First, detect style elements to set deoptimized flag
        detectStyleElements(root, state)

        fun visitNode(node: XastChild, parent: XastParent) {
            if (node !is XastElement) return

            // Non-rendering elements are tracked, not visited further
            if (node.name in Collections.nonRenderingElements) {
                state.nonRenderedNodes[node] = parent
                return
            }

            // opacity="0" check (attribute-based since computeStyle is a stub)
            if (params.opacity0 && getOpacity(node) == "0") {
                // If styles exist and element has a class, CSS may override - skip
                if (state.deoptimized && node.attributes.containsKey("class")) {
                    // CSS class may override opacity, don't remove
                } else {
                    if (node.name == "path") {
                        state.nonRenderedNodes[node] = parent
                        return
                    }
                    removeElement(node = node, parentNode = parent, state = state)
                    return
                }
            }

            // Recurse into children (copy to avoid ConcurrentModification)
            val children = node.children.toList()
            for (child in children) {
                visitNode(node = child, parent = node)
            }
        }

        val children = root.children.toList()
        for (child in children) {
            visitNode(node = child, parent = root)
        }
    }

    private fun detectStyleElements(parent: XastParent, state: PluginState) {
        for (child in parent.children) {
            if (child is XastElement) {
                if ((child.name == "style" && child.children.isNotEmpty()) || isScript(child)) {
                    state.deoptimized = true
                    return
                }
                detectStyleElements(parent = child, state = state)
            }
        }
    }

    private fun onEnter(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
        state: PluginState,
        stylesheet: svgokt.domain.css.Stylesheet,
    ): VisitState {
        // Track style and script elements for deoptimization
        if ((node.name == "style" && node.children.isNotEmpty()) || isScript(node)) {
            state.deoptimized = true
            return VisitState.Continue
        }

        // Track defs for cleanup
        if (node.name == "defs" && parentNode != null) {
            state.allDefs[node] = parentNode
        }

        // Track <use> references
        if (node.name == "use") {
            trackUseReferences(node = node, parentNode = parentNode, state = state)
        }

        // Non-rendering elements are handled in the pre-visit pass.
        // Skip this node and its children (matches JS visitSkip behavior).
        // References from non-rendering elements' children are collected
        // via collectChildReferences to ensure proper reference tracking.
        if (node.name in Collections.nonRenderingElements) {
            collectAllNestedReferences(node = node, state = state)
            return VisitState.Skip
        }

        // Zero-sized elements
        if (tryRemoveZeroSized(node, parentNode, params, state)) return VisitState.Continue

        // Polyline/polygon empty points
        if (tryRemoveEmptyPoints(node, parentNode, params, state)) return VisitState.Continue

        // Visibility hidden
        if (tryRemoveHidden(node, parentNode, params, state)) return VisitState.Continue

        // Display none
        if (tryRemoveDisplayNone(node, parentNode, params, state)) return VisitState.Continue

        // Empty path data
        if (tryRemoveEmptyPath(node, parentNode, params, state, stylesheet)) return VisitState.Continue

        // Collect all references from attributes
        collectReferences(node = node, state = state)

        return VisitState.Continue
    }

    private fun onRootExit(state: PluginState) {
        // Remove <use> elements that referenced removed defs
        for (id in state.removedDefIds) {
            val refs = state.referencesById[id] ?: continue
            for (ref in refs) {
                val parent = ref.parentNode ?: continue
                ref.node.detachFromParent(parent)
            }
        }

        // Remove unreferenced non-rendering elements
        if (!state.deoptimized) {
            for ((node, parent) in state.nonRenderedNodes) {
                if (canRemoveNonRenderingNode(node = node, allReferences = state.allReferences)) {
                    node.detachFromParent(parent)
                }
            }
        }

        // Remove empty defs
        for ((node, parent) in state.allDefs) {
            if (node.children.isEmpty()) {
                node.detachFromParent(parent)
            }
        }
    }

    private fun removeElement(
        node: XastChild,
        parentNode: XastParent,
        state: PluginState,
    ) {
        if (
            node is XastElement &&
            node.attributes.containsKey("id") &&
            parentNode is XastElement &&
            parentNode.name == "defs"
        ) {
            val id = node.attributes["id"]
            if (id != null) {
                state.removedDefIds.add(id)
            }
        }
        node.detachFromParent(parentNode)
    }

    private fun tryRemoveZeroSized(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
        state: PluginState,
    ): Boolean {
        if (node.children.isNotEmpty()) return false
        val shouldRemove = when (node.name) {
            "circle" -> params.circleR0 && node.attributes["r"] == "0"
            "ellipse" -> (params.ellipseRX0 && node.attributes["rx"] == "0") ||
                (params.ellipseRY0 && node.attributes["ry"] == "0")
            "rect" -> (params.rectWidth0 && node.attributes["width"] == "0") ||
                (params.rectHeight0 && params.rectWidth0 && node.attributes["height"] == "0")
            "pattern" -> (params.patternWidth0 && node.attributes["width"] == "0") ||
                (params.patternHeight0 && node.attributes["height"] == "0")
            "image" -> (params.imageWidth0 && node.attributes["width"] == "0") ||
                (params.imageHeight0 && node.attributes["height"] == "0")
            else -> false
        }
        if (shouldRemove && parentNode != null) {
            removeElement(node = node, parentNode = parentNode, state = state)
        }
        return shouldRemove
    }

    private fun tryRemoveEmptyPoints(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
        state: PluginState,
    ): Boolean {
        val shouldRemove = when (node.name) {
            "polyline" -> params.polylineEmptyPoints && !node.attributes.containsKey("points")
            "polygon" -> params.polygonEmptyPoints && !node.attributes.containsKey("points")
            else -> false
        }
        if (shouldRemove && parentNode != null) {
            removeElement(node = node, parentNode = parentNode, state = state)
        }
        return shouldRemove
    }

    private fun tryRemoveHidden(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
        state: PluginState,
    ): Boolean {
        if (!params.isHidden) return false
        val visibility = node.attributes["visibility"] ?: return false
        if (visibility != "hidden") return false

        // If styles exist and element has a class, CSS may override visibility
        if (state.deoptimized && node.attributes.containsKey("class")) return false

        // Keep if any descendant enables visibility
        if (querySelector(node, "[visibility=visible]") != null) return false

        if (parentNode != null) {
            removeElement(node = node, parentNode = parentNode, state = state)
        }
        return true
    }

    private fun tryRemoveDisplayNone(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
        state: PluginState,
    ): Boolean {
        if (!params.displayNone) return false
        if (node.attributes["display"] != "none") return false
        // markers with display:none are still rendered
        if (node.name == "marker") return false

        // If styles exist and element has a class, CSS may override display
        if (state.deoptimized && node.attributes.containsKey("class")) return false

        if (parentNode != null) {
            removeElement(node = node, parentNode = parentNode, state = state)
        }
        return true
    }

    @Suppress("ReturnCount")
    private fun tryRemoveEmptyPath(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
        state: PluginState,
        stylesheet: svgokt.domain.css.Stylesheet,
    ): Boolean {
        if (!params.pathEmptyD) return false
        if (node.name != "path") return false

        val d = node.attributes["d"]
        if (d == null) {
            if (parentNode != null) {
                removeElement(node = node, parentNode = parentNode, state = state)
            }
            return true
        }

        val pathData = parsePathData(d)
        if (pathData.isEmpty()) {
            if (parentNode != null) {
                removeElement(node = node, parentNode = parentNode, state = state)
            }
            return true
        }

        // Keep single point paths for markers
        if (pathData.size == 1) {
            val hasMarkerStart = node.attributes.containsKey("marker-start")
            val hasMarkerEnd = node.attributes.containsKey("marker-end")
            if (!hasMarkerStart && !hasMarkerEnd) {
                if (parentNode != null) {
                    removeElement(node = node, parentNode = parentNode, state = state)
                }
                return true
            }
        }

        return false
    }

    private fun trackUseReferences(
        node: XastElement,
        parentNode: XastParent?,
        state: PluginState,
    ) {
        for (attr in node.attributes.keys) {
            if (attr != "href" && !attr.endsWith(":href")) continue
            val value = node.attributes[attr] ?: continue
            if (!value.startsWith("#")) continue
            val id = value.substring(startIndex = 1)
            state.referencesById.getOrPut(id) { mutableListOf() }
                .add(NodeRef(node = node, parentNode = parentNode))
        }
    }

    /**
     * Recursively collect references from a node and all its descendants.
     * Used for non-rendering elements that are skipped by the main visitor.
     */
    private fun collectAllNestedReferences(node: XastElement, state: PluginState) {
        collectReferences(node = node, state = state)
        for (child in node.children) {
            if (child is XastElement) {
                collectAllNestedReferences(node = child, state = state)
            }
        }
    }

    private fun collectReferences(node: XastElement, state: PluginState) {
        for ((name, value) in node.attributes) {
            val ids = findReferences(name, value)
            state.allReferences.addAll(ids)
        }
    }

    private fun canRemoveNonRenderingNode(
        node: XastElement,
        allReferences: Set<String>,
    ): Boolean {
        val nodeId = node.attributes["id"]
        if (nodeId != null && nodeId in allReferences) return false
        for (child in node.children) {
            if (child is XastElement && !canRemoveNonRenderingNode(
                    node = child,
                    allReferences = allReferences,
                )
            ) {
                return false
            }
        }
        return true
    }

    private fun getOpacity(node: XastElement): String? {
        // Check attribute directly
        val attrValue = node.attributes["opacity"]
        if (attrValue != null) return attrValue
        // Check inline style
        val style = node.attributes["style"] ?: return null
        val match = OPACITY_STYLE_REGEX.find(style) ?: return null
        return match.groupValues[1].trim()
    }

    private fun isScript(node: XastElement): Boolean {
        if (node.name == "script") return true
        return SCRIPT_EVENT_ATTRS.any { node.attributes.containsKey(it) }
    }

    private fun findReferences(attribute: String, value: String): List<String> {
        val results = mutableListOf<String>()
        URL_REF_REGEX.findAll(value).forEach { match ->
            results.add(match.groupValues[1])
        }
        if (attribute == "href" || attribute.endsWith(":href")) {
            if (value.startsWith("#")) {
                results.add(value.substring(startIndex = 1))
            }
        }
        return results
    }

    private val URL_REF_REGEX = """\burl\(["']?#(.+?)["']?\)""".toRegex()
    private val OPACITY_STYLE_REGEX = """(?:^|;)\s*opacity\s*:\s*([^;]+)""".toRegex()
    private val SCRIPT_EVENT_ATTRS = setOf(
        "onbegin", "onend", "onrepeat", "onload", "onabort", "onerror",
        "onresize", "onscroll", "onunload", "onzoom", "onclick",
        "onactivate", "onfocusin", "onfocusout", "onmousedown",
        "onmouseup", "onmouseover", "onmousemove", "onmouseout",
    )

    private data class NodeRef(
        val node: XastElement,
        val parentNode: XastParent?,
    )

    private class PluginState {
        val nonRenderedNodes = mutableMapOf<XastElement, XastParent>()
        val removedDefIds = mutableSetOf<String>()
        val allDefs = mutableMapOf<XastElement, XastParent>()
        val allReferences = mutableSetOf<String>()
        val referencesById = mutableMapOf<String, MutableList<NodeRef>>()
        var deoptimized = false
    }
}
