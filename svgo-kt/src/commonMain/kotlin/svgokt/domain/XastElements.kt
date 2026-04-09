package svgokt.domain

import kotlin.jvm.JvmInline

enum class XastElementType {
    DOCTYPE,
    INSTRUCTION,
    COMMENT,
    CDATA,
    TEXT,
    ELEMENT,
    ROOT,
}

sealed interface XastNode {
    val type: XastElementType
}

sealed interface XastParent : XastNode {
    val children: MutableList<XastChild>
}

data class XastRoot(
    override val children: MutableList<XastChild>,
    override val type: XastElementType = XastElementType.ROOT,
) : XastParent

sealed interface XastChild : XastNode {
//    val parentNode: XastParent
}

data class XastDoctype(
    // override val parentNode: XastParent,
    val name: String,
    val data: XastDoctypeData,
    override val type: XastElementType = XastElementType.DOCTYPE,
) : XastChild {
    @JvmInline
    value class XastDoctypeData(val doctype: String)
}

data class XastInstruction(
    // override val parentNode: XastParent,
    val name: String,
    var value: String,
    override val type: XastElementType = XastElementType.INSTRUCTION,
) : XastChild

data class XastComment(
    // override val parentNode: XastParent,
    val value: String,
    override val type: XastElementType = XastElementType.COMMENT,
) : XastChild

data class XastCdata(
    // override val parentNode: XastParent,
    val value: String,
    override val type: XastElementType = XastElementType.CDATA,
) : XastChild

data class XastText(
    // override val parentNode: XastParent,
    val value: String,
    override val type: XastElementType = XastElementType.TEXT,
) : XastChild

data class XastElement(
    // override val parentNode: XastParent,
    val name: String,
    val attributes: MutableMap<String, String>,
    override val children: MutableList<XastChild>,
    override val type: XastElementType = XastElementType.ELEMENT,
) : XastChild, XastParent {
    companion object {
        /**
         * Sentinel value representing a valueless (boolean) attribute.
         *
         * In HTML/SVG, attributes like `data-icon` have no value. In JS this
         * is represented as `undefined`. Since Kotlin maps cannot hold
         * `undefined`, this sentinel is stored as the value and the
         * stringifier omits the `="..."` portion when it encounters it.
         */
        const val VALUELESS_ATTRIBUTE: String = "\u0000__valueless__"
    }
}
