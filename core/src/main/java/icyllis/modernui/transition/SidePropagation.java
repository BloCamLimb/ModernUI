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

package icyllis.modernui.transition;

import icyllis.modernui.math.Rect;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A <code>TransitionPropagation</code> that propagates based on the distance to the side
 * and, orthogonally, the distance to epicenter. If the transitioning View is visible in
 * the start of the transition, then it will transition sooner when closer to the side and
 * later when farther. If the view is not visible in the start of the transition, then
 * it will transition later when closer to the side and sooner when farther from the edge.
 * This is the default TransitionPropagation used with {@link Slide}.
 */
public class SidePropagation extends VisibilityPropagation {

    private float mPropagationSpeed = 3.0f;
    private int mSide = Gravity.BOTTOM;

    /**
     * Sets the side that is used to calculate the transition propagation. If the transitioning
     * View is visible in the start of the transition, then it will transition sooner when
     * closer to the side and later when farther. If the view is not visible in the start of
     * the transition, then it will transition later when closer to the side and sooner when
     * farther from the edge. The default is {@link Gravity#BOTTOM}.
     *
     * @param side The side that is used to calculate the transition propagation. Must be one of
     *             {@link Gravity#LEFT}, {@link Gravity#TOP}, {@link Gravity#RIGHT},
     *             {@link Gravity#BOTTOM}, {@link Gravity#START}, or {@link Gravity#END}.
     */
    public void setSide(@Slide.GravityFlag int side) {
        mSide = side;
    }

    /**
     * Sets the speed at which transition propagation happens, relative to the duration of the
     * Transition. A <code>propagationSpeed</code> of 1 means that a View centered at the side
     * set in {@link #setSide(int)} and View centered at the opposite edge will have a difference
     * in start delay of approximately the duration of the Transition. A speed of 2 means the
     * start delay difference will be approximately half of the duration of the transition. A
     * value of 0 is illegal, but negative values will invert the propagation.
     *
     * @param propagationSpeed The speed at which propagation occurs, relative to the duration
     *                         of the transition. A speed of 4 means it works 4 times as fast
     *                         as the duration of the transition. May not be 0.
     */
    public void setPropagationSpeed(float propagationSpeed) {
        if (propagationSpeed == 0) {
            throw new IllegalArgumentException("propagationSpeed may not be 0");
        }
        mPropagationSpeed = propagationSpeed;
    }

    @Override
    public long getStartDelay(@Nonnull ViewGroup sceneRoot, @Nonnull Transition transition,
                              @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        if (startValues == null && endValues == null) {
            return 0;
        }
        int directionMultiplier = 1;
        Rect epicenter = transition.getEpicenter();
        TransitionValues positionValues;
        if (endValues == null || getViewVisibility(startValues) == View.VISIBLE) {
            positionValues = startValues;
            directionMultiplier = -1;
        } else {
            positionValues = endValues;
        }

        int viewCenterX = getViewX(positionValues);
        int viewCenterY = getViewY(positionValues);

        int[] loc = new int[2];
        sceneRoot.getLocationInWindow(loc);
        int left = loc[0] + Math.round(sceneRoot.getTranslationX());
        int top = loc[1] + Math.round(sceneRoot.getTranslationY());
        int right = left + sceneRoot.getWidth();
        int bottom = top + sceneRoot.getHeight();

        int epicenterX;
        int epicenterY;
        if (epicenter != null) {
            epicenterX = epicenter.centerX();
            epicenterY = epicenter.centerY();
        } else {
            epicenterX = (left + right) / 2;
            epicenterY = (top + bottom) / 2;
        }

        float distance = distance(sceneRoot, viewCenterX, viewCenterY, epicenterX, epicenterY,
                left, top, right, bottom);
        float maxDistance = getMaxDistance(sceneRoot);
        float distanceFraction = distance / maxDistance;

        long duration = transition.getDuration();
        if (duration < 0) {
            duration = 300;
        }

        return Math.round(duration * directionMultiplier / mPropagationSpeed * distanceFraction);
    }

    private int distance(View sceneRoot, int viewX, int viewY, int epicenterX, int epicenterY,
                         int left, int top, int right, int bottom) {
        final int side;
        if (mSide == Gravity.START) {
            final boolean isRtl = sceneRoot.isLayoutRtl();
            side = isRtl ? Gravity.RIGHT : Gravity.LEFT;
        } else if (mSide == Gravity.END) {
            final boolean isRtl = sceneRoot.isLayoutRtl();
            side = isRtl ? Gravity.LEFT : Gravity.RIGHT;
        } else {
            side = mSide;
        }
        return switch (side) {
            case Gravity.LEFT -> right - viewX + Math.abs(epicenterY - viewY);
            case Gravity.TOP -> bottom - viewY + Math.abs(epicenterX - viewX);
            case Gravity.RIGHT -> viewX - left + Math.abs(epicenterY - viewY);
            case Gravity.BOTTOM -> viewY - top + Math.abs(epicenterX - viewX);
            default -> 0;
        };
    }

    private int getMaxDistance(@Nonnull ViewGroup sceneRoot) {
        return switch (mSide) {
            case Gravity.LEFT, Gravity.RIGHT, Gravity.START, Gravity.END -> sceneRoot.getWidth();
            default -> sceneRoot.getHeight();
        };
    }
}
