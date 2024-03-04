/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.compiler.spirv;

import java.util.Arrays;

/**
 * Used for deduplication.
 */
sealed class Instruction permits InstructionBuilder {

    static final int
            kWord = 0,
            kNoResult = 1,
            kDefaultPrecisionResult = 2,
            kRelaxedPrecisionResult = 3,
            kUniqueResult = 4,
            kKeyedResult = 5;

    int mOpcode;
    int mResultKind;
    int[] mWords;
    transient int mHash;

    Instruction() {
    }

    Instruction(int opcode,
                int resultKind,
                int[] words, int hash) {
        mOpcode = opcode;
        mResultKind = resultKind;
        mWords = words;
        mHash = hash;
    }

    static boolean isResult(int kind) {
        return kind >= kDefaultPrecisionResult;
    }

    @Override
    public int hashCode() {
        int h = mHash;

        if (h == 0) {
            h = mOpcode;
            h = 31 * h + mResultKind;
            for (int j : mWords) {
                h = 31 * h + j;
            }
            mHash = h;
        }

        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass() != Instruction.class) {
            // never compare with lookup keys
            return false;
        }
        Instruction key = (Instruction) o;
        return mOpcode == key.mOpcode &&
                mResultKind == key.mResultKind &&
                Arrays.equals(mWords, key.mWords);
    }
}
