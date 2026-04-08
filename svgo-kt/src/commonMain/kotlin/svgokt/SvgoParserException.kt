package svgokt

class SvgoParserException(
    override val message: String,
    val line: Int,
    val column: Int,
    val source: String,
    val file: String,
) : Exception(message) {
    override fun toString(): String {
        return """
            |SvgoParserException(
            |   message='$message',
            |   line=$line,
            |   column=$column,
            |   source='$source',
            |   file='$file',
            |)
            |
        """.trimMargin()
    }
}
