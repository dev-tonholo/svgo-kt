package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.domain.plugins.VisitorRoot

/**
 * Removes unused IDs and optionally minifies referenced ones.
 *
 * Simplified version that:
 * - Collects all referenced IDs from url(#id), href="#id", xlink:href="#id"
 * - Removes unreferenced IDs from elements
 * - Skips processing when <style> or <script> elements are present (unless forced)
 */
class CleanupIds(
    override val params: Params = Params(),
) : Plugin<CleanupIds.Params> {

    data class Params(
        val remove: Boolean = true,
        val minify: Boolean = true,
        val preserve: Set<String> = emptySet(),
        val preservePrefixes: List<String> = emptyList(),
        val force: Boolean = false,
    ) : PluginParams,
        Map<String, Any> by mapOf(
            "remove" to remove,
            "minify" to minify,
            "preserve" to preserve,
            "preservePrefixes" to preservePrefixes,
            "force" to force,
        )

    override val name: String = "cleanupIds"
    override val description: String = "removes unused IDs and minifies used"

    override val fn: PluginFn = { _, _, _ ->
        val nodeById = mutableMapOf<String, XastElement>()
        val referencesById = mutableMapOf<String, MutableList<ReferenceEntry>>()
        var deoptimized = false

        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    onElementEnter(
                        node = node,
                        nodeById = nodeById,
                        referencesById = referencesById,
                        onDeoptimized = { deoptimized = true },
                    )
                },
            ),
            root = VisitorRoot(
                onExit = { _, _ ->
                    onRootExit(
                        deoptimized = deoptimized,
                        nodeById = nodeById,
                        referencesById = referencesById,
                    )
                },
            ),
        )
    }

    @Suppress("ReturnCount")
    private fun onElementEnter(
        node: XastElement,
        nodeById: MutableMap<String, XastElement>,
        referencesById: MutableMap<String, MutableList<ReferenceEntry>>,
        onDeoptimized: () -> Unit,
    ): VisitState {
        if (!params.force) {
            if (node.name == "style" && node.children.isNotEmpty()) {
                onDeoptimized()
                return VisitState.Continue
            }
            if (hasScripts(node)) {
                onDeoptimized()
                return VisitState.Continue
            }
        }

        for ((attrName, attrValue) in node.attributes) {
            if (attrName == "id") {
                if (nodeById.containsKey(attrValue)) {
                    // remove duplicate id
                    node.attributes.remove("id")
                } else {
                    nodeById[attrValue] = node
                }
            } else {
                val ids = findReferences(attribute = attrName, value = attrValue)
                for (id in ids) {
                    referencesById.getOrPut(id) { mutableListOf() }
                        .add(ReferenceEntry(element = node, attributeName = attrName))
                }
            }
        }

        return VisitState.Continue
    }

    @Suppress("ReturnCount", "CyclomaticComplexMethod", "NestedBlockDepth")
    private fun onRootExit(
        deoptimized: Boolean,
        nodeById: MutableMap<String, XastElement>,
        referencesById: MutableMap<String, MutableList<ReferenceEntry>>,
    ) {
        if (deoptimized) {
            return
        }

        var currentId: MutableList<Int>? = null

        for ((id, refs) in referencesById) {
            val node = nodeById[id] ?: continue

            if (params.minify && !isIdPreserved(id)) {
                var currentIdString: String
                do {
                    currentId = generateId(currentId)
                    currentIdString = getIdString(currentId)
                } while (
                    isIdPreserved(currentIdString) ||
                    (referencesById.containsKey(currentIdString) && !nodeById.containsKey(currentIdString))
                )

                node.attributes["id"] = currentIdString
                for ((element, attributeName) in refs) {
                    val value = element.attributes[attributeName] ?: continue
                    element.attributes[attributeName] = if (value.contains('#')) {
                        value.replace("#$id", "#$currentIdString")
                    } else {
                        value.replace("$id.", "$currentIdString.")
                    }
                }
            }

            nodeById.remove(id)
        }

        if (params.remove) {
            for ((id, node) in nodeById) {
                if (!isIdPreserved(id)) {
                    node.attributes.remove("id")
                }
            }
        }
    }

    private fun isIdPreserved(id: String): Boolean =
        params.preserve.contains(id) || params.preservePrefixes.any { id.startsWith(it) }

    private data class ReferenceEntry(
        val element: XastElement,
        val attributeName: String,
    )

    companion object {
        private val GENERATE_ID_CHARS = (('a'..'z') + ('A'..'Z')).toList()
        private val MAX_ID_INDEX = GENERATE_ID_CHARS.size - 1

        private val REGEX_URL_REFERENCE = Regex("\\burl\\([\"']?#(.+?)[\"']?\\)")
        private val REGEX_HREF_REFERENCE = Regex("^#(.+?)$")
        private val REGEX_BEGIN_REFERENCE = Regex("(\\w+)\\.[a-zA-Z]")

        private val SCRIPT_EVENT_ATTRS = setOf(
            "onbegin", "onend", "onrepeat", "onload", "onabort", "onerror",
            "onresize", "onscroll", "onunload", "onzoom", "onclick",
            "onactivate", "onfocusin", "onfocusout", "onmousedown",
            "onmouseup", "onmouseover", "onmousemove", "onmouseout",
        )

        internal fun hasScripts(node: XastElement): Boolean {
            if (node.name == "script") return true
            return SCRIPT_EVENT_ATTRS.any { node.attributes.containsKey(it) }
        }

        internal fun findReferences(attribute: String, value: String): List<String> {
            val results = mutableListOf<String>()

            if (attribute in REFERENCES_PROPS) {
                REGEX_URL_REFERENCE.findAll(value).forEach { match ->
                    results.add(match.groupValues[1])
                }
            }

            if (attribute == "href" || attribute.endsWith(":href")) {
                REGEX_HREF_REFERENCE.find(value)?.let { match ->
                    results.add(match.groupValues[1])
                }
            }

            if (attribute == "begin") {
                REGEX_BEGIN_REFERENCE.find(value)?.let { match ->
                    results.add(match.groupValues[1])
                }
            }

            return results
        }

        private val REFERENCES_PROPS = setOf(
            "clip-path", "color-profile", "fill", "filter",
            "marker-end", "marker-mid", "marker-start", "mask", "stroke", "style",
        )

        private fun generateId(currentId: MutableList<Int>?): MutableList<Int> {
            if (currentId == null) {
                return mutableListOf(0)
            }
            currentId[currentId.lastIndex] += 1
            for (i in currentId.lastIndex downTo 1) {
                if (currentId[i] > MAX_ID_INDEX) {
                    currentId[i] = 0
                    currentId[i - 1] += 1
                }
            }
            if (currentId[0] > MAX_ID_INDEX) {
                currentId[0] = 0
                currentId.add(index = 0, element = 0)
            }
            return currentId
        }

        private fun getIdString(arr: List<Int>): String =
            arr.joinToString(separator = "") { GENERATE_ID_CHARS[it].toString() }
    }
}
