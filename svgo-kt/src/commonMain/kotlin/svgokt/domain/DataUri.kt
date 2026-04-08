package svgokt.domain

enum class DataUri(val value: String) {
    /** Encode as base64 data URI. */
    Base64(value = "base64"),

    /** Encode as URL-encoded (percent-encoded) data URI. */
    Enc(value = "enc"),

    /** Encode as minimally-encoded (unencoded) data URI. */
    UnEnc(value = "unenc"),
}
