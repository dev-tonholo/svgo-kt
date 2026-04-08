package svgokt.transform

private val TRANSFORM_TYPES = setOf(
    "matrix",
    "rotate",
    "scale",
    "skewX",
    "skewY",
    "translate",
)

private val REG_TRANSFORM_SPLIT =
    """\s*(matrix|translate|scale|rotate|skewX|skewY)\s*\(\s*(.+?)\s*\)[\s,]*""".toRegex()

private val REG_NUMERIC_VALUES =
    """[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?""".toRegex()

/**
 * Convert an SVG transform string to a list of [TransformItem].
 *
 * Parses transform functions such as `matrix(a,b,c,d,e,f)`, `translate(tx,ty)`,
 * `scale(sx,sy)`, `rotate(a,cx,cy)`, `skewX(a)`, and `skewY(a)`.
 *
 * @param transformString the raw SVG transform attribute value.
 * @return a list of parsed transform items, or an empty list if the string is malformed.
 */
fun parseTransform(transformString: String): List<TransformItem> {
    val transforms = mutableListOf<TransformItem>()
    var currentTransform: TransformItem? = null

    val matches = REG_TRANSFORM_SPLIT.findAll(input = transformString)
    for (match in matches) {
        val name = match.groupValues[1]
        val valueString = match.groupValues[2]

        if (name !in TRANSFORM_TYPES) continue

        val data = REG_NUMERIC_VALUES.findAll(input = valueString)
            .map { it.value.toDouble() }
            .toList()
            .toDoubleArray()

        val item = TransformItem(name = name, data = data)
        transforms.add(item)
        currentTransform = item
    }

    return if (currentTransform == null || currentTransform.data.isEmpty()) {
        emptyList()
    } else {
        transforms
    }
}
