/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.Rect2i;

/**
 * {@link Clip} is an abstract base class for producing a clip. It constructs a
 * clip mask if necessary, and fills out a {@link ClipResult} instructing the
 * caller on how to set up the draw state.
 */
public abstract class Clip {

    public static final int CLIPPED = 0;
    public static final int NOT_CLIPPED = 1;
    public static final int CLIPPED_OUT = 2;

    public abstract int apply(SurfaceDrawContext sdc,
                              boolean aa, ClipResult out,
                              Rect2f bounds);

    /**
     * Compute a conservative pixel bounds restricted to the given render target dimensions.
     * The returned bounds represent the limits of pixels that can be drawn; anything outside the
     * bounds will be entirely clipped out.
     */
    public abstract void getConservativeBounds(Rect2i out);

    /**
     * This is the maximum distance that a draw may extend beyond a clip's boundary and still count
     * count as "on the other side". We leave some slack because floating point rounding error is
     * likely to blame. The rationale for 1e-3 is that in the coverage case (and barring unexpected
     * rounding), as long as coverage stays within 0.5 * 1/256 of its intended value it shouldn't
     * have any effect on the final pixel values.
     */
    public static final float kBoundsTolerance = 1e-3f;

    /**
     * This is the slack around a half-pixel vertex coordinate where we don't trust the GPU's
     * rasterizer to round consistently. The rounding method is not defined in GPU specs, and
     * rasterizer precision frequently introduces errors where a fraction < 1/2 still rounds up.
     * <p>
     * For non-AA bounds edges, an edge value between 0.45 and 0.55 will round in or round out
     * depending on what side its on. Outside of this range, the non-AA edge will snap using round()
     */
    public static final float kHalfPixelRoundingTolerance = 5e-2f;

    /**
     * Convert the analytic bounds of a shape into an integer pixel bounds, where the given aa type
     * is used when the shape is rendered. The bounds mode can be used to query exterior or interior
     * pixel boundaries. Interior bounds only make sense when its know that the analytic bounds
     * are filled completely.
     * <p>
     * NOTE: When using kExterior_Bounds, some coverage-AA rendering methods may still touch a pixel
     * center outside of these bounds but will evaluate to 0 coverage. This is visually acceptable,
     * but an additional outset of 1px should be used for dst proxy access.
     */
    public static void getPixelBounds(Rect2f bounds, boolean aa,
                                      boolean exterior, Rect2i out) {
        if (bounds.isEmpty()) {
            out.setEmpty();
            return;
        }
        if (exterior) {
            out.set(roundLow(aa, bounds.mLeft), roundLow(aa, bounds.mTop),
                    roundHigh(aa, bounds.mRight), roundHigh(aa, bounds.mBottom));
        } else {
            out.set(roundHigh(aa, bounds.mLeft), roundHigh(aa, bounds.mTop),
                    roundLow(aa, bounds.mRight), roundLow(aa, bounds.mBottom));
        }
    }

    private static int roundLow(boolean aa, float v) {
        v += kBoundsTolerance;
        return aa
                ? (int) Math.floor(v)
                : Math.round(v - kHalfPixelRoundingTolerance);
    }

    private static int roundHigh(boolean aa, float v) {
        v -= kBoundsTolerance;
        return aa
                ? (int) Math.ceil(v)
                : Math.round(v + kHalfPixelRoundingTolerance);
    }
}
