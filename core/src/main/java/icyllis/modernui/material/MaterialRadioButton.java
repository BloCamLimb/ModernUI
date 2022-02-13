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
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.graphics.drawable.StateListDrawable;
import icyllis.modernui.math.Rect;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.widget.RadioButton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MaterialRadioButton extends RadioButton {

    private static final int[][] ENABLED_CHECKED_STATES =
            new int[][]{
                    new int[]{R.attr.state_enabled, R.attr.state_checked}, // [0]
                    new int[]{R.attr.state_enabled, -R.attr.state_checked}, // [1]
                    new int[]{-R.attr.state_enabled} // [2]
            };

    private static final int[] COLORS =
            new int[]{
                    0xFFAADCF0,
                    0xFF8A8A8A,
                    0xFF616161
            };

    public MaterialRadioButton() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(CHECKED_STATE_SET, new CheckedDrawable());
        drawable.addState(StateSet.WILD_CARD, new UncheckedDrawable());
        drawable.setEnterFadeDuration(300);
        drawable.setExitFadeDuration(300);
        setButtonDrawable(drawable);
        setButtonTintList(new ColorStateList(ENABLED_CHECKED_STATES, COLORS));
    }

    private static abstract class BaseDrawable extends Drawable {

        final float mRadius;

        private ColorStateList mTint;
        int mColor = ~0;
        int mAlpha = 255;

        public BaseDrawable() {
            mRadius = dp(4);
        }

        static int modulateAlpha(int paintAlpha, int alpha) {
            int scale = alpha + (alpha >>> 7); // convert to 0..256
            return paintAlpha * scale >>> 8;
        }

        @Override
        public void setTintList(@Nullable ColorStateList tint) {
            mTint = tint;
            if (tint != null) {
                mColor = tint.getColorForState(getState(), ~0);
            } else {
                mColor = ~0;
            }
            invalidateSelf();
        }

        @Override
        protected boolean onStateChange(@Nonnull int[] stateSet) {
            if (mTint != null) {
                mColor = mTint.getColorForState(stateSet, ~0);
                return true;
            }
            return false;
        }

        @Override
        public boolean isStateful() {
            return super.isStateful() || (mTint != null && mTint.isStateful());
        }

        @Override
        public boolean hasFocusStateSpecified() {
            return mTint != null && mTint.hasFocusStateSpecified();
        }

        @Override
        public void setAlpha(int alpha) {
            mAlpha = alpha;
            invalidateSelf();
        }

        @Override
        public int getAlpha() {
            return mAlpha;
        }

        @Override
        public int getIntrinsicWidth() {
            return (int) (mRadius * 6);
        }

        @Override
        public int getIntrinsicHeight() {
            return (int) (mRadius * 6);
        }
    }

    private static class CheckedDrawable extends BaseDrawable {

        @Override
        public void draw(@Nonnull Canvas canvas) {
            final Rect r = getBounds();
            float cx = r.exactCenterX();
            float cy = r.exactCenterY();
            Paint paint = Paint.take();
            paint.setColor(mColor);
            paint.setAlpha(modulateAlpha(paint.getAlpha(), mAlpha));
            if (paint.getAlpha() != 0) {
                canvas.drawCircle(cx, cy, mRadius, paint);
                paint.setStyle(Paint.STROKE);
                paint.setStrokeWidth(mRadius * 0.5f);
                canvas.drawCircle(cx, cy, mRadius * 1.6f, paint);
            }
        }
    }

    private static class UncheckedDrawable extends BaseDrawable {

        @Override
        public void draw(@Nonnull Canvas canvas) {
            final Rect r = getBounds();
            float cx = r.exactCenterX();
            float cy = r.exactCenterY();
            Paint paint = Paint.take();
            paint.setColor(mColor);
            paint.setAlpha(modulateAlpha(paint.getAlpha(), mAlpha));
            if (paint.getAlpha() != 0) {
                paint.setStyle(Paint.STROKE);
                paint.setStrokeWidth(mRadius * 0.5f);
                canvas.drawCircle(cx, cy, mRadius * 1.6f, paint);
            }
        }
    }
}
