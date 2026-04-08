package svgokt.domain

typealias EncodeEntityFn = (char: Char) -> String

sealed interface Indent {
    data class Spaces(val count: Int) : Indent
    data class Custom(val value: String) : Indent
}

data class StringifyOptions(
    val doctypeStart: String?,
    val doctypeEnd: String?,
    val procInstStart: String?,
    val procInstEnd: String?,
    val tagOpenStart: String?,
    val tagOpenEnd: String?,
    val tagCloseStart: String?,
    val tagCloseEnd: String?,
    val tagShortStart: String?,
    val tagShortEnd: String?,
    val attrStart: String?,
    val attrEnd: String?,
    val commentStart: String?,
    val commentEnd: String?,
    val cdataStart: String?,
    val cdataEnd: String?,
    val textStart: String?,
    val textEnd: String?,
    val indent: Indent?,
    val regEntities: Regex?,
    val regValEntities: Regex?,
    val encodeEntity: EncodeEntityFn?,
    val pretty: Boolean?,
    val useShortTags: Boolean?,
    val eol: EndOfLine?,
    val finalNewline: Boolean?,
) {
    @Suppress("CyclomaticComplexMethod")
    fun merge(other: StringifyOptions?): StringifyOptions {
        other ?: return this
        return copy(
            doctypeStart = other.doctypeStart ?: doctypeStart,
            doctypeEnd = other.doctypeEnd ?: doctypeEnd,
            procInstStart = other.procInstStart ?: procInstStart,
            procInstEnd = other.procInstEnd ?: procInstEnd,
            tagOpenStart = other.tagOpenStart ?: tagOpenStart,
            tagOpenEnd = other.tagOpenEnd ?: tagOpenEnd,
            tagCloseStart = other.tagCloseStart ?: tagCloseStart,
            tagCloseEnd = other.tagCloseEnd ?: tagCloseEnd,
            tagShortStart = other.tagShortStart ?: tagShortStart,
            tagShortEnd = other.tagShortEnd ?: tagShortEnd,
            attrStart = other.attrStart ?: attrStart,
            attrEnd = other.attrEnd ?: attrEnd,
            commentStart = other.commentStart ?: commentStart,
            commentEnd = other.commentEnd ?: commentEnd,
            cdataStart = other.cdataStart ?: cdataStart,
            cdataEnd = other.cdataEnd ?: cdataEnd,
            textStart = other.textStart ?: textStart,
            textEnd = other.textEnd ?: textEnd,
            indent = other.indent ?: indent,
            regEntities = other.regEntities ?: regEntities,
            regValEntities = other.regValEntities ?: regValEntities,
            encodeEntity = other.encodeEntity ?: encodeEntity,
            pretty = other.pretty ?: pretty,
            useShortTags = other.useShortTags ?: useShortTags,
            eol = other.eol ?: eol,
            finalNewline = other.finalNewline ?: finalNewline,
        )
    }
}
