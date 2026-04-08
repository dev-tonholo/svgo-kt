package svgokt.plugins.builtin

import svgokt.Tools
import svgokt.domain.XastRoot
import svgokt.domain.plugins.PluginParams
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.Collections
import svgokt.plugins.xast.collectStylesheet
import svgokt.style.computeStyle

/**
 * Apply transformation(s) to the Path data.
 */
@Suppress("UnusedParameter")
fun applyTransforms(root: XastRoot, params: ApplyTransformsParams): Visitor? {
    val stylesheet = collectStylesheet(root)
    return Visitor(
        element = VisitorNode(
            onEnter = { node, _ ->
                if (node.attributes["d"] == null) {
                    return@VisitorNode VisitState.Continue
                }

                // stroke and stroke-width can be redefined with <use>
                if (node.attributes["id"] != null) {
                    return@VisitorNode VisitState.Continue
                }

                // if there are no 'stroke' attr and references to other objects such as
                // gradients or clip-path which are also subjects to transform.
                val transform = node.attributes["transform"]
                val isUrlReference = node.attributes.any { (name, value) ->
                    Collections.referencesProps.contains(name) && Tools.includesUrlReference(value)
                }
                if (
                    transform.isNullOrEmpty() ||
                    // styles are not considered when applying transform
                    // can be fixed properly with new style engine
                    node.attributes["style"] != null ||
                    isUrlReference
                ) {
                    return@VisitorNode VisitState.Continue
                }

                computeStyle(stylesheet, node)

                VisitState.Continue
            }
        )
    )
}

data class ApplyTransformsParams(
    val transformPrecision: Int,
    val applyTransformsStroked: Boolean,
) : PluginParams,
    Map<String, Any> by mapOf(
        "transformPrecision" to transformPrecision,
        "applyTransformsStroked" to applyTransformsStroked,
    )
