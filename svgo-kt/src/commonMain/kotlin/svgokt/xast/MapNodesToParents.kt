package svgokt.xast

import svgokt.domain.XastChild
import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.XastRoot

fun mapNodesToParents(root: XastRoot): Map<XastChild, XastParent> {
    val parents = mutableMapOf<XastChild, XastParent>()
    fun walkChildren(parent: XastParent) {
        for (child in parent.children) {
            parents[child] = parent
            if (child is XastElement) {
                walkChildren(child)
            }
        }
    }
    walkChildren(root)
    return parents
}
