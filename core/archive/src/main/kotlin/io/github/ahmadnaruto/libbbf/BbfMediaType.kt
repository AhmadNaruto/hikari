package io.github.ahmadnaruto.libbbf

enum class BbfMediaType(val value: Int) {
    UNKNOWN(0x00),
    AVIF(0x01),
    PNG(0x02),
    WEBP(0x03),
    JXL(0x04),
    BMP(0x05),
    GIF(0x07),
    TIFF(0x08),
    JPG(0x09);

    companion object {
        fun fromValue(value: Int): BbfMediaType {
            return values().firstOrNull { it.value == value } ?: UNKNOWN
        }
    }
}
