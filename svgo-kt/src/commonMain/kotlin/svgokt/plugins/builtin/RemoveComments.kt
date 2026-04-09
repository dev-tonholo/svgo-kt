package svgokt.plugins.builtin

import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.detachFromParent

private const val PARAM_PRESERVE_PATTERNS = "preservePatterns"

/**
 * Default preserve patterns: comments starting with "!" are kept
 * (commonly used for copyright/license information).
 */
private val DEFAULT_PRESERVE_PATTERNS = listOf(Regex("^!"))

object RemoveComments : Plugin<PluginParams> {
    override val name: String = "removeComments"
    override val description: String = "removes comments"
    override val params: PluginParams = RemoveCommentsParams()
    override val fn: PluginFn = { _, params, _ ->
        val preservePatterns = resolvePreservePatterns(params)

        Visitor(
            comment = VisitorNode(
                onEnter = { node, parentNode ->
                    if (preservePatterns != null) {
                        val matches = preservePatterns.any { pattern ->
                            pattern.containsMatchIn(node.value)
                        }
                        if (matches) {
                            return@VisitorNode VisitState.Continue
                        }
                    }
                    parentNode?.let { node.detachFromParent(it) }
                    VisitState.Continue
                },
            ),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolvePreservePatterns(params: PluginParams): List<Regex>? {
        val raw = params[PARAM_PRESERVE_PATTERNS]
        return when (raw) {
            is Boolean -> if (raw) DEFAULT_PRESERVE_PATTERNS else null
            false -> null
            is List<*> -> (raw as List<Any>).map { pattern ->
                when (pattern) {
                    is Regex -> pattern
                    is String -> Regex(pattern)
                    else -> Regex(pattern.toString())
                }
            }
            null -> DEFAULT_PRESERVE_PATTERNS
            else -> DEFAULT_PRESERVE_PATTERNS
        }
    }
}

private class RemoveCommentsParams : PluginParams, Map<String, Any> by emptyMap()
