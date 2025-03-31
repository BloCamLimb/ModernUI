/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.sketch.j2d;

import icyllis.arc3d.sketch.BlendMode;
import icyllis.arc3d.sketch.Matrixc;
import icyllis.arc3d.sketch.Paint;
import icyllis.arc3d.sketch.Path;
import icyllis.arc3d.sketch.PathUtils;
import org.jetbrains.annotations.ApiStatus;

import java.awt.*;
import java.awt.Color;
import java.awt.geom.AffineTransform;

//TODO
@ApiStatus.Internal
public class DrawBase {

    public Graphics2D mG2D;
    // perspective is not supported
    public Matrixc mCTM;
    public Shape mClip;

    // const args
    public void drawPath(Path path, Paint paint) {
        boolean doFill = true;

        Path tmpPath = new Path();

        if (paint.getStyle() != Paint.FILL ||
                paint.getPathEffect() != null) {
            doFill = PathUtils.fillPathWithPaint(
                    path, paint, tmpPath, null, mCTM
            );
            path = tmpPath;
        }

        path.transform(mCTM, tmpPath);

        drawDevicePath(tmpPath, paint, doFill);
        tmpPath.recycle();
    }

    private static final float MAX_FOR_MATH = Float.MAX_VALUE * 0.25f;
    private static final BasicStroke sHairlineStroke = new BasicStroke(0);

    // const args
    private void drawDevicePath(Path path,
                                Paint paint,
                                boolean doFill) {
        var bounds = path.getBounds();
        // use ! expression so we return true if bounds contains NaN
        if (!(bounds.left() >= -MAX_FOR_MATH && bounds.top() >= -MAX_FOR_MATH &&
                bounds.right() <= MAX_FOR_MATH && bounds.bottom() <= MAX_FOR_MATH)) {
            return;
        }

        var j2dPath = J2DUtils.toPath2D(path, null);

        mG2D.setTransform(new AffineTransform());
        preparePaint(paint);

        if (doFill) {
            mG2D.fill(j2dPath);
        } else {
            mG2D.setStroke(sHairlineStroke);
            mG2D.draw(j2dPath);
        }
    }

    private void preparePaint(Paint paint) {
        mG2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                paint.isAntiAlias() ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        mG2D.setRenderingHint(RenderingHints.KEY_DITHERING,
                paint.isDither() ? RenderingHints.VALUE_DITHER_ENABLE : RenderingHints.VALUE_DITHER_DEFAULT);
        mG2D.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        mG2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        mG2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        mG2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        mG2D.setColor(new Color(paint.r(), paint.g(), paint.b(), paint.a()));
        var bm = paint.getBlendMode();
        if (bm == null) {
            bm = BlendMode.SRC_OVER;
        }
        var comp = switch (bm) {
            case CLEAR -> AlphaComposite.Clear;
            case SRC -> AlphaComposite.Src;
            case DST -> AlphaComposite.Dst;
            case SRC_OVER -> AlphaComposite.SrcOver;
            case DST_OVER -> AlphaComposite.DstOver;
            case SRC_IN -> AlphaComposite.SrcIn;
            case DST_IN -> AlphaComposite.DstIn;
            case SRC_OUT -> AlphaComposite.SrcOut;
            case DST_OUT -> AlphaComposite.DstOut;
            case SRC_ATOP -> AlphaComposite.SrcAtop;
            case DST_ATOP -> AlphaComposite.DstAtop;
            case XOR -> AlphaComposite.Xor;
            default -> AlphaComposite.SrcOver;
        };
        mG2D.setComposite(comp);
    }
}
