package svgokt

object Tools {
    private val regReferencesUrl = """\burl\((?:(["'])#.+?\1|#[^)]+)\)""".toRegex()

    /**
     * For example, a string that contains one or more of the following would match and
     * return true:
     *
     * * `url(#gradient001)`
     * * `url('#gradient001')`
     *
     * @returns If the given string includes a URL reference.
     */
    fun includesUrlReference(body: String): Boolean =
        regReferencesUrl.containsMatchIn(body)
}
