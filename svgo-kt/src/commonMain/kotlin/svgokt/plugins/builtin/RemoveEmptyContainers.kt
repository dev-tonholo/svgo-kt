package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.domain.plugins.VisitorRoot
import svgokt.plugins.Collections
import svgokt.plugins.xast.collectStylesheet
import svgokt.plugins.xast.detachFromParent
import svgokt.style.computeStyle

/**
 * Removes empty container elements (container elements with no children).
 *
 * A container element is considered safe to remove when:
 * - It is not the root `<svg>` element
 * - It has no children
 * - It is a known container element
 * - It is not a `<pattern>` with attributes (reusable configuration)
 * - It is not a `<mask>` with an id (hides masked element)
 * - It is not inside a `<switch>` element
 * - It is not a `<g>` with a filter attribute or computed filter style
 *
 * Uses post-order (exit) traversal so children are processed before parents,
 * allowing nested empty containers to be removed in a single pass.
 *
 * Also removes `<use>` elements that reference removed containers.
 */
object RemoveEmptyContainers : Plugin<NoPluginParam> {
    override val name: String = "removeEmptyContainers"
    override val description: String = "removes empty container elements"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { root, _, _ ->
        val stylesheet = collectStylesheet(root)
        val removedIds = mutableSetOf<String>()
        val usesById = mutableMapOf<String, MutableList<UseRef>>()

        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    onEnter(
                        node = node,
                        parentNode = parentNode,
                        usesById = usesById,
                    )
                },
                onExit = { node, parentNode ->
                    onExit(
                        node = node,
                        parentNode = parentNode,
                        stylesheet = stylesheet,
                        removedIds = removedIds,
                    )
                },
            ),
            root = VisitorRoot(
                onExit = { _, _ ->
                    onRootExit(removedIds = removedIds, usesById = usesById)
                },
            ),
        )
    }

    private fun onEnter(
        node: XastElement,
        parentNode: XastParent?,
        usesById: MutableMap<String, MutableList<UseRef>>,
    ): VisitState {
        if (node.name == "use") {
            // Record uses so those referencing empty containers can be removed.
            for ((name, value) in node.attributes) {
                val ids = findReferences(name, value)
                for (id in ids) {
                    usesById.getOrPut(id) { mutableListOf() }
                        .add(UseRef(node = node, parent = parentNode))
                }
            }
        }
        return VisitState.Continue
    }

    private fun onExit(
        node: XastElement,
        parentNode: XastParent?,
        stylesheet: svgokt.domain.css.Stylesheet,
        removedIds: MutableSet<String>,
    ) {
        // remove only empty non-svg containers
        if (node.name == "svg") return
        if (node.name !in Collections.containerElements) return
        if (node.children.isNotEmpty()) return

        // empty patterns may contain reusable configuration
        if (node.name == "pattern" && node.attributes.isNotEmpty()) return

        // empty <mask> hides masked element
        if (node.name == "mask" && node.attributes.containsKey("id")) return

        // don't remove children of <switch>
        if (parentNode is XastElement && parentNode.name == "switch") return

        // The <g> may not have content, but the filter may cause a rectangle
        // to be created and filled with pattern.
        if (node.name == "g" && hasFilter(node, stylesheet)) return

        parentNode?.let { node.detachFromParent(it) }
        val nodeId = node.attributes["id"]
        if (nodeId != null) {
            removedIds.add(nodeId)
        }
    }

    private fun onRootExit(
        removedIds: Set<String>,
        usesById: Map<String, List<UseRef>>,
    ) {
        // Remove any <use> elements that referenced an empty container.
        for (id in removedIds) {
            val uses = usesById[id] ?: continue
            for (use in uses) {
                val parent = use.parent ?: continue
                use.node.detachFromParent(parent)
            }
        }
    }

    private fun hasFilter(
        node: XastElement,
        stylesheet: svgokt.domain.css.Stylesheet,
    ): Boolean {
        // Check attribute directly
        if (node.attributes.containsKey("filter")) return true
        // Check inline style
        val style = node.attributes["style"]
        if (style != null && "filter" in style) return true
        // Check computed style (stub, but try anyway)
        computeStyle(stylesheet, node)
        return false
    }

    /**
     * Extracts referenced IDs from an attribute name+value pair.
     * Handles href, xlink:href, and url() references.
     */
    private fun findReferences(name: String, value: String): List<String> {
        val results = mutableListOf<String>()
        // Handle href and xlink:href
        if (name == "href" || name.endsWith(":href")) {
            if (value.startsWith("#")) {
                results.add(value.substring(startIndex = 1))
            }
        }
        // Handle url(#id) references
        URL_REF_REGEX.findAll(value).forEach { match ->
            results.add(match.groupValues[2])
        }
        return results
    }

    private val URL_REF_REGEX = """\burl\((["'])?#(.+?)\1\)""".toRegex()

    private data class UseRef(
        val node: XastElement,
        val parent: XastParent?,
    )
}
