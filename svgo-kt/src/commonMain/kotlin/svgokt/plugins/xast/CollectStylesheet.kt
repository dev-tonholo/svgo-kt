package svgokt.plugins.xast

import svgokt.domain.XastCdata
import svgokt.domain.XastElement
import svgokt.domain.XastParent
import svgokt.domain.XastRoot
import svgokt.domain.XastText
import svgokt.domain.css.Specificity
import svgokt.domain.css.Stylesheet
import svgokt.domain.css.StylesheetDeclaration
import svgokt.domain.css.StylesheetRule
import svgokt.domain.plugins.VisitState
import svgokt.domain.plugins.Visitor
import svgokt.domain.plugins.VisitorNode

private const val SPECIFICITY_COMPONENTS = 3

fun collectStylesheet(root: XastRoot): Stylesheet {
    val rules = mutableListOf<StylesheetRule>()
    val parents = mutableMapOf<XastElement, XastParent>()
    root.visit(
        visitor = Visitor(
            element = VisitorNode(
                onEnter = { node, parentNode ->
                    parentNode?.let { parent -> parents += node to parent }
                    if (node.name != "style") return@VisitorNode VisitState.Continue
                    val type = node.attributes["type"]
                    if (type == null || type == "" || type == "text/css") {
                        val media = node.attributes["media"]
                        val dynamic = media != null && media != "all"
                        for (child in node.children) {
                            val value = when (child) {
                                is XastText -> child.value
                                is XastCdata -> child.value
                                else -> null
                            }
                            value?.let { rules.addAll(parseStylesheet(css = value, dynamic)) }
                        }
                    }
                    VisitState.Continue
                }
            )
        )
    )
    rules.sortWith { a, b -> compareSpecificity(a.specificity, b.specificity) }
    return Stylesheet(rules, parents)
}

@Suppress("ReturnCount")
private fun compareSpecificity(a: Specificity, b: Specificity): Int {
    for (i in 0 until SPECIFICITY_COMPONENTS) {
        if (a[i] < b[i]) return -1
        else if (a[i] > b[i]) return 1
    }
    return 0
}

private val COMMENT_REGEX = Regex("/\\*[\\s\\S]*?\\*/")
private val KF = setOf("keyframes", "-webkit-keyframes", "-o-keyframes", "-moz-keyframes")

@Suppress("LoopWithTooManyJumpStatements")
private fun parseStylesheet(css: String, dynamic: Boolean): List<StylesheetRule> {
    val rules = mutableListOf<StylesheetRule>()
    val cleaned = css.replace(COMMENT_REGEX, "")
    var i = 0
    while (i < cleaned.length) {
        while (i < cleaned.length && cleaned[i].isWhitespace()) i++
        if (i >= cleaned.length) break
        if (cleaned[i] == '@') {
            i = parseAtRule(cleaned, i, dynamic, rules)
            continue
        }
        val braceIdx = cleaned.indexOf('{', startIndex = i)
        if (braceIdx < 0) break
        val selectorText = cleaned.substring(startIndex = i, endIndex = braceIdx).trim()
        val closeIdx = cleaned.indexOf('}', startIndex = braceIdx + 1)
        if (closeIdx < 0) break
        val declText = cleaned.substring(startIndex = braceIdx + 1, endIndex = closeIdx).trim()
        if (selectorText.isNotEmpty()) parseRuleSelectors(selectorText, declText, dynamic, rules)
        i = closeIdx + 1
    }
    return rules
}

@Suppress("LoopWithTooManyJumpStatements")
private fun parseAtRule(css: String, start: Int, dynamic: Boolean, rules: MutableList<StylesheetRule>): Int {
    val braceIdx = css.indexOf('{', startIndex = start)
    val semiIdx = css.indexOf(';', startIndex = start)
    if (semiIdx >= 0 && (braceIdx < 0 || semiIdx < braceIdx)) return semiIdx + 1
    if (braceIdx < 0) return css.length
    val prelude = css.substring(startIndex = start, endIndex = braceIdx).trim()
    val atName = prelude.removePrefix("@").substringBefore(' ').substringBefore('{').trim()
    var depth = 1
    var j = braceIdx + 1
    while (j < css.length && depth > 0) { if (css[j] == '{') depth++; if (css[j] == '}') depth--; j++ }
    if (atName in KF) return j
    val inner = css.substring(startIndex = braceIdx + 1, endIndex = j - 1)
    rules.addAll(parseStylesheet(css = inner, dynamic = true))
    return j
}

private fun parseRuleSelectors(selText: String, declText: String, dynamic: Boolean, rules: MutableList<StylesheetRule>) {
    val decls = declText.split(';').mapNotNull { part ->
        val t = part.trim(); if (t.isEmpty()) return@mapNotNull null
        val ci = t.indexOf(':'); if (ci <= 0) return@mapNotNull null
        val n = t.substring(startIndex = 0, endIndex = ci).trim()
        var v = t.substring(startIndex = ci + 1).trim()
        val imp = v.contains("!important"); if (imp) v = v.replace("!important", "").trim()
        StylesheetDeclaration(name = n, value = v, important = imp)
    }
    if (decls.isEmpty()) return
    for (sel in selText.split(',')) {
        val trimmed = sel.trim(); if (trimmed.isEmpty()) continue
        var hasPseudo = false
        val matchSel = trimmed.replace(Regex("::?[a-zA-Z-]+(?:\\([^)]*\\))?")) { m ->
            if (!m.value.startsWith("::")) hasPseudo = true; ""
        }.trim()
        rules.add(StylesheetRule(dynamic = hasPseudo || dynamic, selector = matchSel.ifEmpty { trimmed },
            specificity = calcSpec(trimmed), declarations = decls))
    }
}

@Suppress("MagicNumber")
private fun calcSpec(sel: String): Specificity {
    var a = 0; var b = 0; var c = 0
    a += Regex("#[a-zA-Z_][a-zA-Z0-9_-]*").findAll(sel).count()
    b += Regex("\\.[a-zA-Z_][a-zA-Z0-9_-]*").findAll(sel).count()
    b += Regex("\\[[^\\]]+\\]").findAll(sel).count()
    b += Regex(":(?!:)[a-zA-Z][a-zA-Z0-9-]*").findAll(sel).count()
    c += Regex("::[a-zA-Z][a-zA-Z0-9-]*").findAll(sel).count()
    val s = sel.replace(Regex("#[a-zA-Z_][a-zA-Z0-9_-]*"), "").replace(Regex("\\.[a-zA-Z_][a-zA-Z0-9_-]*"), "")
        .replace(Regex("\\[[^\\]]+\\]"), "").replace(Regex("::?[a-zA-Z][a-zA-Z0-9-]*(?:\\([^)]*\\))?"), "")
    c += Regex("(?:^|[\\s>+~])([a-zA-Z][a-zA-Z0-9]*)").findAll(s).count()
    return Specificity(a = a, b = b, c = c)
}
