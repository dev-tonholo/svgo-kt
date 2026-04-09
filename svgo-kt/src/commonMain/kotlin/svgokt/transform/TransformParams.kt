package svgokt.transform

/**
 * Parameters for the convertTransform plugin's rounding and optimization logic.
 * Mirrors the JS `TransformParams` typedef.
 */
data class TransformParams(
    val convertToShorts: Boolean = true,
    val degPrecision: Int? = null,
    val floatPrecision: Int = DEFAULT_FLOAT_PRECISION,
    val transformPrecision: Int = DEFAULT_TRANSFORM_PRECISION,
    val matrixToTransform: Boolean = true,
    val shortTranslate: Boolean = true,
    val shortScale: Boolean = true,
    val shortRotate: Boolean = true,
    val removeUseless: Boolean = true,
    val collapseIntoOne: Boolean = true,
    val leadingZero: Boolean = true,
    val negativeExtraSpace: Boolean = false,
) {
    companion object {
        const val DEFAULT_FLOAT_PRECISION = 3
        const val DEFAULT_TRANSFORM_PRECISION = 5
    }
}
