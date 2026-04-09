package svgokt.plugins.xast

import svgokt.domain.XastCdata
import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.XastRoot
import svgokt.domain.XastText
import svgokt.domain.css.Specificity
import svgokt.domain.css.Stylesheet
import svgokt.domain.css.StylesheetRule
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

private const val SPECIFICITY_COMPONENTS = 4

fun collectStylesheet(root: XastRoot): Stylesheet {
    val rules = mutableListOf<StylesheetRule>()
    val parents = mutableMapOf<XastElement, XastParent>()

    root.visit(
        visitor = Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    parentNode?.let { parent -> parents += node to parent }

                    if (node.name != "style") {
                        return@VisitorNode VisitState.Continue
                    }

                    val type = node.attributes["type"]
                    if (type == null || type == "" || type == "text/css") {
                        val media = node.attributes["media"]
                        val dynamic = media != null && media != "all"

                        for (child in node.children) {
                            val value = when (child) {
                                is XastText -> child.value
                                is XastCdata -> child.value
                                else -> null
                            }
                            value?.let { rules.addAll(parseStylesheet(css = value, dynamic)) }
                        }
                    }

                    VisitState.Continue
                }
            )
        )
    )

    // sort by selectors specificity
    rules.sortedWith { a, b -> compareSpecificity(a.specificity, b.specificity) }
    return Stylesheet(rules, parents)
}

/**
 * Compares selector specificities.
 * Derived from https://github.com/keeganstreet/specificity/blob/8757133ddd2ed0163f120900047ff0f92760b536/specificity.js#L207
 */
@Suppress("ReturnCount")
private fun compareSpecificity(a: Specificity, b: Specificity): Int {
    for (i in 0 until SPECIFICITY_COMPONENTS) {
        if (a[i] < b[i]) {
            return -1
        } else if (a[i] > b[i]) {
            return 1
        }
    }

    return 0
}

@Suppress("UnusedParameter")
private fun parseStylesheet(css: String, dynamic: Boolean): List<StylesheetRule> {
    // Stub: CSS-to-AST parsing not yet implemented.
    return listOf()
}
