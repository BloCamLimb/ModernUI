/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.test;

import icyllis.arc3d.core.*;
import icyllis.arc3d.core.j2d.Typeface_JDK;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.font.FontRenderContext;

public class TestStrike {

    public static final Logger LOGGER = LoggerFactory.getLogger("Arc3D");

    public static void main(String[] args) {
        Typeface_JDK typeface = new Typeface_JDK(
                new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 1));
        // map a character to glyphID
        int glyphID = typeface.getFont().createGlyphVector(
                new FontRenderContext(null, true, false),
                "我").getGlyphCode(0);

        Font font = new Font();
        font.setTypeface(typeface);
        font.setSize(200);
        font.setEdging(Font.kAntiAlias_Edging);

        Paint paint = new Paint();
        paint.setStyle(Paint.STROKE);
        paint.setStrokeJoin(Paint.JOIN_MITER);

        Matrix devMatrix = new Matrix();

        StrikeDesc strikeDesc = StrikeDesc.makeMask(font, paint, devMatrix);

        Strike strike = strikeDesc.findOrCreateStrike();

        strike.lock();
        try {
            Glyph glyph = strike.digestFor(Glyph.kDirectMask, glyphID);
            int action = glyph.actionFor(Glyph.kDirectMask);
            LOGGER.info("ActionFor DirectMask: {}", action);
            action = strike.digestFor(Glyph.kPath, glyphID).actionFor(Glyph.kPath);
            LOGGER.info("ActionFor Path: {}", action);

            Rect2f bounds = new Rect2f();
            glyph.getBounds(bounds);
            LOGGER.info("Bounds: {}", bounds);

            boolean res = strike.prepareForImage(glyph);
            LOGGER.info("PrepareForImage: {}", res);
            if (res) {
                int size = glyph.getWidth() * glyph.getHeight();
                assert size > 0;
                long dst = MemoryUtil.nmemAlloc(size);
                if (glyph.getMaskFormat() == Mask.kBW_Format) {
                    PixelUtils.unpackBWToA8(
                            glyph.getImageBase(), glyph.getImageAddress(), glyph.getRowBytes(),
                            null, dst, glyph.getWidth(), glyph.getWidth(), glyph.getHeight()
                    );
                } else {
                    PixelUtils.copyImage(
                            glyph.getImageBase(), glyph.getImageAddress(), glyph.getRowBytes(),
                            null, dst, glyph.getWidth(), glyph.getWidth(), glyph.getHeight()
                    );
                }
                STBImageWrite.stbi_write_png("test_prepare_for_image.png",
                        glyph.getWidth(), glyph.getHeight(), 1,
                        MemoryUtil.memByteBuffer(dst, size), 0);
                MemoryUtil.nmemFree(dst);
            }

            res = strike.prepareForPath(glyph);
            LOGGER.info("PrepareForPath: {}", res);
            if (res) {
                var path = glyph.getPath();
                path.forEach(TestPathUtils.PRINTER);
                LOGGER.info("Verbs {} Points {} Bytes {}", path.countVerbs(), path.countPoints(), path.estimatedByteSize());
            }
        } finally {
            strike.unlock();
        }

        LOGGER.info("StrikeCache memory size: {}", StrikeCache.getGlobalStrikeCache().getTotalMemoryUsed());
    }
}
