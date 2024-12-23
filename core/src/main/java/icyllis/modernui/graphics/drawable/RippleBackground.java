/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.drawable;

import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.TimeInterpolator;
import icyllis.modernui.graphics.*;
import icyllis.modernui.util.FloatProperty;

/**
 * Draws a ripple background.
 */
// Modified from Android
class RippleBackground extends RippleComponent {

    static final int OPACITY_DURATION = 80;

    private ObjectAnimator mAnimator;

    private float mOpacity = 0;

    private boolean mFocused = false;
    private boolean mHovered = false;

    RippleBackground(RippleDrawable owner, Rect bounds) {
        super(owner, bounds);
    }

    public boolean isVisible() {
        return mOpacity > 0;
    }

    public void draw(Canvas c, Paint p) {
        final int origAlpha = p.getAlpha();
        final int alpha = Math.min((int) (origAlpha * mOpacity + 0.5f), 255);
        if (alpha > 0) {
            p.setAlpha(alpha);
            c.drawCircle(0, 0, mTargetRadius, p);
            p.setAlpha(origAlpha);
        }
    }

    public void setState(boolean focused, boolean hovered, boolean pressed) {
        if (!mFocused) {
            focused = focused && !pressed;
        }
        if (!mHovered) {
            hovered = hovered && !pressed;
        }
        if (mHovered != hovered || mFocused != focused) {
            mHovered = hovered;
            mFocused = focused;
            onStateChanged();
        }
    }

    private void onStateChanged() {
        // Hover             = .3 * alpha
        // Focus             = .7 * alpha
        // Focused + Hovered = .7 * alpha
        float newOpacity = mFocused ? .7f : mHovered ? .3f : 0f;
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        mAnimator = ObjectAnimator.ofFloat(this, OPACITY, newOpacity);
        mAnimator.setDuration(OPACITY_DURATION);
        mAnimator.setInterpolator(TimeInterpolator.LINEAR);
        mAnimator.start();
    }

    public void end() {
        if (mAnimator != null) {
            mAnimator.end();
            mAnimator = null;
        }
    }

    private static final FloatProperty<RippleBackground> OPACITY = new FloatProperty<>("opacity") {
        @Override
        public void setValue(RippleBackground object, float value) {
            object.mOpacity = value;
            object.invalidateSelf();
        }

        @Override
        public Float get(RippleBackground object) {
            return object.mOpacity;
        }
    };
}
