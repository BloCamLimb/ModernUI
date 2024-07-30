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

package icyllis.arc3d.core;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PathUtils {

    /**
     * Returns the filled equivalent of the stroked path.
     *
     * @param src      Path read to create a filled version
     * @param paint    Paint, from which attributes such as stroke cap, width, miter, and join,
     *                 as well as pathEffect will be used.
     * @param dst      resulting Path; may be the same as src, but may not be nullptr
     * @param cullRect optional limit passed to PathEffect
     * @param resScale if > 1, increase precision, else if (0 < resScale < 1) reduce precision
     *                 to favor speed and size
     * @return true if the dst path was updated, false if it was not (e.g. if the path
     * represents hairline and cannot be filled).
     */
    public static boolean fillPathWithPaint(
            @Nonnull Path src, @Nonnull Paint paint,
            @Nonnull Path dst, @Nullable Rect2fc cullRect,
            float resScale) {
        return fillPathWithPaint(src, paint, dst, cullRect,
                Matrix.makeScale(resScale, resScale));
    }

    public static boolean fillPathWithPaint(
            @Nonnull Path src, @Nonnull Paint paint,
            @Nonnull Path dst, @Nullable Rect2fc cullRect,
            @Nonnull Matrixc ctm) {
        if (!src.isFinite()) {
            dst.reset();
            return false;
        }

        float resScale;
        if (ctm.hasPerspective()) {
            var transformedBounds = new Rect2f();
            ctm.mapRect(src.getBounds(), transformedBounds);
            resScale = ctm.getMaxScale(
                    transformedBounds.centerX(),
                    transformedBounds.centerY()
            );
        } else {
            resScale = ctm.getMaxScale();
        }
        if (resScale <= MathUtil.EPS || !Float.isFinite(resScale)) {
            // capture negative, approx zero, infinity, NaN
            resScale = 1;
        }
        Stroke stroke = new Stroke(paint, resScale);

        //TODO path effect

        if (!stroke.applyToPath(src, dst)) {
            dst.set(src);
        }

        if (!dst.isFinite()) {
            dst.reset();
            return false;
        }
        return !stroke.isHairlineStyle();
    }
}
