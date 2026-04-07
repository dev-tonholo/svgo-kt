package svgokt.xast

import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastParent

fun querySelectorAll(
    node: XastParent,
    selector: String,
    parents: Map<XastChild, XastParent>? = null,
): List<XastElement> {
    TODO("Blocked on kss CSS selector library - see https://github.com/rafaeltonholo/kss")
}

fun querySelector(
    node: XastParent,
    selector: String,
    parents: Map<XastChild, XastParent>? = null,
): XastElement? {
    TODO("Blocked on kss CSS selector library - see https://github.com/rafaeltonholo/kss")
}

fun matches(
    node: XastElement,
    selector: String,
    parents: Map<XastChild, XastParent>? = null,
): Boolean {
    TODO("Blocked on kss CSS selector library - see https://github.com/rafaeltonholo/kss")
}
