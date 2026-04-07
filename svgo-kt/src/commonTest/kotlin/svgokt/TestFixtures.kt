package svgokt

object TestFixtures {
    // language=svg
    val PARSER_TEST_SVG = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<!-- Generator: Adobe Illustrator 15.0.0, SVG Export Plug-In -->
        |<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
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
}
