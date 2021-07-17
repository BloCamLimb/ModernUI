/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics.font;

import icyllis.modernui.text.FontCollection;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;

public class GlyphManagerBase {

    private static GlyphManagerBase instance;

    public static final int TEXTURE_SIZE = 1024;

    /**
     * All font glyphs are packed inside this image and are then loaded from here into an OpenGL texture.
     */
    protected final BufferedImage mGlyphImage = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);

    /**
     * The Graphics2D associated with glyphTextureImage and used for bit blit between stringImage.
     */
    protected final Graphics2D mGlyphGraphics = mGlyphImage.createGraphics();

    /**
     * A cache of all fonts that have at least one glyph pre-rendered in a texture.
     * Each font maps to an integer (monotonically increasing) which forms the
     * upper 32 bits of the key into the glyphCache map. This font cache can include
     * different styles of the same font family like bold or italic and different size.
     */
    protected final Object2IntMap<Font> mFontKeyMap = new Object2IntOpenHashMap<>();

    public GlyphManagerBase() {
        instance = this;
    }

    public static GlyphManagerBase getInstance() {
        return instance;
    }

    /**
     * Given a single OpenType font, perform full text layout and create a new GlyphVector for a string.
     *
     * @param font  the Font used to layout a GlyphVector for the string
     * @param text  the string to layout
     * @param start the offset into text at which to start the layout
     * @param limit the (offset + length) at which to stop performing the layout
     * @param flags either {@link Font#LAYOUT_RIGHT_TO_LEFT} or {@link Font#LAYOUT_LEFT_TO_RIGHT}
     * @return the newly laid-out GlyphVector
     */
    public GlyphVector layoutGlyphVector(@Nonnull Font font, char[] text, int start, int limit, int flags) {
        return font.layoutGlyphVector(mGlyphGraphics.getFontRenderContext(), text, start, limit, flags);
    }

    /**
     * Derive a font family with given style and size
     *
     * @param family font family (with plain style and size 1)
     * @param style  font style
     * @param size   font size in pixel
     * @return derived font with style and size
     */
    @Nonnull
    public Font deriveFont(@Nonnull Font family, int style, int size) {
        family = family.deriveFont(style, size); // a new Font object, but they are equal)
        /* Ensure this font is already in fontKeyMap so it can be referenced by lookupGlyph() later on */
        mFontKeyMap.putIfAbsent(family, mFontKeyMap.size());
        return family;
    }

    /**
     * Re-calculate font metrics in pixels, the higher 32 bits are ascent and
     * lower 32 bits are descent.
     */
    public void getFontMetrics(@Nonnull FontCollection font, int style, int size, @Nonnull FontMetricsInt fm) {
        fm.reset();
        for (Font family : font.getFonts()) {
            fm.extendBy(mGlyphGraphics.getFontMetrics(
                    family.deriveFont(style, size)));
        }
    }

    // extend font metrics
    public void getFontMetrics(@Nonnull Font derivedFont, @Nonnull FontMetricsInt fm) {
        fm.extendBy(mGlyphGraphics.getFontMetrics(derivedFont));
    }
}
