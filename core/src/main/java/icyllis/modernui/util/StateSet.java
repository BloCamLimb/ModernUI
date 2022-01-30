/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import icyllis.modernui.view.View;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;

/**
 * State sets are arrays of positive ints where each element
 * represents the state of a {@link View} (e.g. focused,
 * selected, visible, etc.).  A {@link View} may be in
 * one or more of those states.
 * <p>
 * A state spec is an array of signed ints where each element
 * represents a required (if positive) or an undesired (if negative)
 * {@link View} state.
 * <p>
 * Utils dealing with state sets.
 * <p>
 * In theory, we could encapsulate the state set and state spec arrays
 * and not have static methods here but there is some concern about
 * performance since these methods are called during view drawing.
 * <p>
 * Modified from Android, removes (hardware) accelerated state.
 */
public final class StateSet {

    /**
     * The order here is very important to
     * {@link View#getDrawableState()}
     */
    private static final int[][] VIEW_STATE_SETS;

    // Manually generated IDs
    public static final int state_window_focused = 0x01010090;
    public static final int state_selected = 0x01010091;
    public static final int state_focused = 0x01010092;
    public static final int state_enabled = 0x01010093;
    public static final int state_pressed = 0x01010094;
    public static final int state_activated = 0x01010095;
    public static final int state_hovered = 0x01010096;
    public static final int state_drag_can_accept = 0x01010097;
    public static final int state_drag_hovered = 0x01010098;

    @ApiStatus.Internal
    public static final int
            VIEW_STATE_WINDOW_FOCUSED = 1,
            VIEW_STATE_SELECTED = 1 << 1,
            VIEW_STATE_FOCUSED = 1 << 2,
            VIEW_STATE_ENABLED = 1 << 3,
            VIEW_STATE_PRESSED = 1 << 4,
            VIEW_STATE_ACTIVATED = 1 << 5,
            VIEW_STATE_HOVERED = 1 << 6,
            VIEW_STATE_DRAG_CAN_ACCEPT = 1 << 7,
            VIEW_STATE_DRAG_HOVERED = 1 << 8;

    static final int[] VIEW_STATE_IDS = new int[]{
            state_window_focused, VIEW_STATE_WINDOW_FOCUSED,
            state_selected, VIEW_STATE_SELECTED,
            state_focused, VIEW_STATE_FOCUSED,
            state_enabled, VIEW_STATE_ENABLED,
            state_pressed, VIEW_STATE_PRESSED,
            state_activated, VIEW_STATE_ACTIVATED,
            state_hovered, VIEW_STATE_HOVERED,
            state_drag_can_accept, VIEW_STATE_DRAG_CAN_ACCEPT,
            state_drag_hovered, VIEW_STATE_DRAG_HOVERED
    };

    static {
        // 20KB
        VIEW_STATE_SETS = new int[1 << (VIEW_STATE_IDS.length >> 1)][];
        for (int i = 0; i < VIEW_STATE_SETS.length; i++) {
            final int numBits = Integer.bitCount(i);
            final int[] set = new int[numBits];
            int pos = 0;
            for (int j = 0; j < VIEW_STATE_IDS.length; j += 2) {
                if ((i & VIEW_STATE_IDS[j + 1]) != 0) {
                    set[pos++] = VIEW_STATE_IDS[j];
                }
            }
            VIEW_STATE_SETS[i] = set;
        }
    }

    @ApiStatus.Internal
    public static int[] get(int mask) {
        if (mask >= VIEW_STATE_SETS.length) {
            throw new IllegalArgumentException("Invalid state set mask");
        }
        return VIEW_STATE_SETS[mask];
    }

    private StateSet() {
    }

    /**
     * A state specification that will be matched by all StateSets.
     */
    public static final int[] WILD_CARD = new int[0];

    /**
     * A state set that does not contain any valid states.
     */
    public static final int[] NOTHING = new int[]{0};

    /**
     * Return whether the stateSetOrSpec is matched by all StateSets.
     *
     * @param stateSetOrSpec a state set or state spec.
     */
    public static boolean isWildCard(@Nonnull int[] stateSetOrSpec) {
        return stateSetOrSpec.length == 0 || stateSetOrSpec[0] == 0;
    }

    /**
     * Return whether the stateSet matches the desired stateSpec.
     *
     * @param stateSpec an array of required (if positive) or
     *                  prohibited (if negative) {@link View} states.
     * @param stateSet  an array of {@link View} states
     */
    public static boolean stateSetMatches(int[] stateSpec, int[] stateSet) {
        if (stateSet == null) {
            return (stateSpec == null || isWildCard(stateSpec));
        }
        for (int stateSpecState : stateSpec) {
            if (stateSpecState == 0) {
                // We've reached the end of the cases to match against.
                return true;
            }
            final boolean mustMatch;
            if (stateSpecState > 0) {
                mustMatch = true;
            } else {
                // We use negative values to indicate must-NOT-match states.
                mustMatch = false;
                stateSpecState = -stateSpecState;
            }
            boolean found = false;
            for (int state : stateSet) {
                if (state == 0) {
                    // We've reached the end of states to match.
                    if (mustMatch) {
                        // We didn't find this must-match state.
                        return false;
                    } else {
                        // Continue checking other must-not-match states.
                        break;
                    }
                }
                if (state == stateSpecState) {
                    if (mustMatch) {
                        found = true;
                        // Continue checking other must-match states.
                        break;
                    } else {
                        // Any match of a must-not-match state returns false.
                        return false;
                    }
                }
            }
            if (mustMatch && !found) {
                // We've reached the end of states to match, and we didn't
                // find a must-match state.
                return false;
            }
        }
        return true;
    }

    /**
     * Return whether the state matches the desired stateSpec.
     *
     * @param stateSpec an array of required (if positive) or
     *                  prohibited (if negative) {@link View} states.
     * @param state     a {@link View} state
     */
    public static boolean stateSetMatches(@Nonnull int[] stateSpec, int state) {
        for (int stateSpecState : stateSpec) {
            if (stateSpecState == 0) {
                // We've reached the end of the cases to match against.
                return true;
            }
            if (stateSpecState > 0) {
                if (state != stateSpecState) {
                    return false;
                }
            } else {
                // We use negative values to indicate must-NOT-match states.
                if (state == -stateSpecState) {
                    // We matched a must-not-match case.
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check whether a list of state specs has an attribute specified.
     *
     * @param stateSpecs a list of state specs we're checking.
     * @param attr       an attribute we're looking for.
     * @return {@code true} if the attribute is contained in the state specs.
     */
    @ApiStatus.Internal
    public static boolean containsAttribute(int[][] stateSpecs, int attr) {
        if (stateSpecs != null) {
            for (int[] spec : stateSpecs) {
                if (spec == null) {
                    break;
                }
                for (int specAttr : spec) {
                    if (specAttr == attr || -specAttr == attr) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Nonnull
    public static int[] trimStateSet(@Nonnull int[] states, int newSize) {
        if (states.length == newSize) {
            return states;
        }
        int[] trimmedStates = new int[newSize];
        System.arraycopy(states, 0, trimmedStates, 0, newSize);
        return trimmedStates;
    }
}
