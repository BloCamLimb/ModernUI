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

package icyllis.modernui.material;

import icyllis.modernui.R;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.drawable.StateListDrawable;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.widget.CheckBox;

import javax.annotation.Nonnull;

public class MaterialCheckBox extends CheckBox {

    private static final int[][] ENABLED_CHECKED_STATES = {
            new int[]{R.attr.state_enabled, R.attr.state_checked}, // [0]
            new int[]{R.attr.state_enabled, -R.attr.state_checked}, // [1]
            StateSet.WILD_CARD // [2]
    };

    private static final int[] COLORS = {
            0xFFAADCF0,
            0xFF8A8A8A,
            0xFF616161
    };

    public MaterialCheckBox() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(CHECKED_STATE_SET, new CheckedDrawable());
        drawable.addState(StateSet.WILD_CARD, new UncheckedDrawable());
        drawable.setEnterFadeDuration(300);
        drawable.setExitFadeDuration(300);
        setButtonDrawable(drawable);
        setButtonTintList(new ColorStateList(ENABLED_CHECKED_STATES, COLORS));
    }

    private static class CheckedDrawable extends MaterialDrawable {

        private final float mRadius;

        CheckedDrawable() {
            mRadius = dp(4);
        }

        @Override
        public void draw(@Nonnull Canvas canvas) {
            final Rect r = getBounds();
            Paint paint = Paint.get();
            paint.setColor(mColor);
            paint.setAlpha(modulateAlpha(paint.getAlpha(), mAlpha));
            int alpha = paint.getAlpha();
            if (alpha != 0) {
                float inner = mRadius * 0.5f;
                paint.setStyle(Paint.STROKE);
                paint.setStrokeWidth(mRadius * 0.75f);
                canvas.drawRoundRect(r.left + inner, r.top + inner, r.right - inner,
                        r.bottom - inner, mRadius, paint);
                if (alpha != 0xFF) {
                    paint.setAlpha(0xFF);
                    canvas.saveLayer(r.left, r.top, r.right, r.bottom, alpha);
                }
                canvas.drawRoundLine(mRadius * 1.5f, mRadius * 3.5f, mRadius * 2.5f, mRadius * 4.5f, paint);
                canvas.drawRoundLine(mRadius * 2.5f, mRadius * 4.5f, mRadius * 4.5f, mRadius * 2f, paint);
                if (alpha != 0xFF) {
                    canvas.restore();
                }
            }
        }

        @Override
        public int getIntrinsicWidth() {
            // 24dp
            return (int) (mRadius * 6);
        }

        @Override
        public int getIntrinsicHeight() {
            // 24dp
            return (int) (mRadius * 6);
        }
    }

    private static class UncheckedDrawable extends MaterialDrawable {

        private final float mRadius;

        UncheckedDrawable() {
            mRadius = dp(4);
        }

        @Override
        public void draw(@Nonnull Canvas canvas) {
            final Rect r = getBounds();
            Paint paint = Paint.get();
            paint.setColor(mColor);
            paint.setAlpha(modulateAlpha(paint.getAlpha(), mAlpha));
            if (paint.getAlpha() != 0) {
                float inner = mRadius * 0.5f;
                paint.setStyle(Paint.STROKE);
                paint.setStrokeWidth(mRadius * 0.75f);
                canvas.drawRoundRect(r.left + inner, r.top + inner, r.right - inner,
                        r.bottom - inner, mRadius, paint);
            }
        }

        @Override
        public int getIntrinsicWidth() {
            // 24dp
            return (int) (mRadius * 6);
        }

        @Override
        public int getIntrinsicHeight() {
            // 24dp
            return (int) (mRadius * 6);
        }
    }
}
