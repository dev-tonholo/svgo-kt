package svgokt.path

import svgokt.domain.XastElement

/**
 * Convert path string to JS representation.
 *
 * Parses the element's `d` attribute via [parsePathData] and normalizes
 * the first moveto command to absolute (uppercase M).
 */
fun path2js(element: XastElement): List<PathDataItem> {
    val d = element.attributes["d"] ?: return emptyList()
    val pathData = parsePathData(d).map { item ->
        PathDataItem(command = item.command, args = item.args.toMutableList())
    }

    // First moveto is actually absolute. Subsequent coordinates were separated above.
    if (pathData.isNotEmpty() && pathData[0].command == 'm') {
        pathData[0].command = 'M'
    }

    return pathData
}

/**
 * Convert path data back to the element's `d` attribute string.
 *
 * Filters consecutive moveto commands (keeps only the last one in a row).
 */
fun js2path(
    element: XastElement,
    data: List<PathDataItem>,
    precision: Int? = null,
    noSpaceAfterFlags: Boolean = false,
) {
    val pathData = mutableListOf<PathDataItem>()
    for (item in data) {
        // remove moveto commands which are followed by moveto commands
        if (pathData.isNotEmpty() && (item.command == 'M' || item.command == 'm')) {
            val last = pathData.last()
            if (last.command == 'M' || last.command == 'm') {
                pathData.removeLast()
            }
        }
        pathData.add(
            PathDataItem(
                command = item.command,
                args = item.args.toMutableList(),
            ),
        )
    }

    element.attributes["d"] = stringifyPathData(
        pathData = pathData,
        precision = precision,
        disableSpaceAfterFlags = noSpaceAfterFlags,
    )
}
