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
    JPG(0x09),
    TXT(0x0A),
    HTML(0x0B),
    CSS(0x0C),
    JSON(0x0D),
    EPUB(0x0E),
    TTF(0x0F),
    OTF(0x10),
    WOFF(0x11),
    WOFF2(0x12),
    PDF(0x13),
    MD(0x14),
    SVG(0x15),
    JS(0x16);

    companion object {
        /** Single source of truth for raster/vector image types in a BBF container. */
        val IMAGE_TYPES: Set<BbfMediaType> = setOf(AVIF, PNG, WEBP, JXL, BMP, GIF, TIFF, JPG)

        fun fromValue(value: Int): BbfMediaType {
            return values().firstOrNull { it.value == value } ?: UNKNOWN
        }
    }
}

/** Returns true if this media type represents a renderable image format. */
fun BbfMediaType.isImage(): Boolean = this in BbfMediaType.IMAGE_TYPES
