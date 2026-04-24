package svgokt.plugins.builtin

import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastElementType
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

/**
 * Finds duplicate `<path>` elements with the same `d`, `fill`, and `stroke`
 * attributes and replaces duplicates with `<use>` elements referencing a
 * single `<path>` placed inside `<defs>`.
 */
object ReusePaths : Plugin<NoPluginParam> {
    override val name: String = "reusePaths"
    override val description: String =
        "finds duplicate <path> elements and replaces them with <use> references"
    override val params: NoPluginParam = NoPluginParam

    private data class PathEntry(
        val node: XastElement,
        val parent: XastParent,
    )

    override val fn: PluginFn = { _, _, _ ->
        val paths = mutableMapOf<String, MutableList<PathEntry>>()
        var svgDefs: XastElement? = null
        val hrefs = mutableSetOf<String>()

        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    // Collect path elements keyed by d+fill+stroke
                    if (node.name == "path" &&
                        node.attributes["d"] != null &&
                        parentNode != null
                    ) {
                        val key = buildString {
                            append(node.attributes["d"])
                            append(";s:")
                            append(node.attributes["stroke"].orEmpty())
                            append(";f:")
                            append(node.attributes["fill"].orEmpty())
                        }
                        paths.getOrPut(key) { mutableListOf() }
                            .add(PathEntry(node = node, parent = parentNode))
                    }

                    // Find existing defs element
                    if (svgDefs == null &&
                        node.name == "defs" &&
                        parentNode?.type == XastElementType.ELEMENT &&
                        parentNode is XastElement &&
                        parentNode.name == "svg"
                    ) {
                        svgDefs = node
                    }

                    // Track existing use href references
                    if (node.name == "use") {
                        for (hrefAttr in listOf("href", "xlink:href")) {
                            val href = node.attributes[hrefAttr]
                            if (href != null && href.startsWith("#") && href.length > 1) {
                                hrefs.add(href.substring(startIndex = 1))
                            }
                        }
                    }

                    VisitState.Continue
                },
                onExit = { node, parentNode ->
                    if (node.name != "svg" || parentNode?.type != XastElementType.ROOT) {
                        return@VisitorNode
                    }

                    val defsTag = svgDefs ?: XastElement(
                        name = "defs",
                        attributes = mutableMapOf(),
                        children = mutableListOf(),
                    )

                    var index = 0
                    for (list in paths.values) {
                        if (list.size <= 1) continue

                        val first = list.first()
                        val reusablePath = XastElement(
                            name = "path",
                            attributes = mutableMapOf(),
                            children = mutableListOf(),
                        )

                        for (attr in listOf("fill", "stroke", "d")) {
                            val value = first.node.attributes[attr]
                            if (value != null) {
                                reusablePath.attributes[attr] = value
                            }
                        }

                        // Reuse original ID when it isn't referenced elsewhere
                        val originalId = first.node.attributes["id"]
                        if (originalId == null || hrefs.contains(originalId)) {
                            reusablePath.attributes["id"] = "reuse-$index"
                            index++
                        } else {
                            reusablePath.attributes["id"] = originalId
                            first.node.attributes.remove("id")
                        }

                        defsTag.children.add(reusablePath as XastChild)
                        val reusableId = checkNotNull(reusablePath.attributes["id"])

                        // Convert paths to <use>
                        for (entry in list) {
                            val pathNode = entry.node
                            val pathParent = entry.parent

                            pathNode.attributes.remove("d")
                            pathNode.attributes.remove("stroke")
                            pathNode.attributes.remove("fill")

                            // If pathNode is in defs and has no meaningful attrs, detach it
                            if (isChildOf(pathNode, defsTag) &&
                                pathNode.children.isEmpty()
                            ) {
                                if (pathNode.attributes.isEmpty()) {
                                    removeByIdentity(pathNode, defsTag)
                                    continue
                                }
                                if (pathNode.attributes.size == 1 &&
                                    pathNode.attributes["id"] != null
                                ) {
                                    val oldId = pathNode.attributes["id"]
                                    removeByIdentity(pathNode, defsTag)
                                    if (oldId != null) {
                                        updateHrefReferences(
                                            root = node,
                                            oldId = oldId,
                                            newId = reusableId,
                                        )
                                    }
                                    continue
                                }
                            }

                            // Replace path node with a use element in parent's children
                            replaceWithUseElement(
                                pathNode = pathNode,
                                parent = pathParent,
                                reusableId = reusableId,
                            )
                        }
                    }
                    if (defsTag.children.isNotEmpty()) {
                        if (node.attributes["xmlns:xlink"] == null) {
                            node.attributes["xmlns:xlink"] = "http://www.w3.org/1999/xlink"
                        }
                        if (svgDefs == null) {
                            node.children.add(index = 0, element = defsTag as XastChild)
                        }
                    }
                },
            ),
        )
    }

    /**
     * Checks if [child] is a child of [parent] using referential identity.
     */
    private fun isChildOf(child: XastElement, parent: XastParent): Boolean =
        parent.children.any { it === child }

    /**
     * Removes [child] from [parent]'s children using referential identity.
     */
    private fun removeByIdentity(child: XastElement, parent: XastParent) {
        val iterator = parent.children.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() === child) {
                iterator.remove()
                return
            }
        }
    }

    /**
     * Finds [target] in [parent]'s children using referential identity
     * and returns its index, or -1 if not found.
     */
    private fun indexByIdentity(target: XastElement, parent: XastParent): Int {
        for (i in parent.children.indices) {
            if (parent.children[i] === target) return i
        }
        return -1
    }

    /**
     * Replaces a `<path>` node with a `<use>` element in the parent's children list.
     * Copies over all remaining attributes and adds the xlink:href reference.
     */
    private fun replaceWithUseElement(
        pathNode: XastElement,
        parent: XastParent,
        reusableId: String,
    ) {
        val useElement = XastElement(
            name = "use",
            attributes = mutableMapOf(),
            children = mutableListOf(),
        )

        // Copy remaining attributes to the use element
        for ((key, value) in pathNode.attributes) {
            useElement.attributes[key] = value
        }
        useElement.attributes["xlink:href"] = "#$reusableId"

        // Replace in parent's children list using referential identity
        val idx = indexByIdentity(target = pathNode, parent = parent)
        if (idx >= 0) {
            parent.children[idx] = useElement
        }
    }

    /**
     * Updates all href/xlink:href references from [oldId] to [newId]
     * within the subtree rooted at [root].
     */
    private fun updateHrefReferences(
        root: XastElement,
        oldId: String,
        newId: String,
    ) {
        for (child in root.children) {
            if (child !is XastElement) continue
            for (hrefAttr in listOf("href", "xlink:href")) {
                if (child.attributes[hrefAttr] == "#$oldId") {
                    child.attributes[hrefAttr] = "#$newId"
                }
            }
            updateHrefReferences(root = child, oldId = oldId, newId = newId)
        }
    }
}
