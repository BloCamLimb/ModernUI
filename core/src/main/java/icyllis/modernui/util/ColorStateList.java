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

package icyllis.modernui.util;

import icyllis.modernui.R;
import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.MathUtil;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.concurrent.GuardedBy;
import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * Lets you map {@link icyllis.modernui.view.View} state sets to colors.
 * <p>
 * This defines a set of state spec / color pairs where each state spec specifies a set of
 * states that a view must either be in or not be in and the color specifies the color associated
 * with that spec.
 */
// low-priority: this class should be moved to resources package in 4.0
public class ColorStateList {

    private static final int DEFAULT_COLOR = 0xFFFF0000;
    private static final int[][] EMPTY = new int[][]{new int[0]};

    /**
     * Thread-safe cache of single-color ColorStateLists.
     */
    @GuardedBy("itself")
    private static final SparseArray<WeakReference<ColorStateList>> sCache = new SparseArray<>();

    private final int[][] mStateSpecs;
    private final int[] mColors;
    private int mDefaultColor;
    private boolean mIsOpaque;

    /**
     * Creates a ColorStateList that returns the specified mapping from
     * states to colors. The given arrays should not be modified after construction.
     */
    public ColorStateList(@NonNull int[][] states, @NonNull int[] colors) {
        mStateSpecs = states;
        mColors = colors;

        onColorsChanged();
    }

    /**
     * @return A ColorStateList containing a single color.
     */
    @NonNull
    public static ColorStateList valueOf(@ColorInt int color) {
        synchronized (sCache) {
            final int index = sCache.indexOfKey(color);
            if (index >= 0) {
                final ColorStateList cached = sCache.valueAt(index).get();
                if (cached != null) {
                    return cached;
                }

                // Prune missing entry.
                sCache.removeAt(index);
            }

            // Prune the cache before adding new items.
            final int N = sCache.size();
            for (int i = N - 1; i >= 0; i--) {
                if (sCache.valueAt(i).get() == null) {
                    sCache.removeAt(i);
                }
            }

            final ColorStateList csl = new ColorStateList(EMPTY, new int[]{color});
            sCache.put(color, new WeakReference<>(csl));
            return csl;
        }
    }

    /**
     * Creates a new ColorStateList that has the same states and colors as this
     * one but where each color has the specified alpha value (0-255).
     *
     * @param alpha The new alpha channel value (0-255).
     * @return A new color state list.
     */
    @NonNull
    public ColorStateList withAlpha(int alpha) {
        final int[] colors = new int[mColors.length];
        final int len = colors.length;
        for (int i = 0; i < len; i++) {
            colors[i] = (mColors[i] & 0xFFFFFF) | (alpha << 24);
        }

        return new ColorStateList(mStateSpecs, colors);
    }

    /**
     * Apply alpha modulation to base color.
     *
     * @param baseColor base color
     * @param alphaMod  alpha multiplier applied to the base color
     * @return modulated color
     */
    public static int modulateColor(int baseColor, float alphaMod) {
        if (alphaMod == 1.0f) {
            return baseColor;
        }

        final int baseAlpha = Color.alpha(baseColor);
        final int alpha = MathUtil.clamp((int) (baseAlpha * alphaMod + 0.5f), 0, 255);

        return (baseColor & 0xFFFFFF) | (alpha << 24);
    }

    /**
     * Indicates whether this color state list contains at least one state spec
     * and the first spec is not empty (e.g. match-all).
     *
     * @return True if this color state list changes color based on state, false
     * otherwise.
     * @see #getColorForState(int[], int)
     */
    public boolean isStateful() {
        return mStateSpecs.length >= 1 && mStateSpecs[0].length > 0;
    }

    /**
     * Return whether the state spec list has at least one item explicitly specifying
     */
    @ApiStatus.Internal
    public boolean hasFocusStateSpecified() {
        return StateSet.containsAttribute(mStateSpecs, R.attr.state_focused);
    }

    /**
     * Indicates whether this color state list is opaque, which means that every
     * color returned from {@link #getColorForState(int[], int)} has an alpha
     * value of 255.
     *
     * @return True if this color state list is opaque.
     */
    public boolean isOpaque() {
        return mIsOpaque;
    }

    /**
     * Return the color associated with the given set of
     * {@link icyllis.modernui.view.View} states.
     *
     * @param stateSet     an array of {@link icyllis.modernui.view.View} states
     * @param defaultColor the color to return if there's no matching state
     *                     spec in this {@link ColorStateList} that matches the
     *                     stateSet.
     * @return the color associated with that set of states in this {@link ColorStateList}.
     */
    public int getColorForState(int[] stateSet, int defaultColor) {
        final int setLength = mStateSpecs.length;
        for (int i = 0; i < setLength; i++) {
            final int[] stateSpec = mStateSpecs[i];
            if (StateSet.stateSetMatches(stateSpec, stateSet)) {
                return mColors[i];
            }
        }
        return defaultColor;
    }

    /**
     * Return the default color in this {@link ColorStateList}.
     *
     * @return the default color in this {@link ColorStateList}.
     */
    @ColorInt
    public int getDefaultColor() {
        return mDefaultColor;
    }

    /**
     * Return the states in this {@link ColorStateList}. The returned array
     * should not be modified.
     *
     * @return the states in this {@link ColorStateList}
     * @hidden
     */
    @ApiStatus.Internal
    public int[][] getStates() {
        return mStateSpecs;
    }

    /**
     * Return the colors in this {@link ColorStateList}. The returned array
     * should not be modified.
     *
     * @return the colors in this {@link ColorStateList}
     * @hidden
     */
    @ApiStatus.Internal
    public int[] getColors() {
        return mColors;
    }

    /**
     * Returns whether the specified state is referenced in any of the state
     * specs contained within this ColorStateList.
     * <p>
     * Any reference, either positive or negative {ex. ~R.attr.state_enabled},
     * will cause this method to return {@code true}. Wildcards are not counted
     * as references.
     *
     * @param state the state to search for
     * @return {@code true} if the state if referenced, {@code false} otherwise
     */
    @ApiStatus.Internal
    public boolean hasState(int state) {
        for (final int[] states : mStateSpecs) {
            for (int i : states) {
                if (i == state || i == ~state) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ColorStateList{" +
                "mStateSpecs=" + Arrays.deepToString(mStateSpecs) +
                "mColors=" + Arrays.toString(mColors) +
                "mDefaultColor=" + mDefaultColor + '}';
    }

    /**
     * Updates the default color and opacity.
     */
    private void onColorsChanged() {
        int defaultColor = DEFAULT_COLOR;
        boolean isOpaque = true;

        final int[][] states = mStateSpecs;
        final int[] colors = mColors;
        final int N = states.length;
        if (N > 0) {
            defaultColor = colors[0];

            for (int i = N - 1; i > 0; i--) {
                if (states[i].length == 0) {
                    defaultColor = colors[i];
                    break;
                }
            }

            for (int i = 0; i < N; i++) {
                if (colors[i] >>> 24 != 0xFF) {
                    isOpaque = false;
                    break;
                }
            }
        }

        mDefaultColor = defaultColor;
        mIsOpaque = isOpaque;
    }
}
