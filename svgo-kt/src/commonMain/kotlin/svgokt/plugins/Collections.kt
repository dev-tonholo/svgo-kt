package svgokt.plugins

private object ElementGroups {
    val animation = setOf(
        "animate",
        "animateColor",
        "animateMotion",
        "animateTransform",
        "set",
    )
    val descriptive = setOf("desc", "metadata", "title")
    val shape = setOf(
        "circle",
        "ellipse",
        "line",
        "path",
        "polygon",
        "polyline",
        "rect",
    )
    val structural = setOf("defs", "g", "svg", "symbol", "use")
    val paintServer = setOf(
        "hatch",
        "linearGradient",
        "meshGradient",
        "pattern",
        "radialGradient",
        "solidColor",
    )
    val nonRendering = setOf(
        "clipPath",
        "filter",
        "linearGradient",
        "marker",
        "mask",
        "pattern",
        "radialGradient",
        "solidColor",
        "symbol",
    )
    val container = setOf(
        "a",
        "defs",
        "foreignObject",
        "g",
        "marker",
        "mask",
        "missing-glyph",
        "pattern",
        "svg",
        "switch",
        "symbol",
    )
    val textContent = setOf(
        "altGlyph",
        "altGlyphDef",
        "altGlyphItem",
        "glyph",
        "glyphRef",
        "text",
        "textPath",
        "tref",
        "tspan",
    )
    val textContentChild = setOf("altGlyph", "textPath", "tref", "tspan")
    val lightSource = setOf(
        "feDiffuseLighting",
        "feDistantLight",
        "fePointLight",
        "feSpecularLighting",
        "feSpotLight",
    )
    val filterPrimitive = setOf(
        "feBlend",
        "feColorMatrix",
        "feComponentTransfer",
        "feComposite",
        "feConvolveMatrix",
        "feDiffuseLighting",
        "feDisplacementMap",
        "feDropShadow",
        "feFlood",
        "feFuncA",
        "feFuncB",
        "feFuncG",
        "feFuncR",
        "feGaussianBlur",
        "feImage",
        "feMerge",
        "feMergeNode",
        "feMorphology",
        "feOffset",
        "feSpecularLighting",
        "feTile",
        "feTurbulence",
    )
}

object Collections {
    /**
     * SVG shape elements that can have stroke and fill attributes.
     */
    val shapeElements: Set<String> = ElementGroups.shape

    /**
     * SVG non-rendering elements that are only used by reference.
     */
    val nonRenderingElements: Set<String> = ElementGroups.nonRendering

    /**
     * SVG container elements - elements that can contain child elements.
     * Empty container elements (with no children) are generally safe to remove.
     */
    val containerElements: Set<String> = ElementGroups.container

    /**
     * SVG animation elements.
     */
    val animationElements: Set<String> = ElementGroups.animation

    val textElements: Set<String> = buildSet {
        addAll(ElementGroups.textContent)
        add("pre")
        add("title")
    }

    /**
     * @see https://www.w3.org/TR/SVG11/linking.html#processingIRI
     */
    val referencesProps = setOf(
        "clip-path",
        "color-profile",
        "fill",
        "filter",
        "marker-end",
        "marker-mid",
        "marker-start",
        "mask",
        "stroke",
        "style",
    )

    /**
     * Namespaces used by SVG editors (Inkscape, Illustrator, Figma, Sketch, etc.).
     * Elements and attributes using these namespaces can be safely removed from
     * optimised SVG output.
     */
    val editorNamespaces = setOf(
        "http://creativecommons.org/ns#",
        "http://inkscape.sourceforge.net/DTD/sodipodi-0.dtd",
        "http://ns.adobe.com/AdobeIllustrator/10.0/",
        "http://ns.adobe.com/AdobeSVGViewerExtensions/3.0/",
        "http://ns.adobe.com/Extensibility/1.0/",
        "http://ns.adobe.com/Flows/1.0/",
        "http://ns.adobe.com/GenericCustomNamespace/1.0/",
        "http://ns.adobe.com/Graphs/1.0/",
        "http://ns.adobe.com/ImageReplacement/1.0/",
        "http://ns.adobe.com/SaveForWeb/1.0/",
        "http://ns.adobe.com/Variables/1.0/",
        "http://ns.adobe.com/xap/1.0/",
        "http://purl.org/dc/elements/1.1/",
        "http://schemas.microsoft.com/visio/2003/SVGExtensions/",
        "http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd",
        "http://taptapclick.com/",
        "http://www.bohemiancoding.com/sketch/ns",
        "http://www.figma.com/figma/ns",
        "http://www.inkscape.org/namespaces/inkscape",
        "http://www.serif.com/",
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        "http://www.w3.org/2000/01/rdf-schema#",
        "http://xmlns.com/foaf/0.1/",
        "http://www.vectornator.io/",
    )

    /**
     * All SVG presentation attributes as defined by the SVG specification.
     */
    val presentationAttrs = setOf(
        "alignment-baseline",
        "baseline-shift",
        "clip",
        "clip-path",
        "clip-rule",
        "color",
        "color-interpolation",
        "color-interpolation-filters",
        "color-profile",
        "cursor",
        "direction",
        "display",
        "dominant-baseline",
        "enable-background",
        "fill",
        "fill-opacity",
        "fill-rule",
        "filter",
        "flood-color",
        "flood-opacity",
        "font",
        "font-family",
        "font-size",
        "font-size-adjust",
        "font-stretch",
        "font-style",
        "font-variant",
        "font-weight",
        "glyph-orientation-horizontal",
        "glyph-orientation-vertical",
        "image-rendering",
        "kerning",
        "letter-spacing",
        "lighting-color",
        "marker",
        "marker-end",
        "marker-mid",
        "marker-start",
        "mask",
        "opacity",
        "overflow",
        "pointer-events",
        "shape-rendering",
        "stop-color",
        "stop-opacity",
        "stroke",
        "stroke-dasharray",
        "stroke-dashoffset",
        "stroke-linecap",
        "stroke-linejoin",
        "stroke-miterlimit",
        "stroke-opacity",
        "stroke-width",
        "text-anchor",
        "text-decoration",
        "text-rendering",
        "transform",
        "unicode-bidi",
        "vector-effect",
        "visibility",
        "word-spacing",
        "writing-mode",
    )

    /**
     * SVG presentation attributes that are inherited by child elements.
     */
    val inheritableAttrs = setOf(
        "clip-rule",
        "color",
        "color-interpolation",
        "color-interpolation-filters",
        "color-profile",
        "cursor",
        "direction",
        "dominant-baseline",
        "fill",
        "fill-opacity",
        "fill-rule",
        "font",
        "font-family",
        "font-size",
        "font-size-adjust",
        "font-stretch",
        "font-style",
        "font-variant",
        "font-weight",
        "glyph-orientation-horizontal",
        "glyph-orientation-vertical",
        "image-rendering",
        "kerning",
        "letter-spacing",
        "marker",
        "marker-end",
        "marker-mid",
        "marker-start",
        "paint-order",
        "pointer-events",
        "shape-rendering",
        "stroke",
        "stroke-dasharray",
        "stroke-dashoffset",
        "stroke-linecap",
        "stroke-linejoin",
        "stroke-miterlimit",
        "stroke-opacity",
        "stroke-width",
        "text-anchor",
        "text-decoration",
        "text-rendering",
        "visibility",
        "word-spacing",
        "writing-mode",
    )

    /**
     * Non-inheritable presentation attributes that are explicitly allowed on
     * `<g>` elements because they affect the group as a whole (e.g. opacity,
     * transform, clip-path).
     */
    val presentationNonInheritableGroupAttrs = setOf(
        "display",
        "clip-path",
        "filter",
        "mask",
        "opacity",
        "text-decoration",
        "transform",
        "unicode-bidi",
    )

    /**
     * SVG attributes that hold color values.
     * @see <a href="https://www.w3.org/TR/SVG11/single-page.html#types-DataTypeColor">SVG Color Data Type</a>
     */
    val colorsProps = setOf(
        "color",
        "fill",
        "flood-color",
        "lighting-color",
        "stop-color",
        "stroke",
    )

    /**
     * Map of CSS/SVG named colors to their hex equivalents.
     */
    @Suppress("LargeClass")
    val colorsNames: Map<String, String> = mapOf(
        "aliceblue" to "#f0f8ff",
        "antiquewhite" to "#faebd7",
        "aqua" to "#0ff",
        "aquamarine" to "#7fffd4",
        "azure" to "#f0ffff",
        "beige" to "#f5f5dc",
        "bisque" to "#ffe4c4",
        "black" to "#000",
        "blanchedalmond" to "#ffebcd",
        "blue" to "#00f",
        "blueviolet" to "#8a2be2",
        "brown" to "#a52a2a",
        "burlywood" to "#deb887",
        "cadetblue" to "#5f9ea0",
        "chartreuse" to "#7fff00",
        "chocolate" to "#d2691e",
        "coral" to "#ff7f50",
        "cornflowerblue" to "#6495ed",
        "cornsilk" to "#fff8dc",
        "crimson" to "#dc143c",
        "cyan" to "#0ff",
        "darkblue" to "#00008b",
        "darkcyan" to "#008b8b",
        "darkgoldenrod" to "#b8860b",
        "darkgray" to "#a9a9a9",
        "darkgreen" to "#006400",
        "darkgrey" to "#a9a9a9",
        "darkkhaki" to "#bdb76b",
        "darkmagenta" to "#8b008b",
        "darkolivegreen" to "#556b2f",
        "darkorange" to "#ff8c00",
        "darkorchid" to "#9932cc",
        "darkred" to "#8b0000",
        "darksalmon" to "#e9967a",
        "darkseagreen" to "#8fbc8f",
        "darkslateblue" to "#483d8b",
        "darkslategray" to "#2f4f4f",
        "darkslategrey" to "#2f4f4f",
        "darkturquoise" to "#00ced1",
        "darkviolet" to "#9400d3",
        "deeppink" to "#ff1493",
        "deepskyblue" to "#00bfff",
        "dimgray" to "#696969",
        "dimgrey" to "#696969",
        "dodgerblue" to "#1e90ff",
        "firebrick" to "#b22222",
        "floralwhite" to "#fffaf0",
        "forestgreen" to "#228b22",
        "fuchsia" to "#f0f",
        "gainsboro" to "#dcdcdc",
        "ghostwhite" to "#f8f8ff",
        "gold" to "#ffd700",
        "goldenrod" to "#daa520",
        "gray" to "#808080",
        "green" to "#008000",
        "greenyellow" to "#adff2f",
        "grey" to "#808080",
        "honeydew" to "#f0fff0",
        "hotpink" to "#ff69b4",
        "indianred" to "#cd5c5c",
        "indigo" to "#4b0082",
        "ivory" to "#fffff0",
        "khaki" to "#f0e68c",
        "lavender" to "#e6e6fa",
        "lavenderblush" to "#fff0f5",
        "lawngreen" to "#7cfc00",
        "lemonchiffon" to "#fffacd",
        "lightblue" to "#add8e6",
        "lightcoral" to "#f08080",
        "lightcyan" to "#e0ffff",
        "lightgoldenrodyellow" to "#fafad2",
        "lightgray" to "#d3d3d3",
        "lightgreen" to "#90ee90",
        "lightgrey" to "#d3d3d3",
        "lightpink" to "#ffb6c1",
        "lightsalmon" to "#ffa07a",
        "lightseagreen" to "#20b2aa",
        "lightskyblue" to "#87cefa",
        "lightslategray" to "#789",
        "lightslategrey" to "#789",
        "lightsteelblue" to "#b0c4de",
        "lightyellow" to "#ffffe0",
        "lime" to "#0f0",
        "limegreen" to "#32cd32",
        "linen" to "#faf0e6",
        "magenta" to "#f0f",
        "maroon" to "#800000",
        "mediumaquamarine" to "#66cdaa",
        "mediumblue" to "#0000cd",
        "mediumorchid" to "#ba55d3",
        "mediumpurple" to "#9370db",
        "mediumseagreen" to "#3cb371",
        "mediumslateblue" to "#7b68ee",
        "mediumspringgreen" to "#00fa9a",
        "mediumturquoise" to "#48d1cc",
        "mediumvioletred" to "#c71585",
        "midnightblue" to "#191970",
        "mintcream" to "#f5fffa",
        "mistyrose" to "#ffe4e1",
        "moccasin" to "#ffe4b5",
        "navajowhite" to "#ffdead",
        "navy" to "#000080",
        "oldlace" to "#fdf5e6",
        "olive" to "#808000",
        "olivedrab" to "#6b8e23",
        "orange" to "#ffa500",
        "orangered" to "#ff4500",
        "orchid" to "#da70d6",
        "palegoldenrod" to "#eee8aa",
        "palegreen" to "#98fb98",
        "paleturquoise" to "#afeeee",
        "palevioletred" to "#db7093",
        "papayawhip" to "#ffefd5",
        "peachpuff" to "#ffdab9",
        "peru" to "#cd853f",
        "pink" to "#ffc0cb",
        "plum" to "#dda0dd",
        "powderblue" to "#b0e0e6",
        "purple" to "#800080",
        "rebeccapurple" to "#639",
        "red" to "#f00",
        "rosybrown" to "#bc8f8f",
        "royalblue" to "#4169e1",
        "saddlebrown" to "#8b4513",
        "salmon" to "#fa8072",
        "sandybrown" to "#f4a460",
        "seagreen" to "#2e8b57",
        "seashell" to "#fff5ee",
        "sienna" to "#a0522d",
        "silver" to "#c0c0c0",
        "skyblue" to "#87ceeb",
        "slateblue" to "#6a5acd",
        "slategray" to "#708090",
        "slategrey" to "#708090",
        "snow" to "#fffafa",
        "springgreen" to "#00ff7f",
        "steelblue" to "#4682b4",
        "tan" to "#d2b48c",
        "teal" to "#008080",
        "thistle" to "#d8bfd8",
        "tomato" to "#ff6347",
        "turquoise" to "#40e0d0",
        "violet" to "#ee82ee",
        "wheat" to "#f5deb3",
        "white" to "#fff",
        "whitesmoke" to "#f5f5f5",
        "yellow" to "#ff0",
        "yellowgreen" to "#9acd32",
    )

    /**
     * Map of hex colors to shorter named equivalents.
     */
    val colorsShortNames: Map<String, String> = mapOf(
        "#f0ffff" to "azure",
        "#f5f5dc" to "beige",
        "#ffe4c4" to "bisque",
        "#a52a2a" to "brown",
        "#ff7f50" to "coral",
        "#ffd700" to "gold",
        "#808080" to "gray",
        "#008000" to "green",
        "#4b0082" to "indigo",
        "#fffff0" to "ivory",
        "#f0e68c" to "khaki",
        "#faf0e6" to "linen",
        "#800000" to "maroon",
        "#000080" to "navy",
        "#808000" to "olive",
        "#ffa500" to "orange",
        "#da70d6" to "orchid",
        "#cd853f" to "peru",
        "#ffc0cb" to "pink",
        "#dda0dd" to "plum",
        "#800080" to "purple",
        "#f00" to "red",
        "#ff0000" to "red",
        "#fa8072" to "salmon",
        "#a0522d" to "sienna",
        "#c0c0c0" to "silver",
        "#fffafa" to "snow",
        "#d2b48c" to "tan",
        "#008080" to "teal",
        "#ff6347" to "tomato",
        "#ee82ee" to "violet",
        "#f5deb3" to "wheat",
    )
}
