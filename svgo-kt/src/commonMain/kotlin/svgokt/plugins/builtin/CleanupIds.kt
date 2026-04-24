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
 * Collects all referenced IDs from url(#id), href="#id", xlink:href="#id"
 * and removes unreferenced IDs from elements. Skips processing when
 * style or script elements are present (unless forced).
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

    override val fn: PluginFn = { _, params, _ ->
        val resolvedParams = resolveParams(params)
        val nodeById = mutableMapOf<String, XastElement>()
        val referencesById = mutableMapOf<String, MutableList<ReferenceEntry>>()
        var deoptimized = false

        Visitor(
            element = VisitorNode(
                onEnter = { node, _ ->
                    onElementEnter(
                        node = node,
                        resolvedParams = resolvedParams,
                        nodeById = nodeById,
                        referencesById = referencesById,
                        onDeoptimized = { deoptimized = true },
                    )
                },
            ),
            root = VisitorRoot(
                onExit = { _, _ ->
                    onRootExit(
                        resolvedParams = resolvedParams,
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
        resolvedParams: Params,
        nodeById: MutableMap<String, XastElement>,
        referencesById: MutableMap<String, MutableList<ReferenceEntry>>,
        onDeoptimized: () -> Unit,
    ): VisitState {
        if (!resolvedParams.force) {
            if (node.name == "style" && node.children.isNotEmpty()) {
                onDeoptimized()
                return VisitState.Continue
            }
            if (hasScripts(node)) {
                onDeoptimized()
                return VisitState.Continue
            }

            // avoid removing IDs if the whole SVG consists only of defs
            if (node.name == "svg") {
                val hasDefsOnly = node.children.all { child ->
                    child is XastElement && child.name == "defs"
                }
                if (hasDefsOnly) {
                    return VisitState.Skip
                }
            }
        }

        // Iterate over a detached snapshot (Pairs, not Map.Entry) so mutating
        // node.attributes during the loop does not throw
        // ConcurrentModificationException on Kotlin/Native and Kotlin/JS —
        // their HashMap implementations invalidate outstanding Map.Entry views
        // even after `.toList()`.
        val entries = node.attributes.entries.map { it.key to it.value }
        for ((attrName, attrValue) in entries) {
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
        resolvedParams: Params,
        deoptimized: Boolean,
        nodeById: MutableMap<String, XastElement>,
        referencesById: MutableMap<String, MutableList<ReferenceEntry>>,
    ) {
        if (deoptimized) {
            return
        }

        val isIdPreserved = { id: String ->
            resolvedParams.preserve.contains(id) ||
                resolvedParams.preservePrefixes.any { id.startsWith(it) }
        }

        var currentId: MutableList<Int>? = null

        for ((id, refs) in referencesById) {
            val node = nodeById[id] ?: continue

            if (resolvedParams.minify && !isIdPreserved(id)) {
                var currentIdString: String
                do {
                    currentId = generateId(currentId)
                    currentIdString = getIdString(currentId)
                } while (
                    isIdPreserved(currentIdString) ||
                    (
                        referencesById.containsKey(currentIdString) &&
                            nodeById[currentIdString] == null
                        )
                )

                node.attributes["id"] = currentIdString
                for ((element, attributeName) in refs) {
                    val value = element.attributes[attributeName] ?: continue
                    element.attributes[attributeName] = if (value.contains('#')) {
                        // Replace both URI-encoded and plain id forms
                        value
                            .replace("#${encodeUri(id)}", "#$currentIdString")
                            .replace("#$id", "#$currentIdString")
                    } else {
                        // Replace id in begin attribute
                        value.replace("$id.", "$currentIdString.")
                    }
                }
            }

            nodeById.remove(id)
        }

        if (resolvedParams.remove) {
            for ((id, node) in nodeById) {
                if (!isIdPreserved(id)) {
                    node.attributes.remove("id")
                }
            }
        }
    }

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

            // Decode URI-encoded references (e.g. %E4%BA%BA%E5%8F%A3 -> actual chars)
            return results.map { decodeUri(it) }
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

        /**
         * Encodes a string like JavaScript's encodeURI.
         * Percent-encodes all characters except: A-Z a-z 0-9 - _ . ~ ! * ' ( ) ; / ? : @ & = + $ , #
         */
        private fun encodeUri(input: String): String {
            val builder = StringBuilder()
            for (byte in input.encodeToByteArray()) {
                val c = byte.toInt().toChar()
                if (c in URI_UNRESERVED) {
                    builder.append(c)
                } else {
                    val unsigned = byte.toInt() and 0xFF
                    builder.append('%')
                    builder.append(HEX_DIGITS[unsigned shr 4])
                    builder.append(HEX_DIGITS[unsigned and 0x0F])
                }
            }
            return builder.toString()
        }

        private val HEX_DIGITS = "0123456789ABCDEF".toCharArray()

        /**
         * Characters that JS encodeURI does NOT encode.
         */
        private val URI_UNRESERVED: Set<Char> = buildSet {
            addAll('A'..'Z')
            addAll('a'..'z')
            addAll('0'..'9')
            addAll("-_.~!*'();/?:@&=+\$,#".toList())
        }

        /**
         * Decodes a percent-encoded URI string, like JavaScript's decodeURI.
         */
        private fun decodeUri(input: String): String {
            val bytes = mutableListOf<Byte>()
            var i = 0
            while (i < input.length) {
                if (input[i] == '%' && i + 2 < input.length) {
                    val hex = input.substring(startIndex = i + 1, endIndex = i + 3)
                    val value = hex.toIntOrNull(radix = 16)
                    if (value != null) {
                        bytes.add(value.toByte())
                        i += 3
                        continue
                    }
                }
                bytes.addAll(input[i].toString().encodeToByteArray().toList())
                i++
            }
            return bytes.toByteArray().decodeToString()
        }

        /**
         * Resolves the effective [Params] from the [PluginParams] passed to [fn].
         * The integration test harness merges fixture-specific params on top of
         * the plugin's default params, so we need to read them from the map.
         */
        @Suppress("UNCHECKED_CAST")
        private fun resolveParams(pluginParams: PluginParams): Params {
            if (pluginParams is Params) return pluginParams
            return Params(
                remove = pluginParams["remove"] as? Boolean ?: true,
                minify = pluginParams["minify"] as? Boolean ?: true,
                preserve = when (val raw = pluginParams["preserve"]) {
                    is Set<*> -> raw as Set<String>
                    is List<*> -> (raw as List<String>).toSet()
                    is String -> setOf(raw)
                    else -> emptySet()
                },
                preservePrefixes = when (val raw = pluginParams["preservePrefixes"]) {
                    is List<*> -> raw as List<String>
                    is String -> listOf(raw)
                    else -> emptyList()
                },
                force = pluginParams["force"] as? Boolean ?: false,
            )
        }
    }
}
