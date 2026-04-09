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

    /**
     * Original curve data preserved when a curve is converted to an arc.
     * Used by makeArcs to check if the previous item was part of an arc sequence.
     */
    var sdata: List<Double>? = null
}
