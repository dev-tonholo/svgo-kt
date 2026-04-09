@file:Suppress("LargeClass", "MaxLineLength")

package svgokt.plugins

/**
 * Per-element SVG specification data ported from svgo's _collections.js.
 *
 * Each entry describes the attribute groups, individual attributes, default
 * values, deprecated attributes, allowed content groups, and allowed content
 * elements for a single SVG element.
 */
data class SvgElementConfig(
    val attrsGroups: Set<String>,
    val attrs: Set<String> = emptySet(),
    val defaults: Map<String, String> = emptyMap(),
    val deprecated: DeprecatedAttrs? = null,
    val contentGroups: Set<String> = emptySet(),
    val content: Set<String> = emptySet(),
)

data class DeprecatedAttrs(
    val safe: Set<String> = emptySet(),
    val unsafe: Set<String> = emptySet(),
)

/**
 * Attribute group definitions.
 * Maps group names to the set of attribute names belonging to that group.
 */
object AttrsGroups {
    val animationAddition = setOf("additive", "accumulate")
    val animationAttributeTarget = setOf("attributeType", "attributeName")
    val animationEvent = setOf("onbegin", "onend", "onrepeat", "onload")
    val animationTiming = setOf(
        "begin", "dur", "end", "fill", "max", "min",
        "repeatCount", "repeatDur", "restart",
    )
    val animationValue = setOf(
        "by", "calcMode", "from", "keySplines", "keyTimes", "to", "values",
    )
    val conditionalProcessing = setOf(
        "requiredExtensions", "requiredFeatures", "systemLanguage",
    )
    val core = setOf("id", "tabindex", "xml:base", "xml:lang", "xml:space")
    val graphicalEvent = setOf(
        "onactivate", "onclick", "onfocusin", "onfocusout", "onload",
        "onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup",
    )
    val presentation = setOf(
        "alignment-baseline", "baseline-shift", "clip-path", "clip-rule", "clip",
        "color-interpolation-filters", "color-interpolation", "color-profile",
        "color-rendering", "color", "cursor", "direction", "display",
        "dominant-baseline", "enable-background", "fill-opacity", "fill-rule",
        "fill", "filter", "flood-color", "flood-opacity", "font-family",
        "font-size-adjust", "font-size", "font-stretch", "font-style",
        "font-variant", "font-weight", "glyph-orientation-horizontal",
        "glyph-orientation-vertical", "image-rendering", "letter-spacing",
        "lighting-color", "marker-end", "marker-mid", "marker-start", "mask",
        "opacity", "overflow", "paint-order", "pointer-events", "shape-rendering",
        "stop-color", "stop-opacity", "stroke-dasharray", "stroke-dashoffset",
        "stroke-linecap", "stroke-linejoin", "stroke-miterlimit", "stroke-opacity",
        "stroke-width", "stroke", "text-anchor", "text-decoration", "text-overflow",
        "text-rendering", "transform-origin", "transform", "unicode-bidi",
        "vector-effect", "visibility", "word-spacing", "writing-mode",
    )
    val xlink = setOf(
        "xlink:actuate", "xlink:arcrole", "xlink:href", "xlink:role",
        "xlink:show", "xlink:title", "xlink:type",
    )
    val documentEvent = setOf(
        "onabort", "onerror", "onresize", "onscroll", "onunload", "onzoom",
    )
    val documentElementEvent = setOf("oncopy", "oncut", "onpaste")
    val globalEvent = setOf(
        "oncancel", "oncanplay", "oncanplaythrough", "onchange", "onclick",
        "onclose", "oncuechange", "ondblclick", "ondrag", "ondragend",
        "ondragenter", "ondragleave", "ondragover", "ondragstart", "ondrop",
        "ondurationchange", "onemptied", "onended", "onerror", "onfocus",
        "oninput", "oninvalid", "onkeydown", "onkeypress", "onkeyup", "onload",
        "onloadeddata", "onloadedmetadata", "onloadstart", "onmousedown",
        "onmouseenter", "onmouseleave", "onmousemove", "onmouseout",
        "onmouseover", "onmouseup", "onmousewheel", "onpause", "onplay",
        "onplaying", "onprogress", "onratechange", "onreset", "onresize",
        "onscroll", "onseeked", "onseeking", "onselect", "onshow", "onstalled",
        "onsubmit", "onsuspend", "ontimeupdate", "ontoggle", "onvolumechange",
        "onwaiting",
    )
    val filterPrimitive = setOf("x", "y", "width", "height", "result")
    val transferFunction = setOf(
        "amplitude", "exponent", "intercept", "offset", "slope", "tableValues", "type",
    )

    /**
     * Maps attribute group names to their attribute sets.
     */
    val byName: Map<String, Set<String>> = mapOf(
        "animationAddition" to animationAddition,
        "animationAttributeTarget" to animationAttributeTarget,
        "animationEvent" to animationEvent,
        "animationTiming" to animationTiming,
        "animationValue" to animationValue,
        "conditionalProcessing" to conditionalProcessing,
        "core" to core,
        "graphicalEvent" to graphicalEvent,
        "presentation" to presentation,
        "xlink" to xlink,
        "documentEvent" to documentEvent,
        "documentElementEvent" to documentElementEvent,
        "globalEvent" to globalEvent,
        "filterPrimitive" to filterPrimitive,
        "transferFunction" to transferFunction,
    )
}

/**
 * Default attribute values per attribute group.
 */
val attrsGroupsDefaults: Map<String, Map<String, String>> = mapOf(
    "core" to mapOf("xml:space" to "default"),
    "presentation" to mapOf(
        "clip" to "auto",
        "clip-path" to "none",
        "clip-rule" to "nonzero",
        "mask" to "none",
        "opacity" to "1",
        "stop-color" to "#000",
        "stop-opacity" to "1",
        "fill-opacity" to "1",
        "fill-rule" to "nonzero",
        "fill" to "#000",
        "stroke" to "none",
        "stroke-width" to "1",
        "stroke-linecap" to "butt",
        "stroke-linejoin" to "miter",
        "stroke-miterlimit" to "4",
        "stroke-dasharray" to "none",
        "stroke-dashoffset" to "0",
        "stroke-opacity" to "1",
        "paint-order" to "normal",
        "vector-effect" to "none",
        "display" to "inline",
        "visibility" to "visible",
        "marker-start" to "none",
        "marker-mid" to "none",
        "marker-end" to "none",
        "color-interpolation" to "sRGB",
        "color-interpolation-filters" to "linearRGB",
        "color-rendering" to "auto",
        "shape-rendering" to "auto",
        "text-rendering" to "auto",
        "image-rendering" to "auto",
        "font-style" to "normal",
        "font-variant" to "normal",
        "font-weight" to "normal",
        "font-stretch" to "normal",
        "font-size" to "medium",
        "font-size-adjust" to "none",
        "kerning" to "auto",
        "letter-spacing" to "normal",
        "word-spacing" to "normal",
        "text-decoration" to "none",
        "text-anchor" to "start",
        "text-overflow" to "clip",
        "writing-mode" to "lr-tb",
        "glyph-orientation-vertical" to "auto",
        "glyph-orientation-horizontal" to "0deg",
        "direction" to "ltr",
        "unicode-bidi" to "normal",
        "dominant-baseline" to "auto",
        "alignment-baseline" to "baseline",
        "baseline-shift" to "baseline",
    ),
    "transferFunction" to mapOf(
        "slope" to "1",
        "intercept" to "0",
        "amplitude" to "1",
        "exponent" to "1",
        "offset" to "0",
    ),
)

/**
 * Deprecated attributes per attribute group (safe and unsafe).
 */
val attrsGroupsDeprecated: Map<String, DeprecatedAttrs> = mapOf(
    "animationAttributeTarget" to DeprecatedAttrs(
        unsafe = setOf("attributeType"),
    ),
    "conditionalProcessing" to DeprecatedAttrs(
        unsafe = setOf("requiredFeatures"),
    ),
    "core" to DeprecatedAttrs(
        unsafe = setOf("xml:base", "xml:lang", "xml:space"),
    ),
    "presentation" to DeprecatedAttrs(
        unsafe = setOf(
            "clip", "color-profile", "enable-background",
            "glyph-orientation-horizontal", "glyph-orientation-vertical", "kerning",
        ),
    ),
)

/**
 * Full SVG element definitions ported from svgo _collections.js `elems`.
 */
@Suppress("TooManyFunctions")
object SvgElements {
    val elems: Map<String, SvgElementConfig> = buildMap {
        put("a", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation", "xlink"),
            attrs = setOf("class", "externalResourcesRequired", "style", "target", "transform"),
            defaults = mapOf("target" to "_self"),
            contentGroups = setOf("animation", "descriptive", "paintServer", "shape", "structural"),
            content = setOf("a", "altGlyphDef", "clipPath", "color-profile", "cursor", "filter", "font-face", "font", "foreignObject", "image", "marker", "mask", "pattern", "script", "style", "switch", "text", "view", "tspan"),
        ))
        put("altGlyph", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation", "xlink"),
            attrs = setOf("class", "dx", "dy", "externalResourcesRequired", "format", "glyphRef", "rotate", "style", "x", "y"),
        ))
        put("altGlyphDef", SvgElementConfig(
            attrsGroups = setOf("core"),
            content = setOf("glyphRef"),
        ))
        put("altGlyphItem", SvgElementConfig(
            attrsGroups = setOf("core"),
            content = setOf("glyphRef", "altGlyphItem"),
        ))
        put("animate", SvgElementConfig(
            attrsGroups = setOf("animationAddition", "animationAttributeTarget", "animationEvent", "animationTiming", "animationValue", "conditionalProcessing", "core", "presentation", "xlink"),
            attrs = setOf("externalResourcesRequired"),
            contentGroups = setOf("descriptive"),
        ))
        put("animateColor", SvgElementConfig(
            attrsGroups = setOf("animationAddition", "animationAttributeTarget", "animationEvent", "animationTiming", "animationValue", "conditionalProcessing", "core", "presentation", "xlink"),
            attrs = setOf("externalResourcesRequired"),
            contentGroups = setOf("descriptive"),
        ))
        put("animateMotion", SvgElementConfig(
            attrsGroups = setOf("animationAddition", "animationEvent", "animationTiming", "animationValue", "conditionalProcessing", "core", "xlink"),
            attrs = setOf("externalResourcesRequired", "keyPoints", "origin", "path", "rotate"),
            defaults = mapOf("rotate" to "0"),
            contentGroups = setOf("descriptive"),
            content = setOf("mpath"),
        ))
        put("animateTransform", SvgElementConfig(
            attrsGroups = setOf("animationAddition", "animationAttributeTarget", "animationEvent", "animationTiming", "animationValue", "conditionalProcessing", "core", "xlink"),
            attrs = setOf("externalResourcesRequired", "type"),
            contentGroups = setOf("descriptive"),
        ))
        put("circle", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "cx", "cy", "externalResourcesRequired", "r", "style", "transform"),
            defaults = mapOf("cx" to "0", "cy" to "0"),
            contentGroups = setOf("animation", "descriptive"),
        ))
        put("clipPath", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "presentation"),
            attrs = setOf("class", "clipPathUnits", "externalResourcesRequired", "style", "transform"),
            defaults = mapOf("clipPathUnits" to "userSpaceOnUse"),
            contentGroups = setOf("animation", "descriptive", "shape"),
            content = setOf("text", "use"),
        ))
        put("color-profile", SvgElementConfig(
            attrsGroups = setOf("core", "xlink"),
            attrs = setOf("local", "name", "rendering-intent"),
            defaults = mapOf("name" to "sRGB", "rendering-intent" to "auto"),
            deprecated = DeprecatedAttrs(unsafe = setOf("name")),
            contentGroups = setOf("descriptive"),
        ))
        put("cursor", SvgElementConfig(
            attrsGroups = setOf("core", "conditionalProcessing", "xlink"),
            attrs = setOf("externalResourcesRequired", "x", "y"),
            defaults = mapOf("x" to "0", "y" to "0"),
            contentGroups = setOf("descriptive"),
        ))
        put("defs", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "externalResourcesRequired", "style", "transform"),
            contentGroups = setOf("animation", "descriptive", "paintServer", "shape", "structural"),
            content = setOf("a", "altGlyphDef", "clipPath", "color-profile", "cursor", "filter", "font-face", "font", "foreignObject", "image", "marker", "mask", "pattern", "script", "style", "switch", "text", "view"),
        ))
        put("desc", SvgElementConfig(
            attrsGroups = setOf("core"),
            attrs = setOf("class", "style"),
        ))
        put("ellipse", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "cx", "cy", "externalResourcesRequired", "rx", "ry", "style", "transform"),
            defaults = mapOf("cx" to "0", "cy" to "0"),
            contentGroups = setOf("animation", "descriptive"),
        ))
        put("feBlend", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "style", "in", "in2", "mode"),
            defaults = mapOf("mode" to "normal"),
            content = setOf("animate", "set"),
        ))
        put("feColorMatrix", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "style", "in", "type", "values"),
            defaults = mapOf("type" to "matrix"),
            content = setOf("animate", "set"),
        ))
        put("feComponentTransfer", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "style", "in"),
            content = setOf("feFuncA", "feFuncB", "feFuncG", "feFuncR"),
        ))
        put("feComposite", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "in", "in2", "k1", "k2", "k3", "k4", "operator", "style"),
            defaults = mapOf("operator" to "over", "k1" to "0", "k2" to "0", "k3" to "0", "k4" to "0"),
            content = setOf("animate", "set"),
        ))
        put("feConvolveMatrix", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "in", "kernelMatrix", "order", "style", "bias", "divisor", "edgeMode", "targetX", "targetY", "kernelUnitLength", "preserveAlpha"),
            defaults = mapOf("order" to "3", "bias" to "0", "edgeMode" to "duplicate", "preserveAlpha" to "false"),
            content = setOf("animate", "set"),
        ))
        put("feDiffuseLighting", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "diffuseConstant", "in", "kernelUnitLength", "style", "surfaceScale"),
            defaults = mapOf("surfaceScale" to "1", "diffuseConstant" to "1"),
            contentGroups = setOf("descriptive"),
            content = setOf("feDistantLight", "fePointLight", "feSpotLight"),
        ))
        put("feDisplacementMap", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "in", "in2", "scale", "style", "xChannelSelector", "yChannelSelector"),
            defaults = mapOf("scale" to "0", "xChannelSelector" to "A", "yChannelSelector" to "A"),
            content = setOf("animate", "set"),
        ))
        put("feDistantLight", SvgElementConfig(
            attrsGroups = setOf("core"),
            attrs = setOf("azimuth", "elevation"),
            defaults = mapOf("azimuth" to "0", "elevation" to "0"),
            content = setOf("animate", "set"),
        ))
        put("feDropShadow", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "style", "in", "stdDeviation", "dx", "dy"),
        ))
        put("feFlood", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "style"),
            content = setOf("animate", "animateColor", "set"),
        ))
        put("feFuncA", SvgElementConfig(
            attrsGroups = setOf("core", "transferFunction"),
            content = setOf("set", "animate"),
        ))
        put("feFuncB", SvgElementConfig(
            attrsGroups = setOf("core", "transferFunction"),
            content = setOf("set", "animate"),
        ))
        put("feFuncG", SvgElementConfig(
            attrsGroups = setOf("core", "transferFunction"),
            content = setOf("set", "animate"),
        ))
        put("feFuncR", SvgElementConfig(
            attrsGroups = setOf("core", "transferFunction"),
            content = setOf("set", "animate"),
        ))
        put("feGaussianBlur", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "style", "in", "stdDeviation"),
            defaults = mapOf("stdDeviation" to "0"),
            content = setOf("set", "animate"),
        ))
        put("feImage", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive", "xlink"),
            attrs = setOf("class", "externalResourcesRequired", "href", "preserveAspectRatio", "style", "xlink:href"),
            defaults = mapOf("preserveAspectRatio" to "xMidYMid meet"),
            content = setOf("animate", "animateTransform", "set"),
        ))
        put("feMerge", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "style"),
            content = setOf("feMergeNode"),
        ))
        put("feMergeNode", SvgElementConfig(
            attrsGroups = setOf("core"),
            attrs = setOf("in"),
            content = setOf("animate", "set"),
        ))
        put("feMorphology", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "style", "in", "operator", "radius"),
            defaults = mapOf("operator" to "erode", "radius" to "0"),
            content = setOf("animate", "set"),
        ))
        put("feOffset", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "style", "in", "dx", "dy"),
            defaults = mapOf("dx" to "0", "dy" to "0"),
            content = setOf("animate", "set"),
        ))
        put("fePointLight", SvgElementConfig(
            attrsGroups = setOf("core"),
            attrs = setOf("x", "y", "z"),
            defaults = mapOf("x" to "0", "y" to "0", "z" to "0"),
            content = setOf("animate", "set"),
        ))
        put("feSpecularLighting", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "in", "kernelUnitLength", "specularConstant", "specularExponent", "style", "surfaceScale"),
            defaults = mapOf("surfaceScale" to "1", "specularConstant" to "1", "specularExponent" to "1"),
            contentGroups = setOf("descriptive", "lightSource"),
        ))
        put("feSpotLight", SvgElementConfig(
            attrsGroups = setOf("core"),
            attrs = setOf("limitingConeAngle", "pointsAtX", "pointsAtY", "pointsAtZ", "specularExponent", "x", "y", "z"),
            defaults = mapOf("x" to "0", "y" to "0", "z" to "0", "pointsAtX" to "0", "pointsAtY" to "0", "pointsAtZ" to "0", "specularExponent" to "1"),
            content = setOf("animate", "set"),
        ))
        put("feTile", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("class", "style", "in"),
            content = setOf("animate", "set"),
        ))
        put("feTurbulence", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "filterPrimitive"),
            attrs = setOf("baseFrequency", "class", "numOctaves", "seed", "stitchTiles", "style", "type"),
            defaults = mapOf("baseFrequency" to "0", "numOctaves" to "1", "seed" to "0", "stitchTiles" to "noStitch", "type" to "turbulence"),
            content = setOf("animate", "set"),
        ))
        put("filter", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "xlink"),
            attrs = setOf("class", "externalResourcesRequired", "filterRes", "filterUnits", "height", "href", "primitiveUnits", "style", "width", "x", "xlink:href", "y"),
            defaults = mapOf("primitiveUnits" to "userSpaceOnUse", "x" to "-10%", "y" to "-10%", "width" to "120%", "height" to "120%"),
            deprecated = DeprecatedAttrs(unsafe = setOf("filterRes")),
            contentGroups = setOf("descriptive", "filterPrimitive"),
            content = setOf("animate", "set"),
        ))
        put("font", SvgElementConfig(
            attrsGroups = setOf("core", "presentation"),
            attrs = setOf("class", "externalResourcesRequired", "horiz-adv-x", "horiz-origin-x", "horiz-origin-y", "style", "vert-adv-y", "vert-origin-x", "vert-origin-y"),
            defaults = mapOf("horiz-origin-x" to "0", "horiz-origin-y" to "0"),
            deprecated = DeprecatedAttrs(unsafe = setOf("horiz-origin-x", "horiz-origin-y", "vert-adv-y", "vert-origin-x", "vert-origin-y")),
            contentGroups = setOf("descriptive"),
            content = setOf("font-face", "glyph", "hkern", "missing-glyph", "vkern"),
        ))
        put("font-face", SvgElementConfig(
            attrsGroups = setOf("core"),
            attrs = setOf("font-family", "font-style", "font-variant", "font-weight", "font-stretch", "font-size", "unicode-range", "units-per-em", "panose-1", "stemv", "stemh", "slope", "cap-height", "x-height", "accent-height", "ascent", "descent", "widths", "bbox", "ideographic", "alphabetic", "mathematical", "hanging", "v-ideographic", "v-alphabetic", "v-mathematical", "v-hanging", "underline-position", "underline-thickness", "strikethrough-position", "strikethrough-thickness", "overline-position", "overline-thickness"),
            defaults = mapOf("font-style" to "all", "font-variant" to "normal", "font-weight" to "all", "font-stretch" to "normal", "unicode-range" to "U+0-10FFFF", "units-per-em" to "1000", "panose-1" to "0 0 0 0 0 0 0 0 0 0", "slope" to "0"),
            deprecated = DeprecatedAttrs(unsafe = setOf("accent-height", "alphabetic", "ascent", "bbox", "cap-height", "descent", "hanging", "ideographic", "mathematical", "panose-1", "slope", "stemh", "stemv", "unicode-range", "units-per-em", "v-alphabetic", "v-hanging", "v-ideographic", "v-mathematical", "widths", "x-height")),
            contentGroups = setOf("descriptive"),
            content = setOf("font-face-src"),
        ))
        put("font-face-format", SvgElementConfig(
            attrsGroups = setOf("core"),
            attrs = setOf("string"),
            deprecated = DeprecatedAttrs(unsafe = setOf("string")),
        ))
        put("font-face-name", SvgElementConfig(
            attrsGroups = setOf("core"),
            attrs = setOf("name"),
            deprecated = DeprecatedAttrs(unsafe = setOf("name")),
        ))
        put("font-face-src", SvgElementConfig(
            attrsGroups = setOf("core"),
            content = setOf("font-face-name", "font-face-uri"),
        ))
        put("font-face-uri", SvgElementConfig(
            attrsGroups = setOf("core", "xlink"),
            attrs = setOf("href", "xlink:href"),
            content = setOf("font-face-format"),
        ))
        put("foreignObject", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "externalResourcesRequired", "height", "style", "transform", "width", "x", "y"),
            defaults = mapOf("x" to "0", "y" to "0"),
        ))
        put("g", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "externalResourcesRequired", "style", "transform"),
            contentGroups = setOf("animation", "descriptive", "paintServer", "shape", "structural"),
            content = setOf("a", "altGlyphDef", "clipPath", "color-profile", "cursor", "filter", "font-face", "font", "foreignObject", "image", "marker", "mask", "pattern", "script", "style", "switch", "text", "view"),
        ))
        put("glyph", SvgElementConfig(
            attrsGroups = setOf("core", "presentation"),
            attrs = setOf("arabic-form", "class", "d", "glyph-name", "horiz-adv-x", "lang", "orientation", "style", "unicode", "vert-adv-y", "vert-origin-x", "vert-origin-y"),
            defaults = mapOf("arabic-form" to "initial"),
            deprecated = DeprecatedAttrs(unsafe = setOf("arabic-form", "glyph-name", "horiz-adv-x", "orientation", "unicode", "vert-adv-y", "vert-origin-x", "vert-origin-y")),
            contentGroups = setOf("animation", "descriptive", "paintServer", "shape", "structural"),
            content = setOf("a", "altGlyphDef", "clipPath", "color-profile", "cursor", "filter", "font-face", "font", "foreignObject", "image", "marker", "mask", "pattern", "script", "style", "switch", "text", "view"),
        ))
        put("glyphRef", SvgElementConfig(
            attrsGroups = setOf("core", "presentation"),
            attrs = setOf("class", "d", "horiz-adv-x", "style", "vert-adv-y", "vert-origin-x", "vert-origin-y"),
            deprecated = DeprecatedAttrs(unsafe = setOf("horiz-adv-x", "vert-adv-y", "vert-origin-x", "vert-origin-y")),
            contentGroups = setOf("animation", "descriptive", "paintServer", "shape", "structural"),
            content = setOf("a", "altGlyphDef", "clipPath", "color-profile", "cursor", "filter", "font-face", "font", "foreignObject", "image", "marker", "mask", "pattern", "script", "style", "switch", "text", "view"),
        ))
        put("hatch", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "xlink"),
            attrs = setOf("class", "hatchContentUnits", "hatchUnits", "pitch", "rotate", "style", "transform", "x", "y"),
            defaults = mapOf("hatchUnits" to "objectBoundingBox", "hatchContentUnits" to "userSpaceOnUse", "x" to "0", "y" to "0", "pitch" to "0", "rotate" to "0"),
            contentGroups = setOf("animation", "descriptive"),
            content = setOf("hatchPath"),
        ))
        put("hatchPath", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "xlink"),
            attrs = setOf("class", "style", "d", "offset"),
            defaults = mapOf("offset" to "0"),
            contentGroups = setOf("animation", "descriptive"),
        ))
        put("hkern", SvgElementConfig(
            attrsGroups = setOf("core"),
            attrs = setOf("u1", "g1", "u2", "g2", "k"),
            deprecated = DeprecatedAttrs(unsafe = setOf("g1", "g2", "k", "u1", "u2")),
        ))
        put("image", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation", "xlink"),
            attrs = setOf("class", "externalResourcesRequired", "height", "href", "preserveAspectRatio", "style", "transform", "width", "x", "xlink:href", "y"),
            defaults = mapOf("x" to "0", "y" to "0", "preserveAspectRatio" to "xMidYMid meet"),
            contentGroups = setOf("animation", "descriptive"),
        ))
        put("line", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "externalResourcesRequired", "style", "transform", "x1", "x2", "y1", "y2"),
            defaults = mapOf("x1" to "0", "y1" to "0", "x2" to "0", "y2" to "0"),
            contentGroups = setOf("animation", "descriptive"),
        ))
        put("linearGradient", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "xlink"),
            attrs = setOf("class", "externalResourcesRequired", "gradientTransform", "gradientUnits", "href", "spreadMethod", "style", "x1", "x2", "xlink:href", "y1", "y2"),
            defaults = mapOf("x1" to "0", "y1" to "0", "x2" to "100%", "y2" to "0", "spreadMethod" to "pad"),
            contentGroups = setOf("descriptive"),
            content = setOf("animate", "animateTransform", "set", "stop"),
        ))
        put("marker", SvgElementConfig(
            attrsGroups = setOf("core", "presentation"),
            attrs = setOf("class", "externalResourcesRequired", "markerHeight", "markerUnits", "markerWidth", "orient", "preserveAspectRatio", "refX", "refY", "style", "viewBox"),
            defaults = mapOf("markerUnits" to "strokeWidth", "refX" to "0", "refY" to "0", "markerWidth" to "3", "markerHeight" to "3"),
            contentGroups = setOf("animation", "descriptive", "paintServer", "shape", "structural"),
            content = setOf("a", "altGlyphDef", "clipPath", "color-profile", "cursor", "filter", "font-face", "font", "foreignObject", "image", "marker", "mask", "pattern", "script", "style", "switch", "text", "view"),
        ))
        put("mask", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "presentation"),
            attrs = setOf("class", "externalResourcesRequired", "height", "mask-type", "maskContentUnits", "maskUnits", "style", "width", "x", "y"),
            defaults = mapOf("maskUnits" to "objectBoundingBox", "maskContentUnits" to "userSpaceOnUse", "x" to "-10%", "y" to "-10%", "width" to "120%", "height" to "120%"),
            contentGroups = setOf("animation", "descriptive", "paintServer", "shape", "structural"),
            content = setOf("a", "altGlyphDef", "clipPath", "color-profile", "cursor", "filter", "font-face", "font", "foreignObject", "image", "marker", "mask", "pattern", "script", "style", "switch", "text", "view"),
        ))
        put("metadata", SvgElementConfig(
            attrsGroups = setOf("core"),
        ))
        put("missing-glyph", SvgElementConfig(
            attrsGroups = setOf("core", "presentation"),
            attrs = setOf("class", "d", "horiz-adv-x", "style", "vert-adv-y", "vert-origin-x", "vert-origin-y"),
            deprecated = DeprecatedAttrs(unsafe = setOf("horiz-adv-x", "vert-adv-y", "vert-origin-x", "vert-origin-y")),
            contentGroups = setOf("animation", "descriptive", "paintServer", "shape", "structural"),
            content = setOf("a", "altGlyphDef", "clipPath", "color-profile", "cursor", "filter", "font-face", "font", "foreignObject", "image", "marker", "mask", "pattern", "script", "style", "switch", "text", "view"),
        ))
        put("mpath", SvgElementConfig(
            attrsGroups = setOf("core", "xlink"),
            attrs = setOf("externalResourcesRequired", "href", "xlink:href"),
            contentGroups = setOf("descriptive"),
        ))
        put("path", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "d", "externalResourcesRequired", "pathLength", "style", "transform"),
            contentGroups = setOf("animation", "descriptive"),
        ))
        put("pattern", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "presentation", "xlink"),
            attrs = setOf("class", "externalResourcesRequired", "height", "href", "patternContentUnits", "patternTransform", "patternUnits", "preserveAspectRatio", "style", "viewBox", "width", "x", "xlink:href", "y"),
            defaults = mapOf("patternUnits" to "objectBoundingBox", "patternContentUnits" to "userSpaceOnUse", "x" to "0", "y" to "0", "width" to "0", "height" to "0", "preserveAspectRatio" to "xMidYMid meet"),
            contentGroups = setOf("animation", "descriptive", "paintServer", "shape", "structural"),
            content = setOf("a", "altGlyphDef", "clipPath", "color-profile", "cursor", "filter", "font-face", "font", "foreignObject", "image", "marker", "mask", "pattern", "script", "style", "switch", "text", "view"),
        ))
        put("polygon", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "externalResourcesRequired", "points", "style", "transform"),
            contentGroups = setOf("animation", "descriptive"),
        ))
        put("polyline", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "externalResourcesRequired", "points", "style", "transform"),
            contentGroups = setOf("animation", "descriptive"),
        ))
        put("radialGradient", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "xlink"),
            attrs = setOf("class", "cx", "cy", "externalResourcesRequired", "fr", "fx", "fy", "gradientTransform", "gradientUnits", "href", "r", "spreadMethod", "style", "xlink:href"),
            defaults = mapOf("gradientUnits" to "objectBoundingBox", "cx" to "50%", "cy" to "50%", "r" to "50%"),
            contentGroups = setOf("descriptive"),
            content = setOf("animate", "animateTransform", "set", "stop"),
        ))
        put("meshGradient", SvgElementConfig(
            attrsGroups = setOf("core", "presentation", "xlink"),
            attrs = setOf("class", "style", "x", "y", "gradientUnits", "transform"),
            contentGroups = setOf("descriptive", "paintServer", "animation"),
            content = setOf("meshRow"),
        ))
        put("meshRow", SvgElementConfig(
            attrsGroups = setOf("core", "presentation"),
            attrs = setOf("class", "style"),
            contentGroups = setOf("descriptive"),
            content = setOf("meshPatch"),
        ))
        put("meshPatch", SvgElementConfig(
            attrsGroups = setOf("core", "presentation"),
            attrs = setOf("class", "style"),
            contentGroups = setOf("descriptive"),
            content = setOf("stop"),
        ))
        put("rect", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "externalResourcesRequired", "height", "rx", "ry", "style", "transform", "width", "x", "y"),
            defaults = mapOf("x" to "0", "y" to "0"),
            contentGroups = setOf("animation", "descriptive"),
        ))
        put("script", SvgElementConfig(
            attrsGroups = setOf("core", "xlink"),
            attrs = setOf("externalResourcesRequired", "type", "href", "xlink:href"),
        ))
        put("set", SvgElementConfig(
            attrsGroups = setOf("animation", "animationAttributeTarget", "animationTiming", "conditionalProcessing", "core", "xlink"),
            attrs = setOf("externalResourcesRequired", "to"),
            contentGroups = setOf("descriptive"),
        ))
        put("solidColor", SvgElementConfig(
            attrsGroups = setOf("core", "presentation"),
            attrs = setOf("class", "style"),
            contentGroups = setOf("paintServer"),
        ))
        put("stop", SvgElementConfig(
            attrsGroups = setOf("core", "presentation"),
            attrs = setOf("class", "style", "offset", "path"),
            content = setOf("animate", "animateColor", "set"),
        ))
        put("style", SvgElementConfig(
            attrsGroups = setOf("core"),
            attrs = setOf("type", "media", "title"),
            defaults = mapOf("type" to "text/css"),
        ))
        put("svg", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "documentEvent", "graphicalEvent", "presentation"),
            attrs = setOf("baseProfile", "class", "contentScriptType", "contentStyleType", "height", "preserveAspectRatio", "style", "version", "viewBox", "width", "x", "y", "zoomAndPan"),
            defaults = mapOf("x" to "0", "y" to "0", "width" to "100%", "height" to "100%", "preserveAspectRatio" to "xMidYMid meet", "zoomAndPan" to "magnify", "version" to "1.1", "baseProfile" to "none", "contentScriptType" to "application/ecmascript", "contentStyleType" to "text/css"),
            deprecated = DeprecatedAttrs(
                safe = setOf("version"),
                unsafe = setOf("baseProfile", "contentScriptType", "contentStyleType", "zoomAndPan"),
            ),
            contentGroups = setOf("animation", "descriptive", "paintServer", "shape", "structural"),
            content = setOf("a", "altGlyphDef", "clipPath", "color-profile", "cursor", "filter", "font-face", "font", "foreignObject", "image", "marker", "mask", "pattern", "script", "style", "switch", "text", "view"),
        ))
        put("switch", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "externalResourcesRequired", "style", "transform"),
            contentGroups = setOf("animation", "descriptive", "shape"),
            content = setOf("a", "foreignObject", "g", "image", "svg", "switch", "text", "use"),
        ))
        put("symbol", SvgElementConfig(
            attrsGroups = setOf("core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "externalResourcesRequired", "preserveAspectRatio", "refX", "refY", "style", "viewBox"),
            defaults = mapOf("refX" to "0", "refY" to "0"),
            contentGroups = setOf("animation", "descriptive", "paintServer", "shape", "structural"),
            content = setOf("a", "altGlyphDef", "clipPath", "color-profile", "cursor", "filter", "font-face", "font", "foreignObject", "image", "marker", "mask", "pattern", "script", "style", "switch", "text", "view"),
        ))
        put("text", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "dx", "dy", "externalResourcesRequired", "lengthAdjust", "rotate", "style", "textLength", "transform", "x", "y"),
            defaults = mapOf("x" to "0", "y" to "0", "lengthAdjust" to "spacing"),
            contentGroups = setOf("animation", "descriptive", "textContentChild"),
            content = setOf("a"),
        ))
        put("textPath", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation", "xlink"),
            attrs = setOf("class", "d", "externalResourcesRequired", "href", "method", "spacing", "startOffset", "style", "xlink:href"),
            defaults = mapOf("startOffset" to "0", "method" to "align", "spacing" to "exact"),
            contentGroups = setOf("descriptive"),
            content = setOf("a", "altGlyph", "animate", "animateColor", "set", "tref", "tspan"),
        ))
        put("title", SvgElementConfig(
            attrsGroups = setOf("core"),
            attrs = setOf("class", "style"),
        ))
        put("tref", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation", "xlink"),
            attrs = setOf("class", "externalResourcesRequired", "href", "style", "xlink:href"),
            contentGroups = setOf("descriptive"),
            content = setOf("animate", "animateColor", "set"),
        ))
        put("tspan", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation"),
            attrs = setOf("class", "dx", "dy", "externalResourcesRequired", "lengthAdjust", "rotate", "style", "textLength", "x", "y"),
            contentGroups = setOf("descriptive"),
            content = setOf("a", "altGlyph", "animate", "animateColor", "set", "tref", "tspan"),
        ))
        put("use", SvgElementConfig(
            attrsGroups = setOf("conditionalProcessing", "core", "graphicalEvent", "presentation", "xlink"),
            attrs = setOf("class", "externalResourcesRequired", "height", "href", "style", "transform", "width", "x", "xlink:href", "y"),
            defaults = mapOf("x" to "0", "y" to "0"),
            contentGroups = setOf("animation", "descriptive"),
        ))
        put("view", SvgElementConfig(
            attrsGroups = setOf("core"),
            attrs = setOf("externalResourcesRequired", "preserveAspectRatio", "viewBox", "viewTarget", "zoomAndPan"),
            deprecated = DeprecatedAttrs(unsafe = setOf("viewTarget", "zoomAndPan")),
            contentGroups = setOf("descriptive"),
        ))
        put("vkern", SvgElementConfig(
            attrsGroups = setOf("core"),
            attrs = setOf("u1", "g1", "u2", "g2", "k"),
            deprecated = DeprecatedAttrs(unsafe = setOf("g1", "g2", "k", "u1", "u2")),
        ))
    }

    /**
     * Element group definitions used to resolve `contentGroups` references.
     */
    private val elemsGroups: Map<String, Set<String>> = mapOf(
        "animation" to setOf("animate", "animateColor", "animateMotion", "animateTransform", "set"),
        "descriptive" to setOf("desc", "metadata", "title"),
        "shape" to setOf("circle", "ellipse", "line", "path", "polygon", "polyline", "rect"),
        "structural" to setOf("defs", "g", "svg", "symbol", "use"),
        "paintServer" to setOf("hatch", "linearGradient", "meshGradient", "pattern", "radialGradient", "solidColor"),
        "nonRendering" to setOf("clipPath", "filter", "linearGradient", "marker", "mask", "pattern", "radialGradient", "solidColor", "symbol"),
        "container" to setOf("a", "defs", "foreignObject", "g", "marker", "mask", "missing-glyph", "pattern", "svg", "switch", "symbol"),
        "textContent" to setOf("a", "altGlyph", "altGlyphDef", "altGlyphItem", "glyph", "glyphRef", "text", "textPath", "tref", "tspan"),
        "textContentChild" to setOf("altGlyph", "textPath", "tref", "tspan"),
        "lightSource" to setOf("feDiffuseLighting", "feDistantLight", "fePointLight", "feSpecularLighting", "feSpotLight"),
        "filterPrimitive" to setOf("feBlend", "feColorMatrix", "feComponentTransfer", "feComposite", "feConvolveMatrix", "feDiffuseLighting", "feDisplacementMap", "feDropShadow", "feFlood", "feFuncA", "feFuncB", "feFuncG", "feFuncR", "feGaussianBlur", "feImage", "feMerge", "feMergeNode", "feMorphology", "feOffset", "feSpecularLighting", "feTile", "feTurbulence"),
    )

    /**
     * Pre-computed set of allowed child element names per element.
     * Resolves both `content` and `contentGroups` references.
     */
    val allowedChildrenPerElement: Map<String, Set<String>> by lazy {
        buildMap {
            for ((name, config) in elems) {
                val children = buildSet {
                    addAll(config.content)
                    for (groupName in config.contentGroups) {
                        val group = elemsGroups[groupName]
                        if (group != null) {
                            addAll(group)
                        }
                    }
                }
                put(name, children)
            }
        }
    }

    /**
     * Pre-computed set of allowed attribute names per element.
     * Resolves attrsGroups references and element-specific attrs.
     */
    val allowedAttributesPerElement: Map<String, Set<String>> by lazy {
        buildMap {
            for ((name, config) in elems) {
                val attrs = buildSet {
                    addAll(config.attrs)
                    for (groupName in config.attrsGroups) {
                        val group = AttrsGroups.byName[groupName]
                        if (group != null) {
                            addAll(group)
                        }
                    }
                }
                put(name, attrs)
            }
        }
    }

    /**
     * Pre-computed map of attribute default values per element.
     * Merges element-specific defaults with attrsGroupsDefaults.
     */
    val attributesDefaultsPerElement: Map<String, Map<String, String>> by lazy {
        buildMap {
            for ((name, config) in elems) {
                val defaults = buildMap {
                    // Element-specific defaults first (matching JS order)
                    putAll(config.defaults)
                    // Group defaults can overwrite element defaults (JS Map.set behavior)
                    for (groupName in config.attrsGroups) {
                        val groupDefaults = attrsGroupsDefaults[groupName]
                        if (groupDefaults != null) {
                            putAll(groupDefaults)
                        }
                    }
                }
                put(name, defaults)
            }
        }
    }
}
