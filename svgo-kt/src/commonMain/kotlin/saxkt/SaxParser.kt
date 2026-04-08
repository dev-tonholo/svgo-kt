package saxkt

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import saxkt.domain.PositionTracker
import saxkt.domain.QName
import saxkt.domain.SaxAttribute
import saxkt.domain.SaxError
import saxkt.domain.SaxErrorException
import saxkt.domain.SaxEvent
import saxkt.domain.SaxNamespace
import saxkt.domain.SaxOptions
import saxkt.domain.SaxTag
import saxkt.domain.copy
import saxkt.extensions.SaxExtensions.isAttributeEnd
import saxkt.extensions.SaxExtensions.isMatch
import saxkt.extensions.SaxExtensions.isNotMatch
import saxkt.extensions.SaxExtensions.isQuote
import saxkt.extensions.SaxExtensions.isSaxWhitespace
import kotlin.math.max

private const val EMPTY_CHAR = '\u0000'

// this really needs to be replaced with character classes.
// XML allows all manner of ridiculous numbers and digits.
private const val CDATA = "[CDATA["
private const val DOCTYPE = "DOCTYPE"
private const val XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace"
private const val XMLNS_NAMESPACE = "http://www.w3.org/2000/xmlns/"
private val rootNs = SaxNamespace("xml" to XML_NAMESPACE, "xmlns" to XMLNS_NAMESPACE)

// http://www.w3.org/TR/REC-xml/#NT-NameStartChar
// This implementation works on strings, a single character at a time
// as such, it cannot ever support astral-plane characters (10000-EFFFF)
// without a significant breaking change to either this  parser, or the
// JavaScript language.  Implementation of an emoji-capable xml parser
// is left as an exercise for the reader.
private val nameStart =
    """[:_A-Za-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD]""".toRegex()
private val nameBody =
    """[:_A-Za-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD\u00B7\u0300-\u036F\u203F-\u2040.\d-]""".toRegex()
private val entityStart =
    """[#:_A-Za-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD]""".toRegex()
private val entityBody =
    """[#:_A-Za-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD\u00B7\u0300-\u036F\u203F-\u2040.\d-]""".toRegex()

data class Buffer(
    val comment: String = "",
    val sgmlDecl: String = "",
    val textNode: String = "",
    val tagName: String = "",
    val doctype: String = "",
    val isDoctypeVisited: Boolean = false,
    val procInstName: String = "",
    val procInstBody: String = "",
    val entity: String = "",
    val attribName: String = "",
    val attribValue: String = "",
    val cdata: String = "",
    val script: String = "",
) {
    operator fun get(key: String): String = when (key) {
        "comment" -> comment
        "sgmlDecl" -> sgmlDecl
        "textNode" -> textNode
        "tagName" -> tagName
        "doctype" -> doctype
        "procInstName" -> procInstName
        "procInstBody" -> procInstBody
        "entity" -> entity
        "attribName" -> attribName
        "attribValue" -> attribValue
        "cdata" -> cdata
        "script" -> script
        else -> throw IllegalArgumentException("Unexpected buffer name $key")
    }

    companion object {
        val bufferList = setOf(
            "comment",
            "sgmlDecl",
            "textNode",
            "tagName",
            "doctype",
            "procInstName",
            "procInstBody",
            "entity",
            "attribName",
            "attribValue",
            "cdata",
            "script",
        )
    }
}

internal class SaxParser(
    internal val strict: Boolean,
    val options: SaxOptions,
) {
    private var q: Char = EMPTY_CHAR // what is q?

    /* reference to parser.c on sax.js */
    internal var currentChar: Char = EMPTY_CHAR
    internal val looseCase = { value: String ->
        if (options.lowercase) value.lowercase() else value.uppercase()
    }
    internal val tags = mutableListOf<SaxTag>()
    private var closed = false
    private var closedRoot = false
    internal var sawRoot = false
    internal var tag: SaxTag? = null
    internal var error: SaxError? = null
    internal val noScript = strict || options.noScript
    private val strictEntities = options.strictEntities
    internal val entities: MutableMap<String, String> = (if (strictEntities) Sax.XmlEntities else Sax.Entities)
        .mapValues { (_, value) -> value.toChar().toString() }
        .toMutableMap()
    internal val attribList = mutableListOf<Pair<String, Any>>()
    internal var entity: String = ""

    // namespaces form a prototype chain.
    // it always points at the current tag,
    // which protos to its parent tag.
    private val ns: SaxNamespace? = if (options.xmlns) rootNs else null
    internal val parentNs: SaxNamespace?
        get() = tags.lastOrNull()?.ns ?: ns

    // mostly just for error reporting
    private val trackPosition = options.position
    internal var positionTracker: PositionTracker? = if (trackPosition) PositionTracker() else null

    private var startTagPosition: Int = 0
    internal var buffer = Buffer()
    private var bufferCheckPosition = Sax.MaxBufferLength

    internal var state: Sax.State = Sax.State.BEGIN
    private val _events = MutableSharedFlow<SaxEvent>()
    val events: SharedFlow<SaxEvent> = _events.asSharedFlow()

    suspend fun write(chunk: String?): SaxParser {
        error?.let { throw SaxErrorException(error = it) }

        if (closed) {
            return error(parser = this, reason = "Cannot write after close. Assign an onready handler.")
        }

        var currentChunk = chunk ?: return end()

        var i = 0
        var currentChar: Char
        while (true) {
            currentChar = charAt(currentChunk, index = i++)
            this.currentChar = currentChar
            if (currentChar == EMPTY_CHAR) {
                break
            }

            trackPosition(currentChar)

            when (state) {
                Sax.State.BEGIN -> {
                    state = Sax.State.BEGIN_WHITESPACE
                    if (currentChar == '\uFEFF') {
                        continue
                    }
                    beginWhiteSpace(currentChar)
                    continue
                }

                Sax.State.BEGIN_WHITESPACE -> {
                    beginWhiteSpace(currentChar)
                    continue
                }

                Sax.State.TEXT -> {
                    if (sawRoot && !closedRoot) {
                        val startIndex = i - 1
                        while (currentChar != '<' && currentChar != '&') {
                            currentChar = charAt(currentChunk, index = i++)
                            trackPosition(currentChar)
                        }
                        updateBuffer {
                            copy(textNode = textNode + currentChunk.substring(startIndex, i - 1))
                        }
                    }

                    if (currentChar == '<' && !(sawRoot && closedRoot && !strict)) {
                        state = Sax.State.OPEN_WAKA
                        startTagPosition = positionTracker?.position.orZero() // TODO: is this right?
                    } else {
                        if (!currentChar.isSaxWhitespace() && (!sawRoot || closedRoot)) {
                            strictFail(parser = this, "Text data outside of root node.")
                        }
                        if (currentChar == '&') {
                            state = Sax.State.TEXT_ENTITY
                        } else {
                            updateBuffer { copy(textNode = textNode + currentChar) }
                        }
                    }
                    continue
                }

                Sax.State.SCRIPT -> {
                    // only non-strict
                    if (currentChar == '<') {
                        state = Sax.State.SCRIPT_ENDING
                    } else {
                        updateBuffer { copy(script = script + currentChar) }
                    }
                    continue
                }

                Sax.State.SCRIPT_ENDING -> {
                    if (currentChar == '/') {
                        state = Sax.State.CLOSE_TAG
                    } else {
                        updateBuffer { copy(script = "$script<$currentChar") }
                        state = Sax.State.SCRIPT
                    }
                    continue
                }

                Sax.State.OPEN_WAKA -> {
                    when {
                        // either a /, ?, !, or text is coming next.
                        currentChar == '!' -> {
                            state = Sax.State.SGML_DECL
                            updateBuffer { copy(sgmlDecl = "") }
                        }

                        // wait for it
                        currentChar.isSaxWhitespace() -> Unit

                        currentChar.isMatch(nameStart) -> {
                            state = Sax.State.OPEN_TAG
                            updateBuffer { copy(tagName = currentChar.toString()) }
                        }

                        currentChar == '/' -> {
                            state = Sax.State.CLOSE_TAG
                            updateBuffer { copy(tagName = "") }
                        }

                        currentChar == '?' -> {
                            state = Sax.State.PROC_INST
                            updateBuffer { copy(procInstBody = "") }
                        }

                        else -> {
                            strictFail(parser = this, "Unencoded <")
                            // if there was some whitespace, then add that in.
                            val paddedString = if (startTagPosition + 1 < positionTracker?.position.orZero()) {
                                val pad = positionTracker?.position.orZero() - startTagPosition
                                " ".repeat(pad) + currentChar
                            } else {
                                currentChar.toString()
                            }
                            updateBuffer { copy(textNode = "$textNode<$paddedString") }
                        }
                    }
                    continue
                }

                Sax.State.SGML_DECL -> {
                    when {
                        (buffer.sgmlDecl + currentChar.toString()).uppercase() == CDATA -> {
                            emitNode(SaxEvent.OpenCdata)
                            state = Sax.State.CDATA
                            updateBuffer { copy(sgmlDecl = "", cdata = "") }
                        }

                        (buffer.sgmlDecl + currentChar.toString()) == "--" -> {
                            state = Sax.State.COMMENT
                            updateBuffer { copy(comment = "", sgmlDecl = "") }
                        }

                        (buffer.sgmlDecl + currentChar.toString()).uppercase() == DOCTYPE -> {
                            state = Sax.State.DOCTYPE
                            if (buffer.isDoctypeVisited || sawRoot) {
                                strictFail(parser = this, "Inappropriately located doctype declaration")
                            }
                            updateBuffer { copy(doctype = "", sgmlDecl = "") }
                        }

                        currentChar == '>' -> {
                            emitNode(SaxEvent.SgmlDeclaration(declaration = buffer.sgmlDecl))
                            updateBuffer { copy(sgmlDecl = "") }
                            state = Sax.State.TEXT
                        }

                        currentChar.isQuote() -> {
                            state = Sax.State.SGML_DECL_QUOTED
                            updateBuffer { copy(sgmlDecl = buffer.sgmlDecl + currentChar) }
                        }

                        else -> updateBuffer { copy(sgmlDecl = buffer.sgmlDecl + currentChar) }
                    }
                    continue
                }

                Sax.State.SGML_DECL_QUOTED -> {
                    if (currentChar == q) {
                        state = Sax.State.SGML_DECL
                        q = EMPTY_CHAR
                    }
                    updateBuffer { copy(sgmlDecl = buffer.sgmlDecl + currentChar) }
                    continue
                }

                Sax.State.DOCTYPE -> {
                    if (currentChar == '>') {
                        state = Sax.State.TEXT
                        emitNode(SaxEvent.Doctype(data = buffer.doctype))
                        updateBuffer { copy(isDoctypeVisited = true) }
                    } else {
                        updateBuffer { copy(doctype = doctype + currentChar) }
                        if (currentChar == '[') {
                            state = Sax.State.DOCTYPE_DTD
                        } else if (currentChar.isQuote()) {
                            state = Sax.State.DOCTYPE_DTD_QUOTED
                            q = currentChar
                        }
                    }
                    continue
                }

                Sax.State.DOCTYPE_QUOTED -> {
                    updateBuffer { copy(doctype = doctype + currentChar) }
                    if (currentChar == q) {
                        q = EMPTY_CHAR
                        state = Sax.State.DOCTYPE
                    }
                    continue
                }

                Sax.State.DOCTYPE_DTD -> {
                    updateBuffer { copy(doctype = doctype + currentChar) }
                    if (currentChar == ']') {
                        state = Sax.State.DOCTYPE
                    } else if (currentChar.isQuote()) {
                        state = Sax.State.DOCTYPE_DTD_QUOTED
                        q = currentChar
                    }
                }

                Sax.State.DOCTYPE_DTD_QUOTED -> {
                    updateBuffer { copy(doctype = doctype + currentChar) }
                    if (currentChar == q) {
                        state = Sax.State.DOCTYPE_DTD
                        q = EMPTY_CHAR
                    }
                    continue
                }

                Sax.State.COMMENT -> {
                    if (currentChar == '-') {
                        state = Sax.State.COMMENT_ENDING
                    } else {
                        updateBuffer { copy(comment = comment + currentChar) }
                    }
                }

                Sax.State.COMMENT_ENDING -> {
                    if (currentChar == '-') {
                        state = Sax.State.COMMENT_ENDED
                        updateBuffer { copy(comment = textOpts(comment)) }
                        if (buffer.comment.isNotEmpty()) {
                            emitNode(SaxEvent.Comment(buffer.comment))
                        }
                        updateBuffer { copy(comment = "") }
                    } else {
                        updateBuffer { copy(comment = "$comment-$currentChar") }
                        state = Sax.State.COMMENT
                    }
                    continue
                }

                Sax.State.COMMENT_ENDED -> {
                    if (currentChar != '>') {
                        strictFail(parser = this, "Malformed comment")
                        // allow <!-- blah -- bloo --> in non-strict mode,
                        // which is a comment of " blah -- bloo "
                        updateBuffer { copy(comment = "$comment--$currentChar") }
                        state = Sax.State.COMMENT
                    } else {
                        state = Sax.State.TEXT
                    }
                    continue
                }

                Sax.State.CDATA -> {
                    if (currentChar == ']') {
                        state = Sax.State.CDATA_ENDING
                    } else {
                        updateBuffer { copy(cdata = cdata + currentChar) }
                    }
                    continue
                }

                Sax.State.CDATA_ENDING -> {
                    if (currentChar == ']') {
                        state = Sax.State.CDATA_ENDING_2
                    } else {
                        updateBuffer { copy(cdata = "$cdata]$currentChar") }
                    }
                    continue
                }

                Sax.State.CDATA_ENDING_2 -> {
                    when (currentChar) {
                        '>' -> {
                            if (buffer.cdata.isNotEmpty()) {
                                emitNode(SaxEvent.Cdata(buffer.cdata))
                            }
                            emitNode(SaxEvent.CloseCdata)
                            updateBuffer { copy(cdata = "") }
                            state = Sax.State.TEXT
                        }

                        ']' -> updateBuffer { copy(cdata = "$cdata]$currentChar") }
                        else -> {
                            updateBuffer { copy(cdata = "$cdata]]$currentChar") }
                            state = Sax.State.CDATA
                        }
                    }
                    continue
                }

                Sax.State.PROC_INST -> {
                    when {
                        currentChar == '?' -> state = Sax.State.PROC_INST_ENDING
                        currentChar.isSaxWhitespace() -> state = Sax.State.PROC_INST_BODY
                        else -> updateBuffer { copy(procInstName = procInstName + currentChar) }
                    }
                    continue
                }

                Sax.State.PROC_INST_BODY -> {
                    when {
                        buffer.procInstBody.isEmpty() && currentChar.isSaxWhitespace() -> continue
                        currentChar == '?' -> state = Sax.State.PROC_INST_ENDING
                        else -> updateBuffer { copy(procInstBody = procInstBody + currentChar) }
                    }
                }

                Sax.State.PROC_INST_ENDING -> {
                    if (currentChar == '>') {
                        emitNode(
                            SaxEvent.ProcessingInstruction(
                                name = buffer.procInstName,
                                body = buffer.procInstBody,
                            ),
                        )
                        updateBuffer { copy(procInstName = "") }
                        state = Sax.State.TEXT
                    } else {
                        updateBuffer { copy(procInstBody = "$procInstBody?$currentChar") }
                        state = Sax.State.PROC_INST_BODY
                    }
                    continue
                }

                Sax.State.OPEN_TAG -> {
                    when {
                        currentChar.isMatch(nameBody) -> updateBuffer { copy(tagName = tagName + currentChar) }
                        else -> {
                            newTag()
                            when (currentChar) {
                                '>' -> openTag(parser = this)
                                '/' -> state = Sax.State.OPEN_TAG_SLASH
                                else -> {
                                    if (!currentChar.isSaxWhitespace()) {
                                        strictFail(parser = this, "Invalid character in tag name")
                                    }
                                    state = Sax.State.ATTRIB
                                }
                            }
                        }
                    }

                    continue
                }

                Sax.State.OPEN_TAG_SLASH -> {
                    if (currentChar == '>') {
                        openTag(parser = this, selfClosing = true)
                        closeTag()
                    } else {
                        strictFail(parser = this, "Forward-slash in opening tag not followed by >")
                        state = Sax.State.ATTRIB
                    }
                    continue
                }

                Sax.State.ATTRIB -> {
                    // haven't read the attribute name yet.
                    when {
                        currentChar.isSaxWhitespace() -> continue
                        currentChar == '>' -> openTag(parser = this)
                        currentChar == '/' -> state = Sax.State.OPEN_TAG_SLASH
                        currentChar.isMatch(nameStart) -> {
                            updateBuffer {
                                copy(
                                    attribName = currentChar.toString(),
                                    attribValue = "",
                                )
                            }
                            state = Sax.State.ATTRIB_NAME
                        }

                        else -> strictFail(parser = this, "Invalid attribute name")
                    }
                    continue
                }

                Sax.State.ATTRIB_NAME -> {
                    when {
                        currentChar == '=' -> state = Sax.State.ATTRIB_VALUE

                        currentChar == '>' -> {
                            strictFail(parser = this, "Attribute without value")
                            updateBuffer { copy(attribValue = attribName) }
                            attrib(parser = this)
                            openTag(parser = this)
                        }

                        currentChar.isSaxWhitespace() -> state = Sax.State.ATTRIB_NAME_SAW_WHITE

                        currentChar.isMatch(nameBody) -> updateBuffer {
                            copy(attribName = attribName + currentChar)
                        }

                        else -> strictFail(parser = this, "Invalid attribute name")
                    }
                    continue
                }

                Sax.State.ATTRIB_NAME_SAW_WHITE -> {
                    if (currentChar.isSaxWhitespace()) continue

                    if (currentChar == '=') {
                        state = Sax.State.ATTRIB_VALUE
                    } else {
                        strictFail(parser = this, "Attribute without value")
                        val attribute = SaxAttribute(name = buffer.attribName, value = buffer.attribValue)
                        tag = tag?.copy(
                            attributes = buildMap {
                                tag?.attributes?.let { putAll(it) }
                                put(buffer.attribName, attribute)
                            }
                        )
                        emitNode(SaxEvent.Attribute(attribute))
                        updateBuffer { copy(attribValue = "", attribName = "") }
                        when {
                            currentChar == '>' -> openTag(parser = this)
                            currentChar.isMatch(nameStart) -> {
                                updateBuffer { copy(attribName = currentChar.toString()) }
                                state = Sax.State.ATTRIB_NAME
                            }

                            else -> {
                                strictFail(parser = this, "Invalid attribute name")
                                state = Sax.State.ATTRIB
                            }
                        }
                    }

                    continue
                }

                Sax.State.ATTRIB_VALUE -> {
                    when {
                        currentChar.isSaxWhitespace() -> continue
                        currentChar.isQuote() -> {
                            q = currentChar
                            state = Sax.State.ATTRIB_VALUE_QUOTED
                        }

                        else -> {
                            strictFail(parser = this, "Unquoted attribute value")
                            state = Sax.State.ATTRIB_VALUE_UNQUOTED
                            updateBuffer { copy(attribValue = currentChar.toString()) }
                        }
                    }
                    continue
                }

                Sax.State.ATTRIB_VALUE_QUOTED -> {
                    if (currentChar != q) {
                        if (currentChar == '&') {
                            state = Sax.State.ATTRIB_VALUE_ENTITY_Q
                        } else {
                            updateBuffer { copy(attribValue = attribValue + currentChar) }
                        }
                        continue
                    }
                    attrib(parser = this)
                    q = EMPTY_CHAR
                    state = Sax.State.ATTRIB_VALUE_CLOSED
                    continue
                }

                Sax.State.ATTRIB_VALUE_CLOSED -> {
                    when {
                        currentChar.isSaxWhitespace() -> state = Sax.State.ATTRIB
                        currentChar == '>' -> openTag(parser = this)
                        currentChar == '/' -> state = Sax.State.OPEN_TAG_SLASH
                        currentChar.isMatch(nameStart) -> {
                            strictFail(parser = this, "No whitespace between attributes")
                            updateBuffer { copy(attribName = currentChar.toString(), attribValue = "") }
                            state = Sax.State.ATTRIB_NAME
                        }

                        else -> strictFail(parser = this, "Invalid attribute name")
                    }
                    continue
                }

                Sax.State.ATTRIB_VALUE_UNQUOTED -> {
                    if (!currentChar.isAttributeEnd()) {
                        if (currentChar == '&') {
                            state = Sax.State.ATTRIB_VALUE_ENTITY_U
                        } else {
                            updateBuffer { copy(attribValue = attribValue + currentChar) }
                        }
                        continue
                    }
                    attrib(parser = this)
                    if (currentChar == '>') {
                        openTag(parser = this)
                    } else {
                        state = Sax.State.ATTRIB
                    }

                    continue
                }

                Sax.State.CLOSE_TAG -> {
                    when {
                        buffer.tagName.isEmpty() -> when {
                            currentChar.isSaxWhitespace() -> continue
                            currentChar.isNotMatch(nameStart) -> {
                                if (buffer.script.isNotEmpty()) {
                                    updateBuffer { copy(script = "$script</$currentChar") }
                                    state = Sax.State.SCRIPT
                                } else {
                                    strictFail(parser = this, "Invalid tagname in closing tag.")
                                }
                            }

                            else -> updateBuffer { copy(tagName = currentChar.toString()) }
                        }

                        currentChar == '>' -> closeTag()

                        currentChar.isMatch(nameBody) -> updateBuffer {
                            copy(tagName = tagName + currentChar)
                        }

                        buffer.script.isNotEmpty() -> updateBuffer {
                            state = Sax.State.SCRIPT
                            copy(
                                script = "$script</$currentChar",
                                tagName = "",
                            )
                        }

                        currentChar.isSaxWhitespace() -> strictFail(parser = this, "Invalid tagname in closing tag")

                        else -> state = Sax.State.CLOSE_TAG_SAW_WHITE
                    }
                    continue
                }

                Sax.State.CLOSE_TAG_SAW_WHITE -> {
                    if (currentChar.isSaxWhitespace()) continue

                    if (currentChar == '>') {
                        closeTag()
                    } else {
                        strictFail(parser = this, "Invalid characters in closing tag")
                    }
                    continue
                }

                Sax.State.TEXT_ENTITY,
                Sax.State.ATTRIB_VALUE_ENTITY_Q,
                Sax.State.ATTRIB_VALUE_ENTITY_U -> {
                    val attributeValueUpdater = { value: String ->
                        updateBuffer {
                            copy(attribValue = attribValue + value)
                        }
                    }
                    val (returnState, bufferUpdater) = when (state) {
                        Sax.State.TEXT_ENTITY -> Sax.State.TEXT to { value: String ->
                            updateBuffer {
                                copy(textNode = textNode + value)
                            }
                        }

                        Sax.State.ATTRIB_VALUE_ENTITY_Q -> Sax.State.ATTRIB_VALUE_QUOTED to attributeValueUpdater
                        Sax.State.ATTRIB_VALUE_ENTITY_U -> Sax.State.ATTRIB_VALUE_UNQUOTED to attributeValueUpdater
                        else -> throw IllegalStateException("not expected state")
                    }

                    val entityMatcher = if (entity.isNotEmpty()) entityBody else entityStart
                    when {
                        currentChar == ';' -> {
                            val parsedEntity = parseEntity(parser = this)

                            // Custom entities can contain tags, so we potentially need to parse the result
                            if (state == Sax.State.TEXT_ENTITY &&
                                Sax.Entities.containsKey(entity).not() &&
                                parsedEntity != "&$entity;"
                            ) {
                                currentChunk = currentChunk.slice(0..i) +
                                    parsedEntity +
                                    currentChunk.slice(i..currentChunk.lastIndex)
                            } else {
                                bufferUpdater(parsedEntity)
                            }
                            entity = ""
                            state = returnState
                        }

                        currentChar.isMatch(entityMatcher) -> entity += currentChar

                        else -> {
                            strictFail(parser = this, "Invalid character in entity name")
                            bufferUpdater("&$entity$currentChar")
                            entity = ""
                            state = returnState
                        }
                    }
                    continue
                }

                else -> throw SaxErrorException(
                    SaxError(reason = "Parsing SVG", message = "Unknown state: $state")
                )
            }
        } // while

        if ((positionTracker?.position ?: 0) >= bufferCheckPosition) {
            checkBufferLength()
        }

        return this
    }

    suspend fun close() {
        write(chunk = null)
    }

    private suspend fun end(): SaxParser {
        if (sawRoot && !closedRoot) strictFail(parser = this, "Unclosed root tag")
        if (state != Sax.State.BEGIN && state != Sax.State.BEGIN_WHITESPACE && state != Sax.State.TEXT) {
            error("Unexpected end")
        }
        closeText()
        currentChar = EMPTY_CHAR
        closed = true
        tryEmit(SaxEvent.End)
        return SaxParser(strict = strict, options = options) // end and return a new parser.
    }

    private suspend fun checkBufferLength() {
        val maxAllowed = max(Sax.MaxBufferLength, 10)
        var maxActual = 0
        // TODO: Find a better way to solve this.
        for (bufferKey in Buffer.bufferList) {
            val len = buffer[bufferKey].length
            if (len > maxAllowed) {
                // Text/cdata nodes can get big, and since they're buffered,
                // we can get here under normal conditions.
                // Avoid issues by emitting the text node now,
                // so at least it won't get any bigger.
                when (bufferKey) {
                    "textNode" -> closeText()
                    "cdata" -> {
                        emitNode(event = SaxEvent.Cdata(value = buffer.cdata))
                        updateBuffer { copy(cdata = "") }
                    }

                    "script" -> {
                        emitNode(event = SaxEvent.Script(value = buffer.script))
                        updateBuffer { copy(script = "") }
                    }

                    else -> error(parser = this, reason = "Max buffer length exceeded: $bufferKey")
                }
            }
            maxActual = max(maxActual, len)
        }
        // schedule the next check for the earliest possible buffer overrun.
        val m = Sax.MaxBufferLength - maxActual
        bufferCheckPosition = m + (positionTracker?.position ?: 0)
    }

    suspend fun flush() {
        closeText()
        if (buffer.cdata.isNotEmpty()) {
            emitNode(SaxEvent.Cdata(buffer.cdata))
            updateBuffer { copy(cdata = "") }
        }
        if (buffer.script.isNotEmpty()) {
            emitNode(SaxEvent.Script(buffer.script))
            updateBuffer { copy(script = "") }
        }
    }

    private suspend fun emitNode(event: SaxEvent) {
        if (buffer.textNode.isNotEmpty()) closeText()
        tryEmit(event)
    }

    private suspend fun closeTag() {
        if (buffer.tagName.isEmpty()) {
            strictFail(parser = this, "Weird empty close tag.")
            updateBuffer { copy(textNode = "$textNode</>") }
            state = Sax.State.TEXT
            return
        }

        if (buffer.script.isNotEmpty()) {
            updateBuffer {
                copy(
                    script = "$script</$tagName>",
                    tagName = "",
                )
            }
            state = Sax.State.SCRIPT
            return
        }

        // first make sure that the closing tag actually exists.
        // <a><b></c></b></a> will close everything, otherwise.
        var t = tags.size
        var tagName = buffer.tagName
        if (!strict) {
            tagName = looseCase(tagName)
        }
        val closeTo = tagName
        while (t-- >= 0) {
            val close = tags[t]
            if (close.name != closeTo) {
                // fail the first time in strict mode
                strictFail(parser = this, "Unexpected close tag")
            } else {
                break
            }
        }

        // didn't find it.  we already failed for strict, so just abort.
        if (t < 0) {
            strictFail(parser = this, "Unmatched closing tag: ${buffer.tagName}")
            updateBuffer { copy(textNode = "$textNode</${this.tagName}>") }
            state = Sax.State.TEXT
            return
        }

        updateBuffer { copy(tagName = tagName) }
        var s = tags.size
        while (s-- > t) {
            val tag = tags.removeLast().also { this.tag = it }
            updateBuffer { copy(tagName = tag.name) }
            emitNode(SaxEvent.CloseTag(buffer.tagName))

            val parentNS = tags.getOrNull(tags.size - 1) ?: /*parser.*/ns
            val currentNamespace = tag.ns
            if (currentNamespace != null && parentNS != currentNamespace) {
                // remove namespace bindings introduced by tag
                currentNamespace.forEach { (prefix, uri) ->
                    emitNode(
                        SaxEvent.CloseNamespace(
                            prefix = prefix,
                            uri = uri,
                        )
                    )
                }
            }
        }

        if (t == 0) {
            closedRoot = true
        }
        updateBuffer { copy(tagName = "", attribValue = "", attribName = "") }
        attribList.clear()
        state = Sax.State.TEXT
    }

    private suspend fun newTag() {
        if (!strict) updateBuffer { copy(tagName = looseCase(tagName)) }
        val tag = SaxTag(
            name = buffer.tagName,
            attributes = hashMapOf(),
            // will be overridden if tag contains an xmlns="foo" or xmlns:foo="bar"
            ns = if (options.xmlns) {
                tags.getOrNull(index = tags.size - 1)?.ns
            } else {
                null
            },
        )
        this.tag = tag
        attribList.clear()
        tryEmit(SaxEvent.OpenTagStart(tag))
    }

    private fun trackPosition(currentChar: Char) {
        if (trackPosition) {
            positionTracker = positionTracker?.copy(
                position = positionTracker?.position.orZero() + 1,
                line = if (currentChar == '\n') {
                    positionTracker?.line.orZero() + 1
                } else {
                    positionTracker?.line
                },
                column = if (currentChar == '\n') 0 else positionTracker?.column.orZero() + 1,
            )
        }
    }

    private suspend fun beginWhiteSpace(currentChar: Char) {
        if (currentChar == '<') {
            state = Sax.State.OPEN_WAKA
            startTagPosition = positionTracker?.position.orZero() // TODO: is this right?
        } else if (!currentChar.isSaxWhitespace()) {
            // have to process this as a text node.
            // weird, but happens.
            strictFail(parser = this, "Non-whitespace before first tag.")
            updateBuffer { copy(textNode = currentChar.toString()) }
            state = Sax.State.TEXT
        }
    }

    private fun charAt(chunk: String, index: Int): Char = if (index < chunk.length) {
        chunk.elementAt(index)
    } else {
        EMPTY_CHAR
    }

    private fun textOpts(text: String): String {
        var textOpts = if (options.trim) text.trim() else text
        textOpts = if (options.normalize) textOpts.replace("""/\s+/g""".toRegex(), " ") else textOpts
        return textOpts
    }

    internal suspend fun closeText() {
        updateBuffer { copy(textNode = textOpts(buffer.textNode)) }
        if (buffer.textNode.isNotEmpty()) tryEmit(SaxEvent.Text(textNode = buffer.textNode))
        updateBuffer { copy(textNode = "") }
    }

    internal fun updateBuffer(block: Buffer.() -> Buffer) {
        buffer = block(buffer)
    }

    private fun Int?.orZero() = this ?: 0

    internal suspend fun tryEmit(event: SaxEvent) {
//        println("tryEmit() called with: event = $event")
        _events.emit(event)
    }

    internal fun addToEntity(key: String, value: String) {
        entities[key] = value
    }
}

private suspend fun openTag(parser: SaxParser, selfClosing: Boolean = false) {
    var tag = parser.tag ?: throw SaxErrorException(
        SaxError(
            reason = "Unable to open tag",
            message = "Cannot open a non existent tag.",
        )
    )

    if (parser.options.xmlns) {
        // add namespace info to tag
        val qn = qname(parser.buffer.tagName)

        // emit namespace binding events
        tag = tag.copy(
            prefix = qn.prefix,
            local = qn.local,
            uri = tag.ns?.get(qn.prefix) ?: "",
        )

        if (!tag.prefix.isNullOrEmpty() && tag.uri.isNullOrEmpty()) {
            strictFail(parser, "Unbound namespace prefix: ${parser.buffer.tagName}")
            tag = tag.copy(uri = qn.prefix)
        }

        val parentNS = parser.parentNs
        val tagNs = tag.ns
        if (tagNs != null && parentNS != tagNs) {
            tagNs.forEach { (prefix, uri) ->
                parser.tryEmit(
                    SaxEvent.OpenNamespace(
                        prefix = prefix,
                        uri = uri,
                    )
                )
            }
        }

        // handle deferred onattribute events
        // Note: do not apply default ns to attributes:
        //   http://www.w3.org/TR/REC-xml-names/#defaulting
        for ((name, value) in parser.attribList) {
            val (prefix, local) = qname(name, isAttribute = true)
            val uri = if (prefix.isEmpty()) "" else (tag.ns?.get(prefix) ?: parser.parentNs?.get(prefix) ?: "")
            var attribute = SaxAttribute(
                name = name,
                value = value,
                prefix = prefix,
                local = local,
                uri = uri,
            )

            // if there's any attributes with an undefined namespace,
            // then fail on them now.
            if (prefix != "xmlns" && prefix.isNotEmpty() && uri.isEmpty()) {
                strictFail(parser, "Unbound namespace prefix: $prefix")
                attribute = attribute.copy(uri = prefix)
            }

            tag = tag.copy(
                attributes = buildMap {
                    putAll(tag.attributes)
                    put(name, attribute)
                },
            ).also { parser.tag = it }
            parser.tryEmit(SaxEvent.Attribute(attribute = attribute))
        }
        parser.attribList.clear()
    }

    tag = tag.copy(isSelfClosing = !selfClosing).also { parser.tag = it }

    // process the tag
    parser.sawRoot = true
    parser.tags += tag
    parser.tryEmit(SaxEvent.OpenTag(tag = tag))
    if (!selfClosing) {
        // special case for <script> in non-strict mode.
        parser.state = if (!parser.noScript && parser.buffer.tagName.lowercase() == "script") {
            Sax.State.SCRIPT
        } else {
            Sax.State.TEXT
        }
        parser.tag = null
        parser.updateBuffer { copy(tagName = "") }
    }

    parser.updateBuffer { copy(attribValue = "", attribName = "") }
    parser.attribList.clear()
}

private suspend fun attrib(parser: SaxParser) {
    if (!parser.strict) {
        parser.updateBuffer { copy(attribName = parser.looseCase(attribName)) }
    }

    val inAttributeList = parser.attribList.indexOfFirst { it.first == parser.buffer.attribName } != -1
    val inTagAttributes = parser.tag?.attributes?.get(parser.buffer.attribName) != null
    if (inAttributeList || inTagAttributes) {
        parser.updateBuffer {
            copy(attribName = "", attribValue = "")
        }
        return
    }

    val (prefix, local) = qname(parser.buffer.attribName, isAttribute = true)
    if (parser.options.xmlns) {
        if (prefix == "xmlns") {
            // namespace binding attribute. push the binding into scope
            when {
                local == "xml" && parser.buffer.attribValue != XML_NAMESPACE -> strictFail(
                    parser = parser,
                    message = "xml: prefix must be bound to $XML_NAMESPACE\nActual: ${parser.buffer.attribValue}",
                )

                local == "xmlns" && parser.buffer.attribValue != XMLNS_NAMESPACE -> strictFail(
                    parser = parser,
                    message = "xmlns: prefix must be bound to $XMLNS_NAMESPACE\nActual: ${parser.buffer.attribValue}",
                )

                else -> {
                    var tag = parser.tag ?: throw SaxErrorException(
                        SaxError(reason = "Attribute parse", message = "Trying to access null tag."),
                    )

                    val parentNs = parser.parentNs
                    if (tag.ns == parentNs) {
                        tag = tag.copy(ns = parentNs?.copy())
                    }

                    val ns = SaxNamespace(
                        buildMap {
                            // Always include parent bindings as base so inherited prefixes
                            // (e.g. xml, xmlns from rootNs) are available in this tag scope.
                            parentNs?.let { putAll(it) }
                            tag.ns?.let { putAll(it) }
                            put(local, parser.buffer.attribValue)
                        }
                    )
                    parser.tag = tag.copy(ns = ns)
                }
            }
        }

        // defer onattribute events until all attributes have been seen
        // so any new bindings can take effect. preserve attribute order
        // so deferred events can be emitted in document order
        parser.attribList += parser.buffer.attribName to parser.buffer.attribValue
    } else {
        // in non-xmlns mode, we can emit the event right away
        val attribute = SaxAttribute(
            name = parser.buffer.attribName,
            value = parser.buffer.attribValue,
            prefix = prefix,
            local = local,
            uri = if (prefix.isEmpty()) "" else parser.tag?.ns?.get(prefix) ?: "",
        )
        parser.tag = parser.tag?.copy(
            attributes = buildMap {
                parser.tag?.attributes?.let { putAll(it) }
                put(parser.buffer.attribName, attribute)
            }
        )
        parser.tryEmit(SaxEvent.Attribute(attribute))
    }
    parser.updateBuffer {
        copy(attribName = "", attribValue = "")
    }
}

private fun qname(name: String, isAttribute: Boolean = false): QName {
    val index = name.indexOf(":")
    val qualifierName = if (index < 0) listOf("", name) else name.split(":")
    var (prefix, local) = qualifierName
    val xmlns = "xmlns"
    // <x "xmlns"="http://foo">
    if (isAttribute && name == xmlns) {
        prefix = xmlns
        local = ""
    }

    return QName(
        prefix = prefix,
        local = local,
    )
}

private suspend fun strictFail(parser: SaxParser, message: String) {
    if (parser.options.strict) {
        error(parser = parser, reason = message)
    }
}

private suspend fun error(parser: SaxParser, reason: String): SaxParser {
    parser.closeText()

    val message = """$reason
            |Line: ${parser.positionTracker?.line}
            |Column: ${parser.positionTracker?.column}
            |Char: ${parser.currentChar}
    """.trimMargin()

    val error = SaxError(
        reason = reason,
        message = message,
        positionTracker = parser.positionTracker,
    )
    parser.error = error
    parser.tryEmit(SaxEvent.Error(error))

    return parser
}

private suspend fun parseEntity(parser: SaxParser): String {
    var entity = parser.entity
    val entityLowercase = entity.lowercase()
    var num: Int? = null
    var numStr = ""

    parser.entities[entity]?.let { return it }
    parser.entities[entityLowercase]?.let { return it }
    entity = entityLowercase
    if (entity[0] == '#') {
        if (entity[1] == 'x') {
            entity = entity.slice(2..entity.lastIndex)
            num = entity.toIntOrNull(radix = 16)
            numStr = num?.toString(radix = 16) ?: ""
        } else {
            entity = entity.slice(1..entity.lastIndex)
            num = entity.toIntOrNull(radix = 10)
            numStr = num?.toString(radix = 10) ?: ""
        }
    }
    entity = entity.replace("""^0+""".toRegex(), "")
    return if (num == null || numStr.lowercase() != entity) {
        strictFail(parser, "Invalid character entity")
        "&${parser.entity};"
    } else {
        // TODO: Maybe only num.toChar().toString() works well...
        numStr.toInt().toChar().toString()
    }
}
