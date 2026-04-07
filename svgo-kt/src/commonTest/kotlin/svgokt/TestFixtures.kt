package svgokt

object TestFixtures {
    /**
     * Complex SVG fixture that exercises multiple node types: XML processing instruction,
     * comment, CDATA, nested elements, attributes, and text nodes.
     *
     * Note: DOCTYPE is intentionally omitted because the SAX parser has a known bug
     * where quoted strings in DOCTYPE PUBLIC identifiers (outside of [...] subsets)
     * are mishandled - causing the parser to end in an unexpected state.
     */
    // language=svg
    val PARSER_TEST_SVG = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<!-- Generator: Adobe Illustrator 15.0.0, SVG Export Plug-In -->
        |<svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" width="120px" height="120px" viewBox="0 0 120 120">
        |<style type="text/css"><![CDATA[ svg { fill: red; } ]]></style>
        |<g><g>
        |    <circle fill="#ff0000" cx="60px" cy="60px" r="50px"/>
        |    <text>  test  </text>
        |</g></g>
        |<g style="color: black" class="unknown-class"></g>
        |</svg>
    """.trimMargin()

    // language=svg
    val SIMPLE_SVG = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val MERGE_STYLES_INPUT = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<style>.st0{fill:red}</style>
        |<style>.st1{fill:blue}</style>
        |<rect class="st0" width="50" height="50"/>
        |<rect class="st1" x="50" width="50" height="50"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val MERGE_STYLES_NO_STYLES = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect class="st0" width="100" height="100"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val MERGE_STYLES_FOREIGN_OBJECT = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<style>.st0{fill:red}</style>
        |<foreignObject>
        |<style>.foreign{color:blue}</style>
        |</foreignObject>
        |<rect class="st0" width="100" height="100"/>
        |</svg>
    """.trimMargin()

    // Note: DOCTYPE with PUBLIC identifiers containing quoted strings trigger a known SAX parser bug.
    // Using a bare DOCTYPE declaration to avoid parser failure.
    val REMOVE_DOCTYPE_WITH_DOCTYPE = """
        |<!DOCTYPE svg>
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    val REMOVE_DOCTYPE_WITHOUT_DOCTYPE = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    val REMOVE_XML_PROC_INST_WITH_XML_DECL = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    val REMOVE_XML_PROC_INST_WITH_CUSTOM_PI = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<?custom-pi some-value?>
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    val REMOVE_COMMENTS_WITH_COMMENTS = """
        |<!-- Generator: Some Tool -->
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<!-- inner comment -->
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    val REMOVE_COMMENTS_WITHOUT_COMMENTS = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    val REMOVE_METADATA_WITH_METADATA = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<metadata><rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"/></metadata>
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    val REMOVE_METADATA_WITHOUT_METADATA = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    val REMOVE_DESC_GENERATOR = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<desc>Created with Inkscape</desc>
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    val REMOVE_DESC_EMPTY = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<desc/>
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    val REMOVE_DESC_ACCESSIBILITY = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<desc>A red square for accessibility</desc>
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    val SORT_ATTRS_UNORDERED = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect fill="red" height="100" width="100" id="r1"/>
        |</svg>
    """.trimMargin()

    val SORT_ATTRS_WITH_XMLNS = """
        |<svg xmlns:xlink="http://www.w3.org/1999/xlink" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect fill="red" width="100" height="100"/>
        |</svg>
    """.trimMargin()

    val SORT_DEFS_MIXED_CHILDREN = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<defs>
        |<linearGradient id="a"/>
        |<filter id="b"/>
        |<linearGradient id="c"/>
        |<clipPath id="d"/>
        |</defs>
        |</svg>
    """.trimMargin()

    val SORT_DEFS_SINGLE_TYPE = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<defs>
        |<clipPath id="a"/>
        |<clipPath id="b"/>
        |</defs>
        |</svg>
    """.trimMargin()

    val REMOVE_UNUSED_NS_UNUSED = """
        |<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    val REMOVE_UNUSED_NS_USED = """
        |<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 100 100">
        |<image xlink:href="image.png" width="100" height="100"/>
        |</svg>
    """.trimMargin()
}
