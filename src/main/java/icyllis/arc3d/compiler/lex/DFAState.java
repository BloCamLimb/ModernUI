/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler.lex;

import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Unmodifiable;

public class DFAState {

    public final int mIndex;
    public final IntList mStates;

    boolean mScanned = false;

    /**
     * @param index the ID of this DFA state, also the index in the DFA state list
     * @param states a list of NFA states
     */
    public DFAState(int index, @Unmodifiable IntList states) {
        mIndex = index;
        mStates = states;
    }
}
