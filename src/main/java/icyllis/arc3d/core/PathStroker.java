/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.core;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

/**
 * Stroker is a {@link PathConsumer} that converts paths by stroking paths.
 * This is invoked when a {@link Path} is drawn in a canvas with the
 * {@link Paint#STROKE} bit set in the paint. The new path consists of
 * contours, and the style change from thick stroke to fill.
 */
public class PathStroker implements PathConsumer {

    private PathConsumer mOut;

    public void init(@Nonnull PathConsumer out) {
        assert out != this;
        mOut = out;
    }

    @Override
    public void moveTo(float x, float y) {

    }

    @Override
    public void lineTo(float x, float y) {

    }

    @Override
    public void quadTo(float x1, float y1, float x2, float y2) {

    }

    @Override
    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {

    }

    @Override
    public void closePath() {

    }

    @Override
    public void pathDone() {

    }

    public interface CapFactory {

        void drawCap(
                PathConsumer path,
                float pivotX,
                float pivotY,
                float normalX,
                float normalY,
                float stopX,
                float stopY
        );

        static CapFactory get(int cap) {
            return switch (cap) {
                case Paint.CAP_BUTT -> CapFactory::drawButtCap;
                case Paint.CAP_ROUND -> CapFactory::drawRoundCap;
                case Paint.CAP_SQUARE -> CapFactory::drawSquareCap;
                default -> throw new AssertionError(cap);
            };
        }

        static void drawButtCap(
                PathConsumer path,
                float pivotX,
                float pivotY,
                float normalX,
                float normalY,
                float stopX,
                float stopY) {
            path.lineTo(stopX, stopY);
        }

        // degree = 90, kappa = (4/3) * (sqrt(2) - 1)
        float KAPPA = 4 * (MathUtil.SQRT2 - 1) / 3;

        static void drawRoundCap(
                PathConsumer path,
                float pivotX,
                float pivotY,
                float normalX,
                float normalY,
                float stopX,
                float stopY) {
            // two 1/4 arcs, clockwise
            final float Kmx = KAPPA * normalX;
            final float Kmy = KAPPA * normalY;
            path.cubicTo(
                    pivotX + normalX - Kmy, pivotY + normalY + Kmx,
                    pivotX - normalY + Kmx, pivotY + normalX + Kmy,
                    pivotX - normalY, pivotY + normalX
            );
            path.cubicTo(
                    pivotX - normalY - Kmx, pivotY + normalX - Kmy,
                    pivotX - normalX - Kmy, pivotY - normalY + Kmx,
                    stopX, stopY
            );
            assert stopX == pivotX - normalX;
            assert stopY == pivotY - normalY;
        }

        static void drawSquareCap(
                PathConsumer path,
                float pivotX,
                float pivotY,
                float normalX,
                float normalY,
                float stopX,
                float stopY) {
            // clockwise
            path.lineTo(pivotX + normalX - normalY, pivotY + normalY + normalX);
            path.lineTo(pivotX - normalX - normalY, pivotY - normalY + normalX);
            path.lineTo(stopX, stopY);
        }
    }

    public interface JoinFactory {

        void drawJoin(
                PathConsumer outer,
                PathConsumer inner,
                float beforeUnitNormalX,
                float beforeUnitNormalY,
                float pivotX,
                float pivotY,
                float afterUnitNormalX,
                float afterUnitNormalY,
                float radius,
                float invMiterLimit,
                boolean prevIsLine,
                boolean currIsLine
        );

        @Contract(pure = true)
        static boolean isClockwise(float beforeX, float beforeY,
                                   float afterX, float afterY) {
            return beforeX * afterY > beforeY * afterX;
        }
    }
}
