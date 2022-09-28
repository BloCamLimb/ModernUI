/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.sksl.lex;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;

public class NFAState {

    // represents an accept state - if the NFA ends up in this state, we have successfully
    // matched the token indicated by mData[0]
    public static final int Kind_Accept = 0;
    // matches the single character mChar
    public static final int Kind_Char = 1;
    // the regex '.'; matches any char but '\n'
    public static final int Kind_Dot = 2;
    // a state which serves as a placeholder for the states indicated in mData. When we
    // transition to this state, we instead transition to all of the mData states.
    public static final int Kind_Remapped = 3;
    // contains a list of true/false values in mData. mData[c] tells us whether we accept the
    // character c.
    public static final int Kind_Table = 4;

    public static final NFAState PLACEHOLDER = new NFAState(IntLists.emptyList());

    public int mKind;
    public char mChar = 0;
    public boolean mInverse = false;
    public IntList mData = IntLists.emptyList();
    // states we transition to upon a successful match from this state
    public IntList mNext = IntLists.emptyList();

    public NFAState(int kind, IntList next) {
        mKind = kind;
        mNext = next;
    }

    public NFAState(char c, IntList next) {
        mKind = Kind_Char;
        mChar = c;
        mNext = next;
    }

    public NFAState(IntList states) {
        mKind = Kind_Remapped;
        mData = states;
    }

    public NFAState(boolean inverse, IntList accepts, IntList next) {
        mKind = Kind_Table;
        mInverse = inverse;
        mData = accepts;
        mNext = next;
    }

    public NFAState(int token) {
        mKind = Kind_Accept;
        mData = IntLists.singleton(token);
    }

    public boolean accept(char c) {
        switch (mKind) {
            case Kind_Accept:
                return false;
            case Kind_Char:
                return c == mChar;
            case Kind_Dot:
                return c != '\n';
            case Kind_Table: {
                boolean value;
                if (c < mData.size()) {
                    value = mData.getInt(c) != 0;
                } else {
                    value = false;
                }
                return value != mInverse;
            }
            default:
                throw new IllegalStateException();
        }
    }
}
