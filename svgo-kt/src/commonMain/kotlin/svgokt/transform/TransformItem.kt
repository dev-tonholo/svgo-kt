package svgokt.transform

data class TransformItem(
    val name: String,
    val data: DoubleArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransformItem) return false
        return name == other.name && data.contentEquals(other.data)
    }

    override fun hashCode(): Int = 31 * name.hashCode() + data.contentHashCode()
}
