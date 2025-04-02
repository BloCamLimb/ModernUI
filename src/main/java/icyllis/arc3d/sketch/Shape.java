/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.sketch;

import icyllis.arc3d.core.Rect2f;
import org.jspecify.annotations.NonNull;

import java.awt.geom.PathIterator;

/**
 * Interface for geometric shapes that have area.
 */
public interface Shape {

    /**
     * Similar to {@link java.awt.Shape#getBounds2D()}, but stores
     * the result to dst.
     *
     * @param dest the destination rectangle to store the bounds to
     */
    void getBounds(@NonNull Rect2f dest);

    /**
     * Returns the filling rule for determining the interior of the
     * path.
     *
     * @return the winding rule.
     * @see PathIterator#WIND_EVEN_ODD
     * @see PathIterator#WIND_NON_ZERO
     */
    int getWindingRule();

    @NonNull
    PathIterator getPathIterator();

    default void forEach(@NonNull PathConsumer action) {
        PathIterator pi = getPathIterator();
        float[] coords = new float[6];
        while (!pi.isDone()) {
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO -> action.moveTo(coords[0], coords[1]);
                case PathIterator.SEG_LINETO -> action.lineTo(coords[0], coords[1]);
                case PathIterator.SEG_QUADTO -> action.quadTo(coords, 0);
                case PathIterator.SEG_CUBICTO -> action.cubicTo(coords, 0);
                case PathIterator.SEG_CLOSE -> action.close();
            }
            pi.next();
        }
        action.done();
    }
}
