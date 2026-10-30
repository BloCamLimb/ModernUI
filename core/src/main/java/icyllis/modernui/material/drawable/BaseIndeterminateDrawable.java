/*
 * ModernUI.
 * Copyright (C) 2025-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.material.drawable;

import icyllis.modernui.animation.Animator;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.drawable.Animatable;
import icyllis.modernui.graphics.drawable.Drawable;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/**
 * @hidden
 */
@ApiStatus.Internal
public abstract class BaseIndeterminateDrawable extends Drawable implements Animatable {

    protected long mIndicatorColor = Color.WHITE_LONG;
    protected long mTrackColor = Color.WHITE_LONG;
    protected float mAlpha = 1f;

    protected Animator mAnimator;

    public void setIndicatorColor(long indicatorColor) {
        if (mIndicatorColor != indicatorColor) {
            mIndicatorColor = indicatorColor;
            invalidateSelf();
        }
    }

    public void setTrackColor(long trackColor) {
        if (mTrackColor != trackColor) {
            mTrackColor = trackColor;
            invalidateSelf();
        }
    }

    public long getIndicatorColor() {
        return mIndicatorColor;
    }

    public long getTrackColor() {
        return mTrackColor;
    }

    @Override
    public void setAlpha(float alpha) {
        if (mAlpha != alpha) {
            mAlpha = alpha;
            invalidateSelf();
        }
    }

    @Override
    public float getAlpha() {
        return mAlpha;
    }

    @Override
    public void start() {
        if (mAnimator == null) {
            mAnimator = createAnimator();
        }
        mAnimator.start();
    }

    @Override
    public void stop() {
        if (mAnimator != null) {
            mAnimator.end();
        }
    }

    @Override
    public boolean isRunning() {
        return mAnimator != null && mAnimator.isRunning();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        if (mAnimator != null && mAnimator.isStarted()) {
            if (visible) {
                mAnimator.resume();
            } else {
                mAnimator.pause();
            }
        }
        return super.setVisible(visible, restart);
    }

    @NonNull
    protected abstract Animator createAnimator();
}
