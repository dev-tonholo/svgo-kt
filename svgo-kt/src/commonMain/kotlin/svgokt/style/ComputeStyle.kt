package svgokt.style

import svgokt.domain.XastElement
import svgokt.domain.css.ComputedStyles
import svgokt.domain.css.Stylesheet

fun computeOwnStyle(stylesheet: Stylesheet, node: XastElement): ComputedStyles {
    val importantStyles = mutableMapOf<String, String>()
    // collect attributes
    for ((name, value) in node.attributes) {

    }

    return ComputedStyles.DynamicStyle(inherited = false) // TODO: implement properly
}

fun computeStyle(stylesheet: Stylesheet, node: XastElement): ComputedStyles {
    return computeOwnStyle(stylesheet, node) // TODO: implement inherited style resolution
}
