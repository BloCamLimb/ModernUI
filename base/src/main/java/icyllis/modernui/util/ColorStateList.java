/*
 * ModernUI.
 * Copyright (C) 2022-2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.util;

import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.ColorLong;
import icyllis.modernui.annotation.Immutable;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.ComplexColor;
import icyllis.modernui.graphics.MathUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

/**
 * Lets you map {@link StateSet state sets} to colors.
 * <p>
 * This defines a set of state spec / color pairs where each state spec specifies a set of
 * states that a view must either be in or not be in and the color specifies the color associated
 * with that spec.
 */
@Immutable
//TODO: this class should be moved to graphics package and renamed to StateListColor in 4.0
public class ColorStateList extends ComplexColor {

    private static final long DEFAULT_COLOR = Color.RED_SRGB;
    private static final int[][] EMPTY = new int[][]{StateSet.WILD_CARD};

    private static final int IS_OPAQUE = 1 << 31;
    private static final int DEFAULT_COLOR_INDEX = (1 << 31) - 1;

    private final int[][] mStateSpecs;
    private final long[] mColors;
    private final int mFlags;

    /**
     * Creates a ColorStateList that returns the specified mapping from
     * stateSpecs to colors. The given arrays must NOT be modified after construction.
     */
    public ColorStateList(int @NonNull[] @NonNull[] stateSpecs, @ColorInt int @NonNull[] colors) {
        this(stateSpecs, convertColors(colors));
    }

    private static long @NonNull [] convertColors(int @NonNull [] colors) {
        long[] wideColors = new long[colors.length];
        for (int i = 0; i < wideColors.length; i++) {
            wideColors[i] = Color.pack(colors[i]);
        }
        return wideColors;
    }

    /**
     * Creates a ColorStateList that returns the specified mapping from
     * stateSpecs to colors. The given arrays must NOT be modified after construction.
     */
    public ColorStateList(int @NonNull[] @NonNull[] stateSpecs, @ColorLong long @NonNull[] colors) {
        assert colors.length >= stateSpecs.length;
        mStateSpecs = stateSpecs;
        mColors = colors;

        int defaultColorIndex = -1;
        boolean isOpaque = true;

        final int N = stateSpecs.length;
        if (N > 0) {
            defaultColorIndex = 0;

            for (int i = N - 1; i > 0; i--) {
                if (stateSpecs[i].length == 0) {
                    defaultColorIndex = i;
                    break;
                }
            }

            for (int i = 0; i < N; i++) {
                if (Color.alpha(colors[i]) != 1.0f) {
                    isOpaque = false;
                    break;
                }
            }
        }

        // if default color index is negative, both IS_OPAQUE and DEFAULT_COLOR_INDEX are set
        mFlags = (isOpaque ? IS_OPAQUE : 0) | defaultColorIndex;
    }

    /**
     * @return A ColorStateList containing a single color.
     */
    @NonNull
    public static ColorStateList valueOf(@ColorInt int color) {
        return valueOf(Color.pack(color));
    }

    /**
     * @return A ColorStateList containing a single color.
     */
    @NonNull
    public static ColorStateList valueOf(@ColorLong long color) {
        return new ColorStateList(EMPTY, new long[]{color});
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
        return withAlpha(alpha * (1/255f));
    }

    /**
     * Creates a new ColorStateList that has the same states and colors as this
     * one but where each color has the specified alpha value (0.0-1.0).
     *
     * @param alpha The new alpha channel value (0.0-1.0).
     * @return A new color state list.
     */
    @NonNull
    public ColorStateList withAlpha(float alpha) {
        alpha = MathUtil.clamp(alpha, 0f, 1f);

        final long[] colors = new long[mColors.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.withAlpha(mColors[i], alpha);
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
     * @see #getColorForState(int[], long)
     */
    @Override
    public boolean isStateful() {
        return mStateSpecs.length >= 1 && mStateSpecs[0].length > 0;
    }

    /**
     * Return whether the state spec list has at least one item explicitly specifying
     */
    @ApiStatus.Internal
    public boolean hasFocusStateSpecified() {
        return StateSet.containsAttribute(mStateSpecs, StateSet.state_focused);
    }

    /**
     * Indicates whether this color state list is opaque, which means that every
     * color returned from {@link #getColorForState(int[], long)} has an alpha
     * value of 255.
     *
     * @return True if this color state list is opaque.
     */
    @Override
    public boolean isOpaque() {
        return (mFlags & IS_OPAQUE) != 0;
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
    public long getColorForState(int[] stateSet, long defaultColor) {
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
    @Override
    @ColorLong
    public long getDefaultColor() {
        int index = mFlags & DEFAULT_COLOR_INDEX;
        return index == DEFAULT_COLOR_INDEX
                ? DEFAULT_COLOR
                : mColors[index];
    }

    /**
     * Return the states in this {@link ColorStateList}. The returned array
     * should not be modified.
     *
     * @return the states in this {@link ColorStateList}
     * @hide
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
     * @hide
     * @hidden
     */
    @ApiStatus.Internal
    public long[] getColors() {
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
     * @hide
     * @hidden
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
                "mFlags=0x" + Integer.toHexString(mFlags) + '}';
    }
}
