@file:Suppress("TooManyFunctions")

package svgokt.stringfier

import svgokt.domain.EndOfLine
import svgokt.domain.Indent
import svgokt.domain.StringifyOptions
import svgokt.domain.XastCdata
import svgokt.domain.XastComment
import svgokt.domain.XastDoctype
import svgokt.domain.XastElement
import svgokt.domain.XastInstruction
import svgokt.domain.XastParent
import svgokt.domain.XastRoot
import svgokt.domain.XastText
import svgokt.domain.builder.stringifyOptions
import svgokt.plugins.Collections

private const val DEFAULT_INDENT_SPACES = 4

private val entities = mapOf(
    '&' to "&amp;",
    '\'' to "&apos;",
    '"' to "&quot;",
    '>' to "&gt;",
    '<' to "&lt;",
)

private val defaults = stringifyOptions {
    doctypeStart = "!DOCTYPE"
    doctypeEnd = ">"
    procInstStart = "<?"
    procInstEnd = "?>"
    tagOpenStart = "<"
    tagOpenEnd = ">"
    tagCloseStart = "</"
    tagCloseEnd = ">"
    tagShortStart = "<"
    tagShortEnd = "/>"
    attrStart = "=\""
    attrEnd = "\""
    commentStart = "<!--"
    commentEnd = "-->"
    cdataStart = "<![CDATA["
    cdataEnd = "]]>"
    textStart = ""
    textEnd = ""
    indent = DEFAULT_INDENT_SPACES
    regEntities = "[&'\"<>]".toRegex()
    regValEntities = "[&\"<>]".toRegex()
    encodeEntity { char: Char ->
        requireNotNull(entities[char])
    }
    pretty = false
    useShortTags = true
    eol = EndOfLine.LF
    finalNewline = false
}

private data class State(
    val indent: String,
    val textContext: XastElement?,
    val indentLevel: Int,
)

/**
 * convert XAST to SVG string
 */
fun stringifySvg(data: XastRoot, userOptions: StringifyOptions?): String {
    var config = defaults.merge(userOptions)
    val newIndent = when (val indent = config.indent) {
        is Indent.Spaces -> if (indent.count < 0) "\t" else " ".repeat(indent.count)
        is Indent.Custom -> indent.value
        null -> "    "
    }

    val state = State(
        indent = newIndent,
        textContext = null,
        indentLevel = 0,
    )
    val eol = if (config.eol == EndOfLine.CRLF) "\r\n" else "\n"
    if (config.pretty == true) {
        config = config.copy(
            doctypeEnd = config.doctypeEnd + eol,
            procInstEnd = config.procInstEnd + eol,
            commentEnd = config.commentEnd + eol,
            cdataEnd = config.cdataEnd + eol,
            tagShortEnd = config.tagShortEnd + eol,
            tagOpenEnd = config.tagOpenEnd + eol,
            tagCloseEnd = config.tagCloseEnd + eol,
            textEnd = config.textEnd + eol,
        )
    }

    var svg = stringifyNode(data, config, state)
    if (config.finalNewline == true && svg.isNotEmpty() && svg.endsWith("\n").not()) {
        svg += eol
    }

    return svg
}

private fun stringifyNode(
    data: XastParent,
    config: StringifyOptions,
    state: State,
): String = buildString {
    // state.indentLevel += 1
    val childState = state.copy(indentLevel = state.indentLevel + 1)
    for (child in data.children) {
        append(
            when (child) {
                is XastCdata -> stringifyCdata(child, config, childState)
                is XastComment -> stringifyComment(child, config)
                is XastDoctype -> stringifyDoctype(child, config)
                is XastElement -> stringifyElement(child, config, childState)
                is XastInstruction -> stringifyInstruction(child, config)
                is XastText -> stringifyText(child, config, childState)
            },
        )
    }
    // state.indentLevel -=
}

/**
 * create indent string in accordance with the current node level.
 */
private fun createIndent(config: StringifyOptions, state: State): String =
    if (config.pretty == true && state.textContext == null) {
        state.indent.repeat(state.indentLevel - 1)
    } else {
        ""
    }

private fun stringifyCdata(
    node: XastCdata,
    config: StringifyOptions,
    state: State,
): String = createIndent(config, state) +
    config.cdataStart +
    node.value +
    config.cdataEnd

private fun stringifyComment(
    node: XastComment,
    config: StringifyOptions,
): String = config.commentStart +
    node.value +
    config.commentEnd

private fun stringifyDoctype(
    node: XastDoctype,
    config: StringifyOptions,
): String = config.doctypeStart +
    node.data.doctype +
    config.doctypeEnd

private fun stringifyElement(
    node: XastElement,
    config: StringifyOptions,
    state: State,
): String = if (node.children.isEmpty()) {
    // empty element and short tag
    createEmptyOrShortTagElement(node, config, state)
} else {
    // non-empty element
    createNotEmptyElement(node, config, state)
}

private fun createEmptyOrShortTagElement(
    node: XastElement,
    config: StringifyOptions,
    state: State,
): String = createIndent(config, state) +
    if (config.useShortTags == true) {
        config.tagShortStart +
            node.name +
            stringifyAttributes(node, config) +
            config.tagShortEnd
    } else {
        config.tagShortStart +
            node.name +
            stringifyAttributes(node, config) +
            config.tagOpenEnd +
            config.tagCloseStart +
            node.name +
            config.tagCloseEnd
    }

private fun createNotEmptyElement(
    node: XastElement,
    config: StringifyOptions,
    state: State,
): String {
    var tagOpenStart = config.tagOpenStart
    var tagOpenEnd = config.tagOpenEnd
    var tagCloseStart = config.tagCloseStart
    var tagCloseEnd = config.tagCloseEnd
    var openIndent = createIndent(config, state)
    var closeIndent = createIndent(config, state)
    var elementState = state

    if (state.textContext != null) {
        tagOpenStart = defaults.tagOpenStart
        tagOpenEnd = defaults.tagOpenEnd
        tagCloseStart = defaults.tagCloseStart
        tagCloseEnd = defaults.tagCloseEnd
        openIndent = ""
    } else if (Collections.textElements.contains(node.name)) {
        tagOpenEnd = defaults.tagOpenEnd
        tagCloseStart = defaults.tagCloseStart
        closeIndent = ""
        elementState = elementState.copy(textContext = node)
    }

    val children = stringifyNode(node, config, elementState)

    return openIndent +
        tagOpenStart +
        node.name +
        stringifyAttributes(node, config) +
        tagOpenEnd +
        children +
        closeIndent +
        tagCloseStart +
        node.name +
        tagCloseEnd
}

private fun stringifyInstruction(
    node: XastInstruction,
    config: StringifyOptions,
): String = "${config.procInstStart}${node.name} ${node.value}${config.procInstEnd}"

private fun stringifyText(
    node: XastText,
    config: StringifyOptions,
    state: State,
): String = createIndent(config, state) +
    config.textStart +
    node.value.replace(requireNotNull(config.regEntities)) {
        requireNotNull(config.encodeEntity)(it.value.single())
    } +
    if (state.textContext != null) "" else config.textEnd

private fun stringifyAttributes(
    node: XastElement,
    config: StringifyOptions,
): String = buildString {
    for ((name, value) in node.attributes) {
        // Remove attributes without values support in v3
        if (value.isNotEmpty()) {
            val encodedValue = value.replace(requireNotNull(config.regValEntities)) {
                requireNotNull(config.encodeEntity)(it.value.single())
            }
            append(" $name${config.attrStart}$encodedValue${config.attrEnd}")
        } else {
            append(" $name")
        }
    }
}
