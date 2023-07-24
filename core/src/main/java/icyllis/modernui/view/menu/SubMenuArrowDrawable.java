/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.view.menu;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.*;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.material.MaterialDrawable;
import icyllis.modernui.util.LayoutDirection;
import icyllis.modernui.util.TypedValue;

import javax.annotation.Nonnull;
import java.nio.FloatBuffer;

public class SubMenuArrowDrawable extends MaterialDrawable {

    private final int mSize;
    private final FloatBuffer mPoints;

    public SubMenuArrowDrawable(Context context) {
        mSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP,
                24, context.getResources().getDisplayMetrics());
        mPoints = FloatBuffer.allocate(6);
    }

    @Override
    public void draw(@Nonnull Canvas canvas) {
        Paint paint = Paint.obtain();
        paint.setColor(mColor);
        paint.setAlpha(ShapeDrawable.modulateAlpha(paint.getAlpha(), mAlpha));
        if (paint.getAlpha() != 0) {
            canvas.drawTriangleListMesh(mPoints, /*color*/null, paint);
        }
        paint.recycle();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        buildArrowPoints(bounds.width(), bounds.height(), getLayoutDirection());
    }

    @Override
    protected boolean onLayoutDirectionChanged(int layoutDirection) {
        final Rect bounds = getBounds();
        buildArrowPoints(bounds.width(), bounds.height(), layoutDirection);
        return true;
    }

    private void buildArrowPoints(float w, float h, int layoutDirection) {
        boolean mirror = layoutDirection == LayoutDirection.RTL;
        if (mirror) {
            mPoints.put(16 / 24f * w).put(7 / 24f * h).put(8 / 24f * w).put(12 / 24f * h)
                    .put(16 / 24f * w).put(17 / 24f * h).flip();
        } else {
            mPoints.put(8 / 24f * w).put(17 / 24f * h).put(16 / 24f * w).put(12 / 24f * h)
                    .put(8 / 24f * w).put(7 / 24f * h).flip();
        }
    }

    @Override
    public boolean isAutoMirrored() {
        return true;
    }

    @Override
    public int getIntrinsicWidth() {
        return mSize;
    }

    @Override
    public int getIntrinsicHeight() {
        return mSize;
    }
}
