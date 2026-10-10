/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
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

import org.jspecify.annotations.NonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * State sets are arrays of positive ints where each element
 * represents the state of a {@code View} (e.g. focused,
 * selected, visible, etc.).  A {@code View} may be in
 * one or more of those states.
 * <p>
 * A state spec is an array of signed ints where each element
 * represents a required (if positive) or an undesired (if negative)
 * {@code View} state.
 * <p>
 * Utils dealing with state sets.
 * <p>
 * In theory, we could encapsulate the state set and state spec arrays
 * and not have static methods here but there is some concern about
 * performance since these methods are called during view drawing.
 */
// Modified from Android, removes (hardware) accelerated state.
public final class StateSet {

    /**
     * The order here is very important to
     * {@code View.getDrawableState()}.
     */
    private static final int[][] VIEW_STATE_SETS;

    /**
     * A state specification that will be matched by all StateSets.
     */
    public static final int[] WILD_CARD = {};

    /**
     * State identifier indicating that the object is currently checked.
     */
    public static final int state_checked = 0x010100a0;
    /**
     * State identifier indicating the popup will be above the anchor.
     */
    public static final int state_above_anchor = 0x010100aa;
    /**
     * Set when a view's window has input focus.
     */
    public static final int state_window_focused = 0x0101009d;
    /**
     * Set when a view (or one of its parents) is currently selected.
     */
    public static final int state_selected = 0x010100a1;
    /**
     * Set when a view has input focus.
     */
    public static final int state_focused = 0x01010562;
    /**
     * Set when a view is enabled.
     */
    public static final int state_enabled = 0x0101009e;
    /**
     * Set when a view or its parent has been "activated" meaning the user has currently
     * marked it as being of interest.
     */
    public static final int state_activated = 0x010102fe;
    /**
     * Set when a pointer is hovering over a view.
     */
    public static final int state_hovered = 0x01010367;
    /**
     * Set when a view that is capable of accepting a drop of the content currently
     * being manipulated in a drag-and-drop operation.
     */
    public static final int state_drag_can_accept = 0x01010368;
    /**
     * Set when a view is currently positioned over by a drag operation.
     */
    public static final int state_drag_hovered = 0x01010369;
    /**
     * State value for StateListDrawable, set when a view or drawable is in the last position
     * in an ordered set. Actual usage may vary between views. Consult the host view
     * documentation for details.
     */
    public static final int state_last = 0x010100a6;
    /**
     * State value for StateListDrawable, set when the user is pressing down in a view.
     */
    public static final int state_pressed = 0x010100a7;
    /**
     * State identifier indicating the group is expanded.
     */
    public static final int state_expanded = 0x010100a8;
    /**
     * State identifier indicating the group is empty (has no children).
     */
    public static final int state_empty = 0x010100a9;
    public static final int state_checkable = 0x0101009f;

    public static final int state_error = 0x01015001;
    public static final int state_indeterminate = 0x01015002;
    public static final int state_with_icon = 0x01015003;

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
        var viewStateSets = new int[1 << (VIEW_STATE_IDS.length >> 1)][];
        viewStateSets[0] = WILD_CARD;
        for (int i = 1; i < viewStateSets.length; i++) {
            final int numBits = Integer.bitCount(i);
            final int[] set = new int[numBits];
            int pos = 0;
            for (int j = 0; j < VIEW_STATE_IDS.length; j += 2) {
                if ((i & VIEW_STATE_IDS[j + 1]) != 0) {
                    set[pos++] = VIEW_STATE_IDS[j];
                }
            }
            viewStateSets[i] = set;
        }
        VIEW_STATE_SETS = viewStateSets;
    }

    private static final ConcurrentHashMap<String, Integer> sStateIdMap;
    private static final AtomicInteger sNextStateId = new AtomicInteger(0x02010000);

    static {
        var map = new ConcurrentHashMap<String, Integer>();
        map.put("state_checked", state_checked);
        map.put("state_above_anchor", state_above_anchor);
        map.put("state_window_focused", state_window_focused);
        map.put("state_selected", state_selected);
        map.put("state_focused", state_focused);
        map.put("state_enabled", state_enabled);
        map.put("state_activated", state_activated);
        map.put("state_hovered", state_hovered);
        map.put("state_drag_can_accept", state_drag_can_accept);
        map.put("state_drag_hovered", state_drag_hovered);
        map.put("state_last", state_last);
        map.put("state_pressed", state_pressed);
        map.put("state_expanded", state_expanded);
        map.put("state_empty", state_empty);
        map.put("state_checkable", state_checkable);
        map.put("state_error", state_error);
        map.put("state_indeterminate", state_indeterminate);
        map.put("state_with_icon", state_with_icon);
        sStateIdMap = map;
    }

    /**
     * Get or register a state with the given state name, and return the
     * registered state identifier.
     * <p>
     * Since state names are also attribute names, state names must
     * start with "state_" and use snake case to avoid conflicts.
     *
     * @param stateName the state name, like "state_checked", cannot be empty
     * @return an identifier for the given state
     * @throws IllegalArgumentException not start with "state_"
     */
    public static int getStateId(@NonNull String stateName) {
        if (!stateName.startsWith("state_"))
            throw new IllegalArgumentException();
        return sStateIdMap.computeIfAbsent(stateName, __ -> sNextStateId.getAndIncrement());
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
    public static boolean isWildCard(int @NonNull[] state) {
        return state.length == 0 || state[0] == 0;
    }

    /**
     * Return whether the stateSet matches the desired stateSpec.
     *
     * @param stateSpec an array of required (if positive) or
     *                  prohibited (if negative) {@code View} states.
     * @param stateSet  an array of {@code View} states
     */
    public static boolean stateSetMatches(int @NonNull[] stateSpec, int @NonNull[] stateSet) {
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
     *                  prohibited (if negative) {@code View} states.
     * @param state     a {@code View} state
     */
    public static boolean stateSetMatches(int @NonNull[] stateSpec, int state) {
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
    public static boolean containsAttribute(int @NonNull[][] stateSpecs, int attr) {
        for (int[] spec : stateSpecs) {
            for (int specAttr : spec) {
                if (specAttr == attr || -specAttr == attr) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int @NonNull[] trimStateSet(int @NonNull[] states, int newSize) {
        if (states.length == newSize) {
            return states;
        }
        int[] trimmedStates = new int[newSize];
        System.arraycopy(states, 0, trimmedStates, 0, newSize);
        return trimmedStates;
    }
}
