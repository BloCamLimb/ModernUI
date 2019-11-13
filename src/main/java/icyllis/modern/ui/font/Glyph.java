package icyllis.modern.ui.font;

public class Glyph implements Comparable<Glyph> {

    /** The index into the original string (i.e. with color codes) for the character that generated this glyph. */
    int stringIndex;

    /** Texture ID and position/size of the glyph's pre-rendered image within the cache texture. */
    GlyphCache.Entry texture;

    /** Glyph's horizontal position (in pixels) relative to the entire string's baseline */
    int x;

    /** Glyph's vertical position (in pixels) relative to the entire string's baseline */
    int y;

    /** Glyph's horizontal advance (in pixels) used for strikethrough and underline effects */
    int advance;

    /**
     * Allows arrays of Glyph objects to be sorted. Performs numeric comparison on stringIndex.
     *
     * @param o the other Glyph object being compared with this one
     * @return either -1, 0, or 1 if this < other, this == other, or this > other
     */
    @Override
    public int compareTo(Glyph o) {
        return Integer.compare(stringIndex, o.stringIndex);
    }
}
