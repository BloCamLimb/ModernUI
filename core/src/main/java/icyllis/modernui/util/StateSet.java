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

import icyllis.modernui.R;
import icyllis.modernui.view.View;

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
     * {@link View#getDrawableState()}.
     */
    private static final int[][] VIEW_STATE_SETS;

    /**
     * A state specification that will be matched by all StateSets.
     */
    public static final int[] WILD_CARD = {};

    /**
     * Called by View.
     */
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

    private static final int[] VIEW_STATE_IDS = new int[]{
            R.attr.state_window_focused, VIEW_STATE_WINDOW_FOCUSED,
            R.attr.state_selected, VIEW_STATE_SELECTED,
            R.attr.state_focused, VIEW_STATE_FOCUSED,
            R.attr.state_enabled, VIEW_STATE_ENABLED,
            R.attr.state_pressed, VIEW_STATE_PRESSED,
            R.attr.state_activated, VIEW_STATE_ACTIVATED,
            R.attr.state_hovered, VIEW_STATE_HOVERED,
            R.attr.state_drag_can_accept, VIEW_STATE_DRAG_CAN_ACCEPT,
            R.attr.state_drag_hovered, VIEW_STATE_DRAG_HOVERED
    };

    static {
        // 20KB
        VIEW_STATE_SETS = new int[1 << (VIEW_STATE_IDS.length >> 1)][];
        VIEW_STATE_SETS[0] = WILD_CARD;
        for (int i = 1; i < VIEW_STATE_SETS.length; i++) {
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

    private StateSet() {
    }

    /**
     * Called by View.
     */
    public static int[] get(int mask) {
        return VIEW_STATE_SETS[mask];
    }

    /**
     * Return whether the state is matched by all StateSets.
     *
     * @param state a state set or state spec.
     */
    public static boolean isWildCard(@Nonnull int[] state) {
        return state.length == 0 || state[0] == 0;
    }

    /**
     * Return whether the stateSet matches the desired stateSpec.
     *
     * @param stateSpec an array of required (if positive) or
     *                  prohibited (if negative) {@link View} states.
     * @param stateSet  an array of {@link View} states
     */
    public static boolean stateSetMatches(@Nonnull int[] stateSpec, @Nonnull int[] stateSet) {
        CYCLE:
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
            for (int state : stateSet) {
                if (state == 0) {
                    // We've reached the end of states to match.
                    if (mustMatch) {
                        // We didn't find this must-match state.
                        return false;
                    } else {
                        // Continue checking other must-not-match states.
                        continue CYCLE;
                    }
                }
                if (state == stateSpecState) {
                    if (mustMatch) {
                        // Continue checking other must-match states.
                        continue CYCLE;
                    } else {
                        // Any match of a must-not-match state returns false.
                        return false;
                    }
                }
            }
            if (mustMatch) {
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
    public static boolean containsAttribute(@Nonnull int[][] stateSpecs, int attr) {
        for (int[] spec : stateSpecs) {
            for (int specAttr : spec) {
                if (specAttr == attr || -specAttr == attr) {
                    return true;
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
