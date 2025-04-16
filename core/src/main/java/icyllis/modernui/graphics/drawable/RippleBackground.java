/*
 * Modern UI.
 * Copyright (C) 2024-2025 BloCamLimb. All rights reserved.
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
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.Rect;
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

    public void draw(@NonNull Canvas c, float cx, float cy, @NonNull Paint p,
                     @Nullable Rect maskRect, @Nullable float[] maskRadii) {
        final float origAlpha = p.getAlphaF();
        final float alpha = origAlpha * mOpacity;
        if (alpha > 0) {
            p.setAlphaF(alpha);
            // mask is used in bounded case, where targetRadius should cover the full bounds,
            // then we draw the mask and ignore the circle with targetRadius
            if (maskRect != null) {
                if (maskRadii != null) {
                    // matching behavior in ShapeDrawable
                    if (maskRadii[0] == maskRadii[1] &&
                            maskRadii[0] == maskRadii[2] &&
                            maskRadii[0] == maskRadii[3]) {
                        float rad = Math.min(maskRadii[0],
                                Math.min(maskRect.width(), maskRect.height()) * 0.5f);
                        c.drawRoundRect(maskRect.left, maskRect.top, maskRect.right, maskRect.bottom,
                                rad, p);
                    } else {
                        c.drawRoundRect(maskRect.left, maskRect.top, maskRect.right, maskRect.bottom,
                                maskRadii[0], maskRadii[1], maskRadii[2], maskRadii[3], p);
                    }
                } else {
                    c.drawRect(maskRect, p);
                }
            } else {
                c.drawCircle(cx, cy, mTargetRadius, p);
            }
            p.setAlphaF(origAlpha);
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
        float newOpacity = mFocused || mHovered ? 1.0f : 0.0f;
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
