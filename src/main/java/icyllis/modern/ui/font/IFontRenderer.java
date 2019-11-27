package icyllis.modern.ui.font;

public interface IFontRenderer {

    /**
     * Draw string
     *
     * @param str unformatted text
     * @param startX x pos
     * @param startY y pos
     * @param color rgb hex (eg 0xffffff)
     * @param alpha int (0x00-0xff)
     * @param align 0-left 0.25-center 0.5-right
     * @return formatted text width
     */
    float drawString(String str, float startX, float startY, int color, int alpha, float align);

    /**
     * Get string width
     *
     * @param str unformatted text
     * @return formatted text width
     */
    float getStringWidth(String str);

    /**
     * Return the number of characters in a string that will completely fit inside the specified width when rendered.
     *
     * @param str   the String to analyze
     * @param width the desired string width (in GUI coordinate system)
     * @return the number of characters from str that will fit inside width
     */
    int sizeStringToWidth(String str, float width);

    /**
     * Trim a string so that it fits in the specified width when rendered, optionally reversing the string
     *
     * @param str     the String to trim
     * @param width   the desired string width (in GUI coordinate system)
     * @param reverse if true, the returned string will also be reversed
     * @return the trimmed and optionally reversed string
     */
    String trimStringToWidth(String str, float width, boolean reverse);
}
