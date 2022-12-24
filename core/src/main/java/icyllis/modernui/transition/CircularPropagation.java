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

import icyllis.modernui.graphics.Rect;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A propagation that varies with the distance to the epicenter of the Transition
 * or center of the scene if no epicenter exists. When a View is visible in the
 * start of the transition, Views farther from the epicenter will transition
 * sooner than Views closer to the epicenter. When a View is not in the start
 * of the transition or is not visible at the start of the transition, it will
 * transition sooner when closer to the epicenter and later when farther from
 * the epicenter. This is the default TransitionPropagation used with
 * {@link Explode}.
 */
public class CircularPropagation extends VisibilityPropagation {

    private float mPropagationSpeed = 3.0f;

    /**
     * Sets the speed at which transition propagation happens, relative to the duration of the
     * Transition. A <code>propagationSpeed</code> of 1 means that a View centered farthest from
     * the epicenter and View centered at the epicenter will have a difference
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
        TransitionValues positionValues;
        if (endValues == null || getViewVisibility(startValues) == View.VISIBLE) {
            positionValues = startValues;
            directionMultiplier = -1;
        } else {
            positionValues = endValues;
        }

        int viewCenterX = getViewX(positionValues);
        int viewCenterY = getViewY(positionValues);

        Rect epicenter = transition.getEpicenter();
        int epicenterX;
        int epicenterY;
        if (epicenter != null) {
            epicenterX = epicenter.centerX();
            epicenterY = epicenter.centerY();
        } else {
            int[] loc = new int[2];
            sceneRoot.getLocationInWindow(loc);
            int w = (sceneRoot.getWidth() / 2);
            epicenterX = Math.round(loc[0] + w + sceneRoot.getTranslationX());
            int h = (sceneRoot.getHeight() / 2);
            epicenterY = Math.round(loc[1] + h + sceneRoot.getTranslationY());
        }
        float distance = distance(viewCenterX, viewCenterY, epicenterX, epicenterY);
        float maxDistance = distance(0, 0, sceneRoot.getWidth(), sceneRoot.getHeight());
        float distanceFraction = distance / maxDistance;

        long duration = transition.getDuration();
        if (duration < 0) {
            duration = 300;
        }

        return Math.round(duration * directionMultiplier / mPropagationSpeed * distanceFraction);
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        float x = x2 - x1;
        float y = y2 - y1;
        return (float) Math.sqrt((x * x) + (y * y));
    }
}
