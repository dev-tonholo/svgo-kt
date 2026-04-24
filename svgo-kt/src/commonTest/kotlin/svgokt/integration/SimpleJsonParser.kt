@file:Suppress("TooManyFunctions", "MagicNumber")

package svgokt.integration

/**
 * Minimal recursive-descent JSON parser for fixture params.
 *
 * Supports: objects, arrays, strings, numbers (Int/Double), booleans, null.
 * Does not support escape sequences beyond the basics (\\, \", \/, \n, \t, \r, \b, \f, \uXXXX).
 * This is intentionally simple; it only needs to handle svgo fixture params.
 */
internal object SimpleJsonParser {

    fun parse(json: String): Any? {
        val state = ParserState(input = json.trim())
        val result = parseValue(state)
        state.skipWhitespace()
        check(state.isExhausted()) {
            "Unexpected trailing content at position ${state.pos}: '${state.remaining()}'"
        }
        return result
    }

    private fun parseValue(state: ParserState): Any? {
        state.skipWhitespace()
        check(!state.isExhausted()) { "Unexpected end of input" }
        return when (state.peek()) {
            '"' -> parseString(state)
            '{' -> parseObject(state)
            '[' -> parseArray(state)
            't', 'f' -> parseBoolean(state)
            'n' -> parseNull(state)
            else -> parseNumber(state)
        }
    }

    private fun parseString(state: ParserState): String {
        state.expect('"')
        val sb = StringBuilder()
        while (!state.isExhausted()) {
            val ch = state.advance()
            if (ch == '"') return sb.toString()
            if (ch == '\\') {
                sb.append(parseEscape(state))
            } else {
                sb.append(ch)
            }
        }
        error("Unterminated string")
    }

    private fun parseEscape(state: ParserState): Char {
        val ch = state.advance()
        return when (ch) {
            '"' -> '"'
            '\\' -> '\\'
            '/' -> '/'
            'n' -> '\n'
            't' -> '\t'
            'r' -> '\r'
            'b' -> '\b'
            'f' -> '\u000C'
            'u' -> {
                val hex = state.take(count = 4)
                hex.toInt(radix = 16).toChar()
            }
            else -> error("Unknown escape: \\$ch")
        }
    }

    private fun parseObject(state: ParserState): Map<String, Any?> {
        state.expect('{')
        state.skipWhitespace()
        val map = linkedMapOf<String, Any?>()
        if (state.peek() == '}') {
            state.advance()
            return map
        }
        while (true) {
            state.skipWhitespace()
            val key = parseString(state)
            state.skipWhitespace()
            state.expect(':')
            val value = parseValue(state)
            map[key] = value
            state.skipWhitespace()
            when (state.peek()) {
                ',' -> state.advance()
                '}' -> {
                    state.advance()
                    return map
                }
                else -> error("Expected ',' or '}' at position ${state.pos}")
            }
        }
    }

    private fun parseArray(state: ParserState): List<Any?> {
        state.expect('[')
        state.skipWhitespace()
        val list = mutableListOf<Any?>()
        if (state.peek() == ']') {
            state.advance()
            return list
        }
        while (true) {
            list.add(parseValue(state))
            state.skipWhitespace()
            when (state.peek()) {
                ',' -> state.advance()
                ']' -> {
                    state.advance()
                    return list
                }
                else -> error("Expected ',' or ']' at position ${state.pos}")
            }
        }
    }

    private fun parseBoolean(state: ParserState): Boolean {
        return if (state.startsWith("true")) {
            state.skip(count = 4)
            true
        } else if (state.startsWith("false")) {
            state.skip(count = 5)
            false
        } else {
            error("Expected boolean at position ${state.pos}")
        }
    }

    private fun parseNull(state: ParserState): Any? {
        check(state.startsWith("null")) { "Expected null at position ${state.pos}" }
        state.skip(count = 4)
        return null
    }

    private fun parseNumber(state: ParserState): Number {
        val start = state.pos
        if (state.peek() == '-') state.advance()
        while (!state.isExhausted() && state.peek().isDigit()) state.advance()
        var isDouble = false
        if (!state.isExhausted() && state.peek() == '.') {
            isDouble = true
            state.advance()
            while (!state.isExhausted() && state.peek().isDigit()) state.advance()
        }
        if (!state.isExhausted() && (state.peek() == 'e' || state.peek() == 'E')) {
            isDouble = true
            state.advance()
            if (!state.isExhausted() && (state.peek() == '+' || state.peek() == '-')) {
                state.advance()
            }
            while (!state.isExhausted() && state.peek().isDigit()) state.advance()
        }
        val text = state.input.substring(start, state.pos)
        return if (isDouble) text.toDouble() else text.toInt()
    }
}

private class ParserState(val input: String) {
    var pos: Int = 0

    fun isExhausted(): Boolean = pos >= input.length
    fun peek(): Char = input[pos]
    fun advance(): Char = input[pos++]

    fun expect(ch: Char) {
        skipWhitespace()
        check(!isExhausted() && input[pos] == ch) {
            "Expected '$ch' at position $pos but got '${if (isExhausted()) "EOF" else input[pos]}'"
        }
        pos++
    }

    fun skipWhitespace() {
        while (!isExhausted() && input[pos].isWhitespace()) pos++
    }

    fun startsWith(text: String): Boolean = input.startsWith(text, pos)

    fun skip(count: Int) {
        pos += count
    }

    fun take(count: Int): String {
        val result = input.substring(pos, pos + count)
        pos += count
        return result
    }

    fun remaining(): String = input.substring(pos)
}
