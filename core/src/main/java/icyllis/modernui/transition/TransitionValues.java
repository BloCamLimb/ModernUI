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
import icyllis.modernui.view.ViewGroup;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Map;

/**
 * Data structure which holds cached values for the transition.
 * The view field is the target which all the values pertain to.
 * The <code>values</code> field is a map which holds information for fields
 * according to names selected by the transitions. These names should
 * be unique to avoid clobbering values stored by other transitions,
 * such as the convention project:transition_name:property_name. For
 * example, the platform might store a property "alpha" in a transition
 * "Fader" as "modernui:fader:alpha".
 *
 * <p>These values are cached during the
 * {@link Transition#captureStartValues(TransitionValues)}
 * capture} phases of a scene change, once when the start values are captured
 * and again when the end values are captured. These start/end values are then
 * passed into the transitions via the
 * for {@link Transition#createAnimator(ViewGroup, TransitionValues, TransitionValues)} method.</p>
 */
public class TransitionValues {

    /**
     * The View with these values
     */
    @Nonnull
    public final View view;

    /**
     * The set of values tracked by transitions for this scene.
     */
    public final Map<String, Object> values = new Object2ObjectOpenHashMap<>();

    /**
     * The Transitions that targeted this view.
     */
    final ArrayList<Transition> mTargetedTransitions = new ArrayList<>();

    public TransitionValues(@Nonnull View view) {
        this.view = view;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TransitionValues that = (TransitionValues) o;
        return view == that.view && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        int result = view.hashCode();
        result = 31 * result + values.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransitionValues@" + Integer.toHexString(hashCode()) + ":\n");
        sb.append("    view = ").append(view).append("\n");
        sb.append("    values:");
        for (var e : values.entrySet()) {
            sb.append("    ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        return sb.toString();
    }
}
