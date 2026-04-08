package svgokt.style

import svgokt.domain.XastElement
import svgokt.domain.css.ComputedStyles
import svgokt.domain.css.Stylesheet

@Suppress("UnusedParameter")
fun computeOwnStyle(
    stylesheet: Stylesheet,
    node: XastElement,
): ComputedStyles {
    // Stub: attribute collection not yet implemented.
    // Will iterate over node.attributes to collect own styles.
    return ComputedStyles.DynamicStyle(inherited = false)
}

fun computeStyle(
    stylesheet: Stylesheet,
    node: XastElement,
): ComputedStyles {
    // Stub: inherited style resolution not yet implemented.
    return computeOwnStyle(stylesheet, node)
}
