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

package icyllis.arc3d.core.j2d;

import icyllis.arc3d.core.*;
import sun.misc.Unsafe;

import java.awt.Color;
import java.awt.Font;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Wraps Java2D's freetype scaler.
 */
public class ScalerContext_JDK extends ScalerContext {

    private final Font mFont;
    private final FontRenderContext mFRC;

    public ScalerContext_JDK(Typeface_JDK typeface, StrikeDesc desc) {
        super(typeface, desc);
        // init the device transform, AA hint, and FM hint
        var transform = new AffineTransform(
                mDesc.getPostScaleX(),
                mDesc.getPostShearY(),
                mDesc.getPostShearX(),
                mDesc.getPostScaleY(),
                0, 0);
        mFont = typeface.getFont()
                .deriveFont(transform)
                .deriveFont(mDesc.getTextSize());
        // we pass the device transform to mFont instead of here
        // because JDK returns metrics back to user space, not in device space
        mFRC = new FontRenderContext(
                null,
                getMaskFormat() != Mask.kBW_Format,
                isLinearMetrics()
        );
    }

    @Override
    protected GlyphMetrics generateMetrics(Glyph glyph) {
        GlyphMetrics metrics = new GlyphMetrics(glyph.getMaskFormat());
        GlyphVector gv = mFont.createGlyphVector(mFRC, new int[]{glyph.getGlyphID()});
        // JDK has a rasterization limit of 1024x1024 for getPixelBounds,
        // gv.getVisualBounds() is equivalent to FT_Outline_Get_BBox
        var pixelBounds = gv.getPixelBounds(null, 0, 0);
        if (pixelBounds.isEmpty()) {
            var bounds = gv.getVisualBounds();
            metrics.mLeft = (float) bounds.getMinX();
            metrics.mTop = (float) bounds.getMinY();
            metrics.mRight = (float) bounds.getMaxX();
            metrics.mBottom = (float) bounds.getMaxY();
        } else {
            metrics.mLeft = (float) pixelBounds.getMinX();
            metrics.mTop = (float) pixelBounds.getMinY();
            metrics.mRight = (float) pixelBounds.getMaxX();
            metrics.mBottom = (float) pixelBounds.getMaxY();
        }
        return metrics;
    }

    @Override
    protected void generateImage(Glyph glyph, Object imageBase, long imageAddress) {
        BufferedImage bufferedImage = new BufferedImage(glyph.getWidth(), glyph.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        var g2d = bufferedImage.createGraphics();

        // JDK will use freetype if text size < 100, otherwise use outline
        GlyphVector gv = mFont.createGlyphVector(mFRC, new int[]{glyph.getGlyphID()});
        g2d.setColor(Color.WHITE);
        g2d.setComposite(AlphaComposite.Src);
        g2d.drawGlyphVector(gv, -glyph.getLeft(), -glyph.getTop());

        var data = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();

        switch (glyph.getMaskFormat()) {
            case Mask.kBW_Format -> {
                PixelUtils.packA8ToBW(
                        data,
                        Unsafe.ARRAY_BYTE_BASE_OFFSET,
                        glyph.getWidth(),
                        imageBase,
                        imageAddress,
                        glyph.getRowBytes(),
                        glyph.getWidth(),
                        glyph.getHeight()
                );
            }
            case Mask.kA8_Format -> {
                PixelUtils.copyImage(
                        data,
                        Unsafe.ARRAY_BYTE_BASE_OFFSET,
                        glyph.getWidth(),
                        imageBase,
                        imageAddress,
                        glyph.getRowBytes(),
                        glyph.getRowBytes(),
                        glyph.getHeight()
                );
            }
        }
    }

    @Override
    protected boolean generatePath(Glyph glyph, Path dst) {
        GlyphVector gv = mFont.createGlyphVector(mFRC, new int[]{glyph.getGlyphID()});
        var outline = gv.getOutline();

        var pi = outline.getPathIterator(null);
        J2DUtils.toPath(pi, dst);

        // even if the glyph has no path, gv.getOutline() will return an empty path,
        // so we cannot distinguish between 'have no path' and 'have empty path',
        // always return true here by assuming it has path
        return true;
    }
}
