package svgokt.path

data class PathDataItem(
    var command: Char,
    val args: MutableList<Double>,
) {
    /**
     * Absolute coordinates of the point before this command was applied.
     * Set during [convertToRelative] for use by later optimization passes.
     */
    var base: DoubleArray? = null

    /**
     * Absolute coordinates of the cursor after this command.
     * Set during [convertToRelative] for use by later optimization passes.
     */
    var coords: DoubleArray? = null
}
