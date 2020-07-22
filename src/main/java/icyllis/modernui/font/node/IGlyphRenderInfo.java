/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.font.node;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.vector.Matrix4f;

import javax.annotation.Nonnull;

public interface IGlyphRenderInfo {

    /**
     * Draw a character of this info.
     *
     * @param builder vertex builder
     * @param raw     needed by {@link DigitGlyphInfo}
     * @param x       start x of this character
     * @param y       start y
     * @param r       final red
     * @param g       final green
     * @param b       final blue
     * @param a       final alpha
     */
    void drawString(@Nonnull BufferBuilder builder, @Nonnull String raw, float x, float y, int r, int g, int b, int a);

    /**
     * Draw a character of this info
     *
     * @param matrix matrix
     * @param buffer buffer source
     * @param raw    needed by {@link DigitGlyphInfo}
     * @param x      start x of this character
     * @param y      start y
     * @param r      final red
     * @param g      final green
     * @param b      final blue
     * @param a      final alpha
     * @param light  packed light
     */
    void drawString(Matrix4f matrix, @Nonnull IRenderTypeBuffer buffer, @Nonnull String raw, float x, float y, int r, int g, int b, int a, int light);

    /**
     * Get the glyph advance of this info
     *
     * @return advance
     */
    float getAdvance();
}
