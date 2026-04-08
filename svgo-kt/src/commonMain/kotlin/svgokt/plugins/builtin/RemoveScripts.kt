package svgokt.plugins.builtin

import svgokt.domain.XastText
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

/**
 * All SVG event handler attributes.
 */
private val eventAttrs = setOf(
    // animationEvent
    "onbegin", "onend", "onrepeat", "onload",
    // graphicalEvent
    "onactivate", "onclick", "onfocusin", "onfocusout",
    "onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup",
    // documentEvent
    "onabort", "onerror", "onresize", "onscroll", "onunload", "onzoom",
    // documentElementEvent
    "oncopy", "oncut", "onpaste",
    // globalEvent
    "oncancel", "oncanplay", "oncanplaythrough", "onchange", "onclose",
    "oncuechange", "ondblclick", "ondrag", "ondragend", "ondragenter",
    "ondragleave", "ondragover", "ondragstart", "ondrop", "ondurationchange",
    "onemptied", "onended", "onfocus", "oninput", "oninvalid",
    "onkeydown", "onkeypress", "onkeyup", "onloadeddata", "onloadedmetadata",
    "onloadstart", "onmouseenter", "onmouseleave", "onmousewheel",
    "onpause", "onplay", "onplaying", "onprogress", "onratechange",
    "onreset", "onscroll", "onseeked", "onseeking", "onselect", "onshow",
    "onstalled", "onsubmit", "onsuspend", "ontimeupdate", "ontoggle",
    "onvolumechange", "onwaiting",
)

/**
 * Remove scripts: `<script>` elements, event handler attributes,
 * and `javascript:` href values in `<a>` elements.
 *
 * @see <a href="https://www.w3.org/TR/SVG11/script.html">SVG Script</a>
 */
object RemoveScripts : Plugin<NoPluginParam> {
    override val name: String = "removeScripts"
    override val description: String = "removes scripts"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    if (node.name == "script") {
                        parentNode?.let { node.detachFromParent(it) }
                        return@VisitorNode VisitState.Continue
                    }

                    for (attr in eventAttrs) {
                        node.attributes.remove(attr)
                    }
                    VisitState.Continue
                },
                onExit = { node, parentNode ->
                    if (node.name != "a" || parentNode == null) {
                        return@VisitorNode
                    }

                    val hrefKeys = node.attributes.keys.filter { key ->
                        key == "href" || key.endsWith(":href")
                    }
                    for (attr in hrefKeys) {
                        val value = node.attributes[attr] ?: continue
                        if (value.trimStart().startsWith("javascript:")) {
                            val index = parentNode.children.indexOf(node)
                            if (index >= 0) {
                                val usefulChildren = node.children.filter { child ->
                                    child !is XastText
                                }
                                parentNode.children.removeAt(index)
                                parentNode.children.addAll(index, usefulChildren)
                            }
                        }
                    }
                },
            ),
        )
    }
}
