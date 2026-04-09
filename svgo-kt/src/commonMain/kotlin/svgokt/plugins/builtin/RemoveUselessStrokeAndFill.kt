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
import svgokt.plugins.Collections
import svgokt.plugins.xast.detachFromParent

/**
 * Removes useless stroke and fill attributes.
 *
 * When stroke is "none" or stroke-related values indicate no visible stroke,
 * all stroke-* attributes are removed. Similarly for fill.
 *
 * Style and script elements deoptimize this plugin (makes it a no-op),
 * matching the JS reference behavior.
 */
object RemoveUselessStrokeAndFill : Plugin<RemoveUselessStrokeAndFill.Params> {
    data class Params(
        val stroke: Boolean = true,
        val fill: Boolean = true,
        val removeNone: Boolean = false,
    ) : PluginParams,
        Map<String, Any> by mapOf(
            "stroke" to stroke,
            "fill" to fill,
            "removeNone" to removeNone,
        )

    override val name: String = "removeUselessStrokeAndFill"
    override val description: String = "removes useless stroke and fill attributes"
    override val params: Params = Params()
    override val fn: PluginFn = { root, pluginParams, _ ->
        val resolvedParams = resolveParams(pluginParams)

        // Style and script elements deoptimize this plugin
        if (hasStyleOrScript(root)) {
            null
        } else {
            Visitor(
                element = VisitorNode(
                    onEnter = { node, parentNode ->
                        onEnter(node, parentNode, resolvedParams)
                    },
                ),
            )
        }
    }

    private fun hasStyleOrScript(root: XastRoot): Boolean {
        fun walk(children: List<XastChild>): Boolean {
            for (child in children) {
                if (child is XastElement) {
                    if (child.name == "style" || hasScripts(child)) return true
                    if (walk(child.children)) return true
                }
            }
            return false
        }
        return walk(root.children)
    }

    private fun hasScripts(node: XastElement): Boolean {
        if (node.name == "script") return true
        return SCRIPT_EVENT_ATTRS.any { node.attributes.containsKey(it) }
    }

    private fun onEnter(
        node: XastElement,
        parentNode: XastParent?,
        params: Params,
    ): VisitState {
        // Elements with id may be referenced; skip the whole subtree
        if (node.attributes.containsKey("id")) return VisitState.Skip
        if (node.name !in Collections.shapeElements) return VisitState.Continue

        if (params.stroke) {
            removeUselessStroke(node, parentNode)
        }
        if (params.fill) {
            removeUselessFill(node, parentNode)
        }
        if (params.removeNone) {
            removeNoneElement(node, parentNode)
        }
        return VisitState.Continue
    }

    /**
     * Resolves the effective value of an attribute by checking the node's own
     * attributes, then walking up the parent chain for inherited values.
     * Returns null if not found anywhere.
     */
    private fun resolveInheritedAttribute(
        node: XastElement,
        parentNode: XastParent?,
        attrName: String,
    ): String? {
        node.attributes[attrName]?.let { return it }
        var current = parentNode
        while (current is XastElement) {
            current.attributes[attrName]?.let { return it }
            // Cannot walk further up since parent references are not stored.
            break
        }
        return null
    }

    private fun removeUselessStroke(node: XastElement, parentNode: XastParent?) {
        val stroke = resolveInheritedAttribute(node, parentNode, "stroke")
        val strokeOpacity = resolveInheritedAttribute(node, parentNode, "stroke-opacity")
        val strokeWidth = resolveInheritedAttribute(node, parentNode, "stroke-width")
        val markerEnd = resolveInheritedAttribute(node, parentNode, "marker-end")

        val isStrokeNone = stroke == "none"
        val isStrokeAbsent = stroke == null
        val isOpacityZero = strokeOpacity == "0"
        val isWidthZero = strokeWidth == "0"

        // Only proceed if stroke is absent/none or opacity/width is zero
        if (!isStrokeNone && !isStrokeAbsent && !isOpacityZero && !isWidthZero) return

        // stroke-width may affect the size of marker-end.
        // Marker is not visible when stroke-width is 0, so we can still remove.
        // But if stroke-width is NOT 0 and marker-end exists, preserve stroke attrs.
        if (!isWidthZero && markerEnd != null) return

        node.attributes.keys.removeAll { it.startsWith("stroke") }

        // Set explicit none to prevent inheriting from parent
        val parentElement = parentNode as? XastElement
        val parentStroke = parentElement?.attributes?.get("stroke")
        if (parentStroke != null && parentStroke != "none") {
            node.attributes["stroke"] = "none"
        }
    }

    private fun removeUselessFill(node: XastElement, parentNode: XastParent?) {
        val fill = resolveInheritedAttribute(node, parentNode, "fill")
        val fillOpacity = resolveInheritedAttribute(node, parentNode, "fill-opacity")
        val isFillNone = fill == "none"
        val isOpacityZero = fillOpacity == "0"

        if (!isFillNone && !isOpacityZero) return

        node.attributes.keys.removeAll { it.startsWith("fill-") }

        // Set explicit fill="none" only if the resolved fill is not already "none".
        // If fill is inherited as "none" from a parent, no explicit attribute is needed.
        if (fill == null || fill != "none") {
            node.attributes["fill"] = "none"
        }
    }

    private fun removeNoneElement(node: XastElement, parentNode: XastParent?) {
        val stroke = resolveInheritedAttribute(node, parentNode, "stroke")
        val resolvedFill = resolveInheritedAttribute(node, parentNode, "fill")
        val nodeFill = node.attributes["fill"]

        val isStrokeNoneOrAbsent = stroke == null || stroke == "none"
        // Match JS: computed fill is "none" OR node's own fill attribute is "none"
        val isFillNone = resolvedFill == "none" || nodeFill == "none"

        if (isStrokeNoneOrAbsent && isFillNone && parentNode != null) {
            node.detachFromParent(parentNode)
        }
    }

    private fun resolveParams(pluginParams: PluginParams?): Params {
        if (pluginParams is Params) return pluginParams
        if (pluginParams == null) return Params()
        val map = pluginParams as? Map<*, *> ?: return Params()
        return Params(
            stroke = (map["stroke"] as? Boolean) ?: Params().stroke,
            fill = (map["fill"] as? Boolean) ?: Params().fill,
            removeNone = (map["removeNone"] as? Boolean) ?: Params().removeNone,
        )
    }

    private val SCRIPT_EVENT_ATTRS = setOf(
        "onbegin", "onend", "onrepeat", "onload", "onerror",
        "onabort", "onresize", "onscroll", "onunload", "onzoom",
        "oncopy", "oncut", "onpaste", "oncancel", "oncanplay",
        "oncanplaythrough", "onchange", "onclick", "onclose",
        "oncuechange", "ondblclick", "ondrag", "ondragend",
        "ondragenter", "ondragleave", "ondragover", "ondragstart",
        "ondrop", "ondurationchange", "onemptied", "onended",
        "onfocus", "oninput", "oninvalid", "onkeydown",
        "onkeypress", "onkeyup", "onloadeddata", "onloadedmetadata",
        "onloadstart", "onmousedown", "onmouseenter", "onmouseleave",
        "onmousemove", "onmouseout", "onmouseover", "onmouseup",
        "onmousewheel", "onpause", "onplay", "onplaying",
        "onprogress", "onratechange", "onreset", "onseeked",
        "onseeking", "onselect", "onshow", "onstalled", "onsubmit",
        "onsuspend", "ontimeupdate", "ontoggle", "onvolumechange",
        "onwaiting", "onactivate", "onfocusin", "onfocusout",
    )
}
