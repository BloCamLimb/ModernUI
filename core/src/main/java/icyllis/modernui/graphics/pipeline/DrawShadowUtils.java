/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.pipeline;

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.Matrix4;
import icyllis.arc3d.sketch.Canvas;
import icyllis.arc3d.sketch.Paint;
import icyllis.arc3d.sketch.RRect;
import icyllis.modernui.graphics.Rect;
import org.jetbrains.annotations.ApiStatus;

/**
 * @hidden
 */
@ApiStatus.Internal
public class DrawShadowUtils {

    public static final float kAmbientHeightFactor = 1.0f / 128.0f;
    public static final float kAmbientGeomFactor = 128.0f;
    // Assuming that we have a light height of 600 for the spot shadow,
    // the spot values will reach their maximum at a height of approximately 292.3077.
    // We'll round up to 300 to keep it simple.
    public static final float kMaxAmbientRadius = 300*kAmbientHeightFactor*kAmbientGeomFactor;

    //TODO This value is not ideal and can underestimate the dirty area
    public static final float kOutsetPerZ = 3f;

    private static float divide_and_pin(float numer, float denom, float min, float max) {
        float result = MathUtil.pin(numer / denom, min, max);
        // ensure that pin handled non-finites correctly
        assert (result >= min && result <= max);
        return result;
    }

    public static void drawShadow(Canvas canvas,
                                  Rect rect, float rad,
                                  float zPlane0, float zPlane1, float zPlane2,
                                  float lightX, float lightY, float lightZ,
                                  float lightRadius, int ambientColor, int spotColor) {
        if (!MathUtil.isFinite(lightX, lightY, lightZ,
                zPlane0, zPlane1, zPlane2) || !Float.isFinite(lightRadius)) {
            return;
        }
        if (rect.isEmpty()) {
            return;
        }

        Paint paint = new Paint();
        RRect r2 = new RRect();

        if (Color.alpha(ambientColor) > 0) {
            float devSpaceOutset = Math.min(zPlane2*kAmbientHeightFactor*kAmbientGeomFactor, kMaxAmbientRadius);
            float oneOverA = 1.0f + Math.max(zPlane2*kAmbientHeightFactor, 0.0f);
            float blurRadius = 0.5f*devSpaceOutset*oneOverA;
            float strokeWidth = 0.5f*(devSpaceOutset - blurRadius)*0.5f;

            r2.setRectXY(rect.left-strokeWidth, rect.top-strokeWidth,
                    rect.right+strokeWidth, rect.bottom+strokeWidth,
                    rad+strokeWidth, rad+strokeWidth);
            paint.setColor(ambientColor);
            if (blurRadius > 0) {
                blurRadius = blurRadius * 1.73205080f + 1.0f;
            }
            canvas.drawBlurredRRect(r2, paint, blurRadius, 0.1f);
        }

        if (Color.alpha(spotColor) > 0) {
            float occluderZ = rect.centerX() * zPlane0 + rect.centerY() * zPlane1 + zPlane2;
            float zRatio = divide_and_pin(occluderZ, lightZ - occluderZ, 0.0f, 0.95f);
            float blurRadius = lightRadius * zRatio;
            float scale = divide_and_pin(lightZ, lightZ - occluderZ, 1.0f, 1.95f);

            Matrix4 oldLocalToDevice = new Matrix4();
            canvas.getLocalToDevice(oldLocalToDevice);

            Matrix4 shadowTransform = new Matrix4();
            shadowTransform.setScale(scale, scale, 1);
            shadowTransform.postTranslate(-zRatio * lightX, -zRatio * lightY);
            shadowTransform.preConcat(oldLocalToDevice);

            canvas.setMatrix(shadowTransform);

            r2.setRectXY(rect.left, rect.top, rect.right, rect.bottom,
                    rad, rad);
            paint.setColor(spotColor);
            if (blurRadius > 0) {
                blurRadius = blurRadius * 1.73205080f + 1.0f;
            }
            canvas.drawBlurredRRect(r2, paint, blurRadius, 0.1f);

            canvas.setMatrix(oldLocalToDevice);
        }

        paint.close();
    }
}
