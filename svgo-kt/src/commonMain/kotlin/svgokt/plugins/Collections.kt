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
}
