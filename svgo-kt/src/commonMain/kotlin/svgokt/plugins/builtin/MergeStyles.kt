package svgokt.plugins.builtin

import svgokt.domain.XastCdata
import svgokt.domain.XastElement
import svgokt.domain.XastElementType
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
        var firstStyleElement: XastElement? = null
        var collectedStyles = ""
        var styleContentType = XastElementType.TEXT

        Visitor(
            element = VisitorNode(
                onEnter = onEnter@{ node, parentNode ->
                    // skip <foreignObject> content
                    if (node.name == "foreignObject") {
                        return@onEnter VisitState.Skip
                    }
                    // collect style elements
                    if (node.name != "style") {
                        return@onEnter VisitState.Continue
                    }

                    // skip <style> with invalid type attribute
                    val type = node.attributes["type"]
                    if (type != null && type != "" && type != "text/css") {
                        return@onEnter VisitState.Continue
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
                        return@onEnter VisitState.Continue
                    }

                    // collect css and wrap with media query if present in attribute
                    val media = node.attributes["media"]
                    if (media == null) {
                        collectedStyles += css
                    } else {
                        collectedStyles += "@media $media{$css}"
                        node.attributes.remove("media")
                    }

                    // combine collected styles in the first style element
                    if (firstStyleElement == null) {
                        firstStyleElement = node
                    } else {
                        parentNode?.let { node.detachFromParent(it) }
                        val child = when (styleContentType) {
                            XastElementType.CDATA -> XastCdata(value = collectedStyles)
                            else -> XastText(value = collectedStyles)
                        }
                        firstStyleElement?.children?.apply {
                            clear()
                            add(child)
                        }
                    }

                    VisitState.Continue
                },
            ),
        )
    }
}
