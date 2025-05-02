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

package icyllis.modernui.material.drawable;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.RadialGradient;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.graphics.Shader;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.material.MaterialDrawable;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.TypedValue;
import org.jetbrains.annotations.ApiStatus;

/**
 * A thumb drawable for SeekBar, similar to Material Design.
 */
@ApiStatus.Internal
public class SeekbarThumbDrawable extends MaterialDrawable {

    private final int mSize; // 18dp
    private final boolean mHasShadowLayer;
    private Shader mShadowShader;

    public SeekbarThumbDrawable(Resources res, boolean hasShadowLayer) {
        mSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP, 18, res.getDisplayMetrics()));
        mHasShadowLayer = hasShadowLayer;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Paint paint = Paint.obtain();
        paint.setColor(mColor);
        paint.setAlpha(ShapeDrawable.modulateAlpha(paint.getAlpha(), mAlpha));
        if (paint.getAlpha() != 0) {
            final Rect r = getBounds();
            if (mHasShadowLayer && mShadowShader != null) {
                paint.setShader(mShadowShader);
                canvas.drawCircle(r.exactCenterX(), r.exactCenterY(), mSize * 0.75f, paint);
                paint.setShader(null);
            }
            canvas.drawCircle(r.exactCenterX(), r.exactCenterY(), mSize * 0.5f, paint);
        }
        paint.recycle();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
        if (bounds.isEmpty()) {
            mShadowShader = null;
        } else if (mHasShadowLayer) {
            float offset = mSize * (1f / 18f * 0.5f);
            float radius = mSize * (21f / 18f * 0.5f);
            float startRatio = 1f - (1.5f / (21f * 0.5f));
            float midRatio = startRatio + ((1f - startRatio) / 2f);
            //TODO I think this can be optimized by only changing the local matrix
            mShadowShader = new RadialGradient(
                    bounds.exactCenterX(), bounds.exactCenterY() + offset,
                    radius,
                    new int[]{0, 0x44000000, 0x14000000, 0},
                    new float[]{0, startRatio, midRatio, 1},
                    Shader.TileMode.CLAMP, null
            );
        }
    }

    @Override
    public int getIntrinsicWidth() {
        // has halo layer
        return mSize * 2;
    }

    @Override
    public int getIntrinsicHeight() {
        // has halo layer
        return mSize * 2;
    }
}
