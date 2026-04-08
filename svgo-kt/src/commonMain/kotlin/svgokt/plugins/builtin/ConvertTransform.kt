package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.transform.TransformItem
import svgokt.transform.parseTransform
import svgokt.transform.stringifyTransform
import svgokt.transform.transformsMultiply

private const val DEFAULT_PRECISION = 4

/**
 * Transform attribute names that this plugin processes.
 */
private val transformAttributes = listOf(
    "transform",
    "gradientTransform",
    "patternTransform",
)

/**
 * Collapses multiple transformations and optimizes them by:
 * 1. Removing identity transforms (translate(0,0), scale(1,1), rotate(0), etc.)
 * 2. Collapsing multiple transforms into a single matrix when beneficial
 * 3. Stringifying the result with configurable precision
 */
object ConvertTransform : Plugin<NoPluginParam> {
    override val name: String = "convertTransform"
    override val description: String = "collapses multiple transformations and optimizes it"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = { _, _, _ ->
        Visitor(
            element = VisitorNode(
                onEnter = ConvertTransform::onEnter,
            ),
        )
    }

    private fun onEnter(
        node: XastElement,
        @Suppress("UNUSED_PARAMETER") parentNode: XastParent?,
    ): VisitState {
        for (attrName in transformAttributes) {
            val transformValue = node.attributes[attrName] ?: continue
            convertTransform(node = node, attrName = attrName, transformValue = transformValue)
        }
        return VisitState.Continue
    }
}

private fun convertTransform(
    node: XastElement,
    attrName: String,
    transformValue: String,
) {
    val parsed = parseTransform(transformString = transformValue)
    if (parsed.isEmpty()) return

    val transforms = parsed.toMutableList()

    // Remove identity transforms
    transforms.removeAll { isIdentityTransform(transform = it) }

    // If all transforms were identity, remove the attribute entirely
    if (transforms.isEmpty()) {
        node.attributes.remove(attrName)
        return
    }

    // Collapse multiple transforms into a single matrix, or re-stringify a single one
    val optimized = if (transforms.size > 1) {
        listOf(transformsMultiply(transforms = transforms))
    } else {
        transforms
    }
    node.attributes[attrName] = stringifyTransform(
        transforms = optimized,
        precision = DEFAULT_PRECISION,
    )
}

/**
 * Checks whether a transform is an identity (no-op) transform.
 *
 * Identity transforms:
 * - translate(0) or translate(0, 0)
 * - scale(1) or scale(1, 1)
 * - rotate(0) or rotate(0, cx, cy)
 * - skewX(0)
 * - skewY(0)
 * - matrix(1, 0, 0, 1, 0, 0)
 */
private fun isIdentityTransform(transform: TransformItem): Boolean {
    val data = transform.data
    return when (transform.name) {
        "translate" -> data.all { it == 0.0 }
        "scale" -> isIdentityScale(data = data)
        "rotate", "skewX", "skewY" -> data[0] == 0.0
        "matrix" -> isIdentityMatrix(data = data)
        else -> false
    }
}

private fun isIdentityScale(data: DoubleArray): Boolean = when (data.size) {
    1 -> data[0] == 1.0
    else -> data[0] == 1.0 && data[1] == 1.0
}

@Suppress("MagicNumber")
private fun isIdentityMatrix(data: DoubleArray): Boolean =
    data.size >= 6 &&
        data[0] == 1.0 &&
        data[1] == 0.0 &&
        data[2] == 0.0 &&
        data[3] == 1.0 &&
        data[4] == 0.0 &&
        data[5] == 0.0
