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

import icyllis.modernui.animation.Animator;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.drawable.Animatable;
import icyllis.modernui.graphics.drawable.Drawable;
import org.jetbrains.annotations.ApiStatus;

/**
 * @hidden
 */
@ApiStatus.Internal
public abstract class BaseIndeterminateDrawable extends Drawable implements Animatable {

    protected int mIndicatorColor = ~0;
    protected int mTrackColor = ~0;
    protected int mAlpha = 255;

    protected Animator mAnimator;

    public void setIndicatorColor(int indicatorColor) {
        if (mIndicatorColor != indicatorColor) {
            mIndicatorColor = indicatorColor;
            invalidateSelf();
        }
    }

    public void setTrackColor(int trackColor) {
        if (mTrackColor != trackColor) {
            mTrackColor = trackColor;
            invalidateSelf();
        }
    }

    public int getIndicatorColor() {
        return mIndicatorColor;
    }

    public int getTrackColor() {
        return mTrackColor;
    }

    @Override
    public void setAlpha(int alpha) {
        if (mAlpha != alpha) {
            mAlpha = alpha;
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
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
