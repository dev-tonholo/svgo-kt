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
        |<svg xmlns="http://www.w3.org/2000/svg" xmlns:custom="http://example.com/custom" viewBox="0 0 100 100">
        |<custom:shape width="100" height="100"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val REMOVE_TITLE_WITH_TITLE = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<title>My SVG</title>
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val REMOVE_TITLE_WITHOUT_TITLE = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val REMOVE_VIEWBOX_MATCHING = """
        |<svg xmlns="http://www.w3.org/2000/svg" width="100" height="50" viewBox="0 0 100 50">
        |<rect width="100" height="50" fill="red"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val REMOVE_VIEWBOX_NON_MATCHING = """
        |<svg xmlns="http://www.w3.org/2000/svg" width="100" height="50" viewBox="0 0 200 100">
        |<rect width="100" height="50" fill="red"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val REMOVE_XMLNS_WITH_XMLNS = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val REMOVE_STYLE_ELEMENT_WITH_STYLE = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<style>.st0{fill:red}</style>
        |<rect class="st0" width="100" height="100"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val REMOVE_SCRIPTS_WITH_SCRIPT = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<script>alert('hello')</script>
        |<rect width="100" height="100" fill="red" onclick="doSomething()"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val REMOVE_RASTER_IMAGES_WITH_PNG = """
        |<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 100 100">
        |<image xlink:href="data:image/png;base64,abc123"/>
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val REMOVE_RASTER_IMAGES_SVG_ONLY = """
        |<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 100 100">
        |<image xlink:href="data:image/svg+xml;base64,abc123"/>
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val REMOVE_ATTRS_WITH_FILL = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red" stroke="blue"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val ADD_ATTRS_SVG = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    // -- AddClassesToSVGElement --

    // language=svg
    val ADD_CLASSES_SIMPLE = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val ADD_CLASSES_EXISTING = """
        |<svg xmlns="http://www.w3.org/2000/svg" class="existing" viewBox="0 0 100 100">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    // -- RemoveAttributesBySelector --

    // language=svg
    val REMOVE_ATTRS_BY_SELECTOR = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect x="0" y="0" width="100" height="100" fill="#00ff00" stroke="#00ff00"/>
        |</svg>
    """.trimMargin()

    // -- PrefixIds --

    // language=svg
    val PREFIX_IDS_SIMPLE = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<defs><linearGradient id="grad1"/></defs>
        |<rect id="myRect" fill="url(#grad1)" width="100" height="100"/>
        |</svg>
    """.trimMargin()

    // -- RemoveOffCanvasPaths --

    // language=svg
    val REMOVE_OFF_CANVAS_OUTSIDE = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<path d="M200 200 L300 300"/>
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val REMOVE_OFF_CANVAS_INSIDE = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<path d="M50 50 L80 80"/>
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    // -- ConvertStyleToAttrs --

    // language=svg
    val CONVERT_STYLE_TO_ATTRS = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<g style="fill:#000;stroke:blue">
        |<rect width="100" height="100"/>
        |</g>
        |</svg>
    """.trimMargin()

    // language=svg
    val CONVERT_STYLE_TO_ATTRS_NO_OVERRIDE = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<rect style="fill:red;stroke:blue" fill="green" width="100" height="100"/>
        |</svg>
    """.trimMargin()

    // -- CleanupListOfValues --

    // language=svg
    val CLEANUP_LIST_VALUES_VIEWBOX = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200.28423 200.28423">
        |<rect width="100" height="100" fill="red"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val CLEANUP_LIST_VALUES_POINTS = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<polygon points="208.250977 77.1308594 223.069336 95.0800781"/>
        |</svg>
    """.trimMargin()

    // -- ReusePaths --

    // language=svg
    val REUSE_PATHS_DUPLICATES = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<path d="M0 0L10 10" fill="red"/>
        |<path d="M0 0L10 10" fill="red"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val REUSE_PATHS_UNIQUE = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<path d="M0 0L10 10" fill="red"/>
        |<path d="M20 20L30 30" fill="blue"/>
        |</svg>
    """.trimMargin()

    // -- ConvertOneStopGradients --

    // language=svg
    val ONE_STOP_GRADIENT = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<defs><linearGradient id="g1"><stop stop-color="red"/></linearGradient></defs>
        |<rect fill="url(#g1)" width="100" height="100"/>
        |</svg>
    """.trimMargin()

    // language=svg
    val TWO_STOP_GRADIENT = """
        |<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        |<defs><linearGradient id="g1"><stop stop-color="red"/><stop stop-color="blue"/></linearGradient></defs>
        |<rect fill="url(#g1)" width="100" height="100"/>
        |</svg>
    """.trimMargin()
}
