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

import icyllis.arc3d.sketch.Path;
import icyllis.arc3d.sketch.PathConsumer;
import icyllis.arc3d.sketch.PathIterable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

public class J2DUtils {

    public static class J2DPathConverter implements PathConsumer {

        private Path2D mDst;

        public Path2D convert(PathIterable src, Path2D dst) {
            mDst = dst;
            src.forEach(this);
            mDst = null;
            return dst;
        }

        @Override
        public void moveTo(float x, float y) {
            mDst.moveTo(x, y);
        }

        @Override
        public void lineTo(float x, float y) {
            mDst.lineTo(x, y);
        }

        @Override
        public void quadTo(float x1, float y1, float x2, float y2) {
            mDst.quadTo(x1, y1, x2, y2);
        }

        @Override
        public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            mDst.curveTo(x1, y1, x2, y2, x3, y3);
        }

        @Override
        public void close() {
            mDst.closePath();
        }

        @Override
        public void done() {
        }
    }

    //TODO add a PathAdapter that implements Shape interface
    @NonNull
    public static Path2D toPath2D(@NonNull Path src, @Nullable Path2D dst) {
        int winding = toWindingRule(src.getFillRule());
        if (dst == null) {
            dst = new Path2D.Float(winding, src.countVerbs());
        } else {
            dst.reset();
            dst.setWindingRule(winding);
        }
        return new J2DPathConverter().convert(src, dst);
    }

    public static Path toPath(@NonNull PathIterator pi, @Nullable Path dst) {
        if (dst == null) {
            dst = new Path();
        }
        dst.setFillRule(J2DUtils.toFillRule(pi.getWindingRule()));
        float[] coords = new float[6];
        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO -> {
                    dst.moveTo(coords[0], coords[1]);
                }
                case PathIterator.SEG_LINETO -> {
                    dst.lineTo(coords[0], coords[1]);
                }
                case PathIterator.SEG_QUADTO -> {
                    dst.quadTo(coords[0], coords[1], coords[2], coords[3]);
                }
                case PathIterator.SEG_CUBICTO -> {
                    dst.cubicTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                }
                case PathIterator.SEG_CLOSE -> {
                    dst.close();
                }
            }
            pi.next();
        }
        return dst;
    }

    public static int toWindingRule(int fillRule) {
        return switch (fillRule) {
            case Path.FILL_NON_ZERO -> Path2D.WIND_NON_ZERO;
            case Path.FILL_EVEN_ODD -> Path2D.WIND_EVEN_ODD;
            default -> throw new AssertionError(fillRule);
        };
    }

    @Path.FillRule
    public static int toFillRule(int windingRule) {
        return switch (windingRule) {
            case Path2D.WIND_NON_ZERO -> Path.FILL_NON_ZERO;
            case Path2D.WIND_EVEN_ODD -> Path.FILL_EVEN_ODD;
            default -> throw new AssertionError(windingRule);
        };
    }
}
