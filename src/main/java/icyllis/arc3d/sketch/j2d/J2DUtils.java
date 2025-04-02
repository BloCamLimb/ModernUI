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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.geom.PathIterator;

public class J2DUtils {

    public static Path toPath(@NonNull PathIterator pi, @Nullable Path dst) {
        if (dst == null) {
            dst = new Path();
        }
        dst.setWindingRule(pi.getWindingRule());
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
}
