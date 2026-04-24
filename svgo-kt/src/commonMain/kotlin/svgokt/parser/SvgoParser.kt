package svgokt.parser

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import saxkt.SaxParser
import saxkt.domain.SaxEvent
import saxkt.domain.options
import svgokt.SvgoParserException
import svgokt.domain.XastCdata
import svgokt.domain.XastChild
import svgokt.domain.XastComment
import svgokt.domain.XastDoctype
import svgokt.domain.XastElement
import svgokt.domain.XastElementType
import svgokt.domain.XastInstruction
import svgokt.domain.XastParent
import svgokt.domain.XastRoot
import svgokt.domain.XastText
import svgokt.plugins.Collections

private val entityDeclaration = """<!ENTITY\s+(\S+)\s+(?:'([^']+)'|"([^"]+)")\s*>""".toRegex()

/** Regex capture group indices for [entityDeclaration]. */
private const val ENTITY_NAME_GROUP = 1
private const val ENTITY_SINGLE_QUOTE_VALUE_GROUP = 2
private const val ENTITY_DOUBLE_QUOTE_VALUE_GROUP = 3

sealed interface ParsingState {
    data object Parsing : ParsingState
    data object Parsed : ParsingState
}

private object EndParseStateCollectionException :
    CancellationException("Parser has finished. Cancelling flow collection")

@Suppress("TooManyFunctions")
class SvgoParser(
    // Create a platform expect dispatcher implementation in the future.
    private val coroutineContext: CoroutineDispatcher = Dispatchers.Default,
) {
    private val options = options {
        strict = true
        // Use trim=false to match the JS svgo SAX parser behavior.
        // Text trimming for non-text elements is handled in handleText().
        trim = false
        normalize = false
        lowercase = false
        xmlns = true
        position = true
    }
    private var parserScope: CoroutineScope = createParserScope()
    private var sax = SaxParser(strict = options.strict, options = options)
    private var root = XastRoot(type = XastElementType.ROOT, children = mutableListOf())
    private var current: XastParent = root
    private val stack: MutableList<XastParent> = mutableListOf(root)
    private var parsingState: ParsingState = ParsingState.Parsing

    private fun createParserScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + coroutineContext)

    private fun pushToContent(node: XastChild) {
        current.children += node
    }

    /**
     * Resets all mutable state so the parser can be reused across multiple
     * [parseSvg] calls. The underlying [SaxParser] latches a `closed` flag
     * after each parse, so a fresh instance is created here as well.
     */
    private fun resetState() {
        sax = SaxParser(strict = options.strict, options = options)
        root = XastRoot(type = XastElementType.ROOT, children = mutableListOf())
        current = root
        stack.clear()
        stack += root
        parsingState = ParsingState.Parsing
    }

    /**
     * Convert SVG (XML) string to SVG-as-Object.
     * @param data the SVG content
     * @param from the file path
     */
    suspend fun parseSvg(data: String, from: String?): XastRoot {
        resetState()
        if (parserScope.isActive.not()) {
            println("W: Attempted to parse without an active coroutine scope. Creating a new one.")
            parserScope = createParserScope()
        }
        // As JS target doesn't have multiple threads, we need to run both the state collector
        // and the writer procedure by using async.
        //
        // Start = UNDISPATCHED so the collector subscribes to `sax.events` (a
        // MutableSharedFlow with replay=0 and no buffer) before the writer
        // begins emitting. Without this, early `emit` calls complete with no
        // subscriber and events are silently dropped, surfacing later as a
        // missing root element or an unbalanced closeTag on an empty stack.
        // The SharedFlow contract guarantees the subscription is registered
        // by the time `.collect { ... }` first suspends.
        val stateCollector = parserScope.async(start = CoroutineStart.UNDISPATCHED) {
            sax.events
                .collect { event ->
                    when (event) {
                        is SaxEvent.Doctype -> handleDoctype(event, data)

                        is SaxEvent.ProcessingInstruction -> handleProcessingInstruction(event)

                        is SaxEvent.Comment -> handleComment(event)

                        is SaxEvent.Cdata -> handleCdata(event)

                        is SaxEvent.OpenTag -> handleOpenTag(event)

                        is SaxEvent.Text -> handleText(event)

                        is SaxEvent.CloseTag -> handleCloseTag()

                        is SaxEvent.Error -> handleError(event, data, from)

                        is SaxEvent.End -> {
                            parsingState = ParsingState.Parsed
                            parserScope.cancel(EndParseStateCollectionException)
                        }

                        else -> Unit
                    }
                }
        }

        val writer = parserScope.async {
            sax.write(data).close()
        }

        try {
            awaitAll(writer, stateCollector)
        } catch (expected: EndParseStateCollectionException) {
            // Expected: parsing completed successfully.
        }

        return root
    }

    private fun handleError(event: SaxEvent.Error, data: String, from: String?) {
        val error = SvgoParserException(
            message = event.error.reason,
            line = (event.error.positionTracker?.line ?: 0) + 1,
            column = (event.error.positionTracker?.column ?: 0),
            source = data,
            file = from.orEmpty(),
        )
        if (error.message.contains("Unexpected end")) {
            throw error
        } else {
            println(error)
        }
    }

    private fun handleCloseTag() {
        stack.removeLast()
        stack.lastOrNull()?.let { current = it }
    }

    private fun handleText(event: SaxEvent.Text) {
        (current as? XastElement)?.let { current ->
            // prevent trimming of meaningful whitespace inside textual tags
            if (Collections.textElements.contains(current.name)) {
                val node = XastText(
                    value = event.textNode,
                    // parentNode = current,
                )
                pushToContent(node)
            } else if ("\\S".toRegex().containsMatchIn(event.textNode)) {
                val node = XastText(
                    value = event.textNode.trim(),
                    // parentNode = current,
                )
                pushToContent(node)
            }
        }
    }

    private fun handleOpenTag(event: SaxEvent.OpenTag) {
        val element = XastElement(
            // parentNode = parentNode,
            name = event.tag.name,
            attributes = event.tag.attributes
                .mapValues { (_, value) -> value.value.toString() }
                .toMutableMap(),
            children = mutableListOf(),
        )
        pushToContent(element)
        current = element
        stack += element
    }

    private fun handleCdata(event: SaxEvent.Cdata) {
        val node = XastCdata(
            value = event.value,
            // parentNode = parentNode,
        )
        pushToContent(node)
    }

    private fun handleComment(event: SaxEvent.Comment) {
        val node = XastComment(
            value = event.comment.trim(),
            // parentNode = parentNode,
        )
        pushToContent(node)
    }

    private fun handleProcessingInstruction(event: SaxEvent.ProcessingInstruction) {
        val node = XastInstruction(
            name = event.name,
            value = event.body,
            // parentNode = parentNode,
        )
        pushToContent(node)
    }

    private fun handleDoctype(event: SaxEvent.Doctype, data: String) {
        val doctype = event.data
        val node = XastDoctype(
            // parentNode = parentNode,
            name = "svg",
            data = XastDoctype.XastDoctypeData(doctype)
        )
        pushToContent(node)
        val subsetStart = doctype.indexOf("[")
        if (subsetStart >= 0) {
            var entityMatch = entityDeclaration.find(input = data, startIndex = subsetStart)
            while (entityMatch != null) {
                val groups = entityMatch.groupValues
                sax.addToEntity(
                    key = groups[ENTITY_NAME_GROUP],
                    value = groups[ENTITY_SINGLE_QUOTE_VALUE_GROUP]
                        .ifEmpty { groups[ENTITY_DOUBLE_QUOTE_VALUE_GROUP] },
                )
                entityMatch = entityMatch.next()
            }
        }
    }
}
