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

import java.util.concurrent.ConcurrentHashMap;

/**
 * State sets are arrays of positive ints where each element
 * represents the state of a {@link icyllis.modernui.view.View} (e.g. focused,
 * selected, visible, etc.).  A {@link icyllis.modernui.view.View} may be in
 * one or more of those states.
 *
 * A state spec is an array of signed ints where each element
 * represents a required (if positive) or an undesired (if negative)
 * {@link android.view.View} state.
 *
 * Utils dealing with state sets.
 *
 * In theory we could encapsulate the state set and state spec arrays
 * and not have static methods here but there is some concern about
 * performance since these methods are called during view drawing.
 */
//TODO
public final class StateSet {

    {

    }
}
