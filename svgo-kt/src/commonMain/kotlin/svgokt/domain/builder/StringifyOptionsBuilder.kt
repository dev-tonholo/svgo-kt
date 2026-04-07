package svgokt.domain.builder

import svgokt.domain.EncodeEntityFn
import svgokt.domain.EndOfLine
import svgokt.domain.Indent
import svgokt.domain.StringifyOptions

fun stringifyOptions(block: StringifyOptionsBuilder.() -> Unit): StringifyOptions =
    StringifyOptionsBuilder().apply(block).build()

@DslMarker
annotation class StringifyOptionsBuilderDsl

@StringifyOptionsBuilderDsl
class StringifyOptionsBuilder {
    var doctypeStart: String? = null
    var doctypeEnd: String? = null
    var procInstStart: String? = null
    var procInstEnd: String? = null
    var tagOpenStart: String? = null
    var tagOpenEnd: String? = null
    var tagCloseStart: String? = null
    var tagCloseEnd: String? = null
    var tagShortStart: String? = null
    var tagShortEnd: String? = null
    var attrStart: String? = null
    var attrEnd: String? = null
    var commentStart: String? = null
    var commentEnd: String? = null
    var cdataStart: String? = null
    var cdataEnd: String? = null
    var textStart: String? = null
    var textEnd: String? = null
    var indent: Int? = null
    var customIndent: String? = null
    var regEntities: Regex? = null
    var regValEntities: Regex? = null
    private var encodeEntity: EncodeEntityFn? = null
    var pretty: Boolean? = null
    var useShortTags: Boolean? = null
    var eol: EndOfLine? = null
    var finalNewline: Boolean? = null

    fun encodeEntity(fn: EncodeEntityFn) {
        encodeEntity = fn
    }

    fun build(): StringifyOptions = StringifyOptions(
        doctypeStart = doctypeStart,
        doctypeEnd = doctypeEnd,
        procInstStart = procInstStart,
        procInstEnd = procInstEnd,
        tagOpenStart = tagOpenStart,
        tagOpenEnd = tagOpenEnd,
        tagCloseStart = tagCloseStart,
        tagCloseEnd = tagCloseEnd,
        tagShortStart = tagShortStart,
        tagShortEnd = tagShortEnd,
        attrStart = attrStart,
        attrEnd = attrEnd,
        commentStart = commentStart,
        commentEnd = commentEnd,
        cdataStart = cdataStart,
        cdataEnd = cdataEnd,
        textStart = textStart,
        textEnd = textEnd,
        indent = customIndent?.let { Indent.Custom(value = it) }
            ?: indent?.let { Indent.Spaces(count = it) },
        regEntities = regEntities,
        regValEntities = regValEntities,
        encodeEntity = encodeEntity,
        pretty = pretty,
        useShortTags = useShortTags,
        eol = eol,
        finalNewline = finalNewline,
    )
}
