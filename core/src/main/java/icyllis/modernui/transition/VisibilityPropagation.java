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

import icyllis.modernui.view.View;

import javax.annotation.Nonnull;

/**
 * Base class for <code>TransitionPropagation</code>s that care about
 * View Visibility and the center position of the View.
 */
public abstract class VisibilityPropagation extends TransitionPropagation {

    /**
     * The property key used for {@link View#getVisibility()}.
     */
    private static final String PROPNAME_VISIBILITY = "modernui:visibilityPropagation:visibility";

    /**
     * The property key used for the center of the View in screen coordinates. This is an
     * int[2] with the index 0 taking the x coordinate and index 1 taking the y coordinate.
     */
    private static final String PROPNAME_VIEW_CENTER = "modernui:visibilityPropagation:center";

    private static final String[] VISIBILITY_PROPAGATION_VALUES = {
            PROPNAME_VISIBILITY,
            PROPNAME_VIEW_CENTER,
    };

    @Override
    public void captureValues(@Nonnull TransitionValues values) {
        View view = values.view;
        Integer visibility = (Integer) values.values.get(Visibility.PROPNAME_VISIBILITY);
        if (visibility == null) {
            visibility = view.getVisibility();
        }
        values.values.put(PROPNAME_VISIBILITY, visibility);
        int[] loc = new int[2];
        view.getLocationInWindow(loc);
        loc[0] += Math.round(view.getTranslationX());
        loc[0] += view.getWidth() / 2;
        loc[1] += Math.round(view.getTranslationY());
        loc[1] += view.getHeight() / 2;
        values.values.put(PROPNAME_VIEW_CENTER, loc);
    }

    @Override
    public String[] getPropagationProperties() {
        return VISIBILITY_PROPAGATION_VALUES;
    }

    /**
     * Returns {@link View#getVisibility()} for the View at the time the values
     * were captured.
     * @param values The TransitionValues captured at the start or end of the Transition.
     * @return {@link View#getVisibility()} for the View at the time the values
     * were captured.
     */
    public int getViewVisibility(TransitionValues values) {
        if (values == null) {
            return View.GONE;
        }
        Integer visibility = (Integer) values.values.get(PROPNAME_VISIBILITY);
        if (visibility == null) {
            return View.GONE;
        }
        return visibility;
    }

    /**
     * Returns the View's center x coordinate, relative to the screen, at the time the values
     * were captured.
     * @param values The TransitionValues captured at the start or end of the Transition.
     * @return the View's center x coordinate, relative to the screen, at the time the values
     * were captured.
     */
    public int getViewX(TransitionValues values) {
        return getViewCoordinate(values, 0);
    }

    /**
     * Returns the View's center y coordinate, relative to the screen, at the time the values
     * were captured.
     * @param values The TransitionValues captured at the start or end of the Transition.
     * @return the View's center y coordinate, relative to the screen, at the time the values
     * were captured.
     */
    public int getViewY(TransitionValues values) {
        return getViewCoordinate(values, 1);
    }

    private static int getViewCoordinate(TransitionValues values, int coordinateIndex) {
        if (values == null) {
            return -1;
        }

        int[] coordinates = (int[]) values.values.get(PROPNAME_VIEW_CENTER);
        if (coordinates == null) {
            return -1;
        }

        return coordinates[coordinateIndex];
    }
}
