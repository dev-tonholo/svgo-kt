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
    fun merge(other: StringifyOptions?): StringifyOptions {
        other ?: return this
        var merged = this
        if (other.doctypeStart != null) merged = merged.copy(doctypeStart = other.doctypeStart)
        if (other.doctypeEnd != null) merged = merged.copy(doctypeEnd = other.doctypeEnd)
        if (other.procInstStart != null) merged = merged.copy(procInstStart = other.procInstStart)
        if (other.procInstEnd != null) merged = merged.copy(procInstEnd = other.procInstEnd)
        if (other.tagOpenStart != null) merged = merged.copy(tagOpenStart = other.tagOpenStart)
        if (other.tagOpenEnd != null) merged = merged.copy(tagOpenEnd = other.tagOpenEnd)
        if (other.tagCloseStart != null) merged = merged.copy(tagCloseStart = other.tagCloseStart)
        if (other.tagCloseEnd != null) merged = merged.copy(tagCloseEnd = other.tagCloseEnd)
        if (other.tagShortStart != null) merged = merged.copy(tagShortStart = other.tagShortStart)
        if (other.tagShortEnd != null) merged = merged.copy(tagShortEnd = other.tagShortEnd)
        if (other.attrStart != null) merged = merged.copy(attrStart = other.attrStart)
        if (other.attrEnd != null) merged = merged.copy(attrEnd = other.attrEnd)
        if (other.commentStart != null) merged = merged.copy(commentStart = other.commentStart)
        if (other.commentEnd != null) merged = merged.copy(commentEnd = other.commentEnd)
        if (other.cdataStart != null) merged = merged.copy(cdataStart = other.cdataStart)
        if (other.cdataEnd != null) merged = merged.copy(cdataEnd = other.cdataEnd)
        if (other.textStart != null) merged = merged.copy(textStart = other.textStart)
        if (other.textEnd != null) merged = merged.copy(textEnd = other.textEnd)
        if (other.indent != null) merged = merged.copy(indent = other.indent)
        if (other.regEntities != null) merged = merged.copy(regEntities = other.regEntities)
        if (other.regValEntities != null) merged = merged.copy(regValEntities = other.regValEntities)
        if (other.encodeEntity != null) merged = merged.copy(encodeEntity = other.encodeEntity)
        if (other.pretty != null) merged = merged.copy(pretty = other.pretty)
        if (other.useShortTags != null) merged = merged.copy(useShortTags = other.useShortTags)
        if (other.eol != null) merged = merged.copy(eol = other.eol)
        if (other.finalNewline != null) merged = merged.copy(finalNewline = other.finalNewline)
        return merged
    }
}
