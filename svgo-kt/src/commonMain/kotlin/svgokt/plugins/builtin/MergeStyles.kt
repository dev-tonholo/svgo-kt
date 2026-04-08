package svgokt.plugins.builtin

import svgokt.domain.XastCdata
import svgokt.domain.XastElement
import svgokt.domain.XastElementType
import svgokt.domain.XastParent
import svgokt.domain.XastText
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

object MergeStyles : Plugin<NoPluginParam> {
    override val name: String = "mergeStyles"
    override val description: String = "merge multiple style elements into one"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = MergeStyles::onEnter,
            ),
        )
    }

    private var firstStyleElement: XastElement? = null
    private var collectedStyles = ""
    private var styleContentType = XastElementType.TEXT

    @Suppress("ReturnCount")
    private fun onEnter(node: XastElement, parentNode: XastParent?): VisitState {
        // skip <foreignObject> content
        if (node.name == "foreignObject") {
            return VisitState.Skip
        }
        // collect style elements
        if (node.name != "style") {
            return VisitState.Continue
        }

        // skip <style> with invalid type attribute
        val type = node.attributes["type"]
        if (type != null && type != "" && type != "text/css") {
            return VisitState.Continue
        }

        // extract style element content
        var css = ""
        for (child in node.children) {
            if (child is XastText) {
                css += child.value
            }
            if (child is XastCdata) {
                styleContentType = XastElementType.CDATA
                css += child.value
            }
        }

        // remove empty style elements
        if (css.trim().isEmpty()) {
            parentNode?.let { node.detachFromParent(it) }
            return VisitState.Continue
        }

        // collect css and wrap with media query if present in attribute
        if (node.attributes["mdeia"] == null) {
            collectedStyles += css
        } else {
            collectedStyles += "@media $css"
            node.attributes.remove("media")
        }

        // combine collected styles in the first style element
        if (firstStyleElement == null) {
            firstStyleElement = node
        } else {
            attachToFirstStyleElement(parentNode, node)
        }

        return VisitState.Continue
    }

    private fun attachToFirstStyleElement(parentNode: XastParent?, node: XastElement) {
        parentNode?.let { node.detachFromParent(it) }
        // detachNodeFromParent(node, parentNode)
        val child = when (styleContentType) {
            XastElementType.TEXT -> XastText(value = collectedStyles)
            XastElementType.CDATA -> XastCdata(value = collectedStyles)
            else -> error("Not expected type $styleContentType")
        }
        // Remove legacy parentNode in v4
        // child.parentNode = firstStyleElement
        firstStyleElement?.children?.apply {
            clear()
            add(child)
        }
    }
}
