package svgokt.plugins.builtin

import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.plugins.NoPluginParam
import svgokt.domain.plugins.Plugin
import svgokt.domain.plugins.PluginFn
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode
import svgokt.plugins.xast.visit

private const val ATTR_ENABLE_BACKGROUND = "enable-background"
private const val ELEMENT_FILTER = "filter"

/**
 * Regex matching `new 0 0 <width> <height>` enable-background values.
 * Captures the width (group 1) and height (group 3).
 */
private val regEnableBackground = Regex(
    """^new\s0\s0\s([-+]?\d*\.?\d+([eE][-+]?\d+)?)\s([-+]?\d*\.?\d+([eE][-+]?\d+)?)$"""
)

private val CLEANUP_ELEMENTS = setOf("svg", "mask", "pattern")

object CleanupEnableBackground : Plugin<NoPluginParam> {
    override val name: String = "cleanupEnableBackground"
    override val description: String =
        "remove or cleanup enable-background attribute when possible"
    override val params: NoPluginParam = NoPluginParam
    override val fn: PluginFn = fn@{ root, _, _ ->
        // First pass: check if any <filter> elements exist in the document
        var hasFilter = false
        root.visit(
            Visitor(
                element = VisitorNode(
                    onEnter = { node, _ ->
                        if (node.name == ELEMENT_FILTER) {
                            hasFilter = true
                        }
                        VisitState.Continue
                    },
                ),
            ),
        )

        Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    processElement(node, parentNode, hasFilter)
                },
            ),
        )
    }

    @Suppress("UnusedParameter")
    private fun processElement(
        node: XastElement,
        parentNode: XastParent?,
        hasFilter: Boolean,
    ): VisitState {
        // Handle enable-background in the style attribute
        val styleValue = node.attributes["style"]
        val styleResult = if (styleValue != null) {
            processStyleAttribute(styleValue, hasFilter, node)
        } else {
            null
        }

        if (!hasFilter) {
            // No filters: safe to remove all enable-background attributes
            node.attributes.remove(ATTR_ENABLE_BACKGROUND)
            applyStyleResult(node, styleResult)
            return VisitState.Continue
        }

        // Filters exist: only clean up on svg/mask/pattern with matching dimensions
        val hasDimensions = node.attributes["width"] != null &&
            node.attributes["height"] != null

        if (node.name in CLEANUP_ELEMENTS && hasDimensions) {
            val attrValue = node.attributes[ATTR_ENABLE_BACKGROUND]
            if (attrValue != null) {
                val width = node.attributes.getValue("width")
                val height = node.attributes.getValue("height")
                val cleaned = cleanupValue(
                    value = attrValue,
                    nodeName = node.name,
                    width = width,
                    height = height,
                )
                if (cleaned != null) {
                    node.attributes[ATTR_ENABLE_BACKGROUND] = cleaned
                } else {
                    node.attributes.remove(ATTR_ENABLE_BACKGROUND)
                }
            }
        }

        applyStyleResult(node, styleResult)
        return VisitState.Continue
    }

    /**
     * Process the enable-background declaration within a style attribute.
     * Returns the updated style string, or null if the style should be removed.
     */
    private fun processStyleAttribute(
        styleValue: String,
        hasFilter: Boolean,
        node: XastElement,
    ): StyleResult {
        // Simple CSS declaration parsing for enable-background
        val declarations = parseSimpleDeclarations(styleValue)
        val enableBgDecls = declarations.filter { it.property == ATTR_ENABLE_BACKGROUND }

        if (enableBgDecls.isEmpty()) {
            return StyleResult.Unchanged
        }

        // Keep only the last enable-background declaration (CSS cascade)
        val lastEnableBg = enableBgDecls.last()
        val otherDecls = declarations.filter { it.property != ATTR_ENABLE_BACKGROUND }

        if (!hasFilter) {
            // Remove all enable-background declarations
            return if (otherDecls.isEmpty()) {
                StyleResult.Remove
            } else {
                StyleResult.Update(
                    otherDecls.joinToString(separator = ";") { "${it.property}:${it.value}" },
                )
            }
        }

        // When filter exists, try to clean up the value
        val hasDimensions = node.attributes["width"] != null &&
            node.attributes["height"] != null

        if (node.name in CLEANUP_ELEMENTS && hasDimensions) {
            val width = node.attributes.getValue("width")
            val height = node.attributes.getValue("height")
            val cleaned = cleanupValue(
                value = lastEnableBg.value,
                nodeName = node.name,
                width = width,
                height = height,
            )
            val remaining = if (cleaned != null) {
                otherDecls + CssDeclaration(ATTR_ENABLE_BACKGROUND, cleaned)
            } else {
                otherDecls
            }
            return if (remaining.isEmpty()) {
                StyleResult.Remove
            } else {
                StyleResult.Update(
                    remaining.joinToString(separator = ";") { "${it.property}:${it.value}" },
                )
            }
        }

        return StyleResult.Unchanged
    }

    private fun applyStyleResult(node: XastElement, result: StyleResult?) {
        when (result) {
            is StyleResult.Remove -> node.attributes.remove("style")
            is StyleResult.Update -> node.attributes["style"] = result.value
            StyleResult.Unchanged, null -> Unit
        }
    }

    /**
     * Checks if the enable-background value matches the element dimensions
     * and returns the cleaned up value.
     *
     * Returns null to indicate the attribute should be removed.
     */
    private fun cleanupValue(
        value: String,
        nodeName: String,
        width: String,
        height: String,
    ): String? {
        val match = regEnableBackground.matchEntire(value) ?: return value
        if (width == match.groupValues[1] && height == match.groupValues[3]) {
            return if (nodeName == "svg") null else "new"
        }
        return value
    }

    private data class CssDeclaration(val property: String, val value: String)

    private sealed interface StyleResult {
        data object Unchanged : StyleResult
        data object Remove : StyleResult
        data class Update(val value: String) : StyleResult
    }

    /**
     * Simple CSS declaration list parser.
     * Handles basic "property:value" pairs separated by semicolons.
     */
    private fun parseSimpleDeclarations(style: String): List<CssDeclaration> {
        val declarations = mutableListOf<CssDeclaration>()
        for (part in style.split(";")) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue
            val colonIndex = trimmed.indexOf(':')
            if (colonIndex < 0) continue
            val property = trimmed.substring(startIndex = 0, endIndex = colonIndex).trim()
            val value = trimmed.substring(startIndex = colonIndex + 1).trim()
            declarations.add(CssDeclaration(property = property, value = value))
        }
        return declarations
    }
}
