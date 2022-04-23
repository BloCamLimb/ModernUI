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

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.material.MaterialDrawable;
import icyllis.modernui.math.Rect;
import icyllis.modernui.util.LayoutDirection;
import icyllis.modernui.view.View;

import javax.annotation.Nonnull;

public class SubMenuArrowDrawable extends MaterialDrawable {

    private final int mSize;

    public SubMenuArrowDrawable() {
        mSize = View.dp(24);
    }

    @Override
    public void draw(@Nonnull Canvas canvas) {
        final Rect r = getBounds();
        final float w = r.width();
        final float h = r.height();
        Paint paint = Paint.get();
        paint.setColor(mColor);
        paint.setAlpha(modulateAlpha(paint.getAlpha(), mAlpha));
        if (paint.getAlpha() != 0) {
            boolean mirror = getLayoutDirection() == LayoutDirection.RTL;
            if (mirror) {
                canvas.drawTriangle(14 / 24f * w, 7 / 24f * h, 9 / 24f * w, 12 / 24f * h,
                        14 / 24f * w, 17 / 24f * h, paint);
            } else {
                canvas.drawTriangle(10 / 24f * w, 17 / 24f * h, 15 / 24f * w, 12 / 24f * h,
                        10 / 24f * w, 7 / 24f * h, paint);
            }
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
