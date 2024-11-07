/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler.spirv;

import icyllis.arc3d.compiler.tree.Type;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Used for cache lookup.
 */
final class InstructionBuilder extends Instruction {

    // word value, result placeholder, or key
    IntArrayList mValues = new IntArrayList();
    // value kind, doesn't affect hash code
    IntArrayList mKinds = new IntArrayList();

    InstructionBuilder(int opcode) {
        mOpcode = opcode;
        mResultKind = kNoResult;
    }

    InstructionBuilder reset(int opcode) {
        mOpcode = opcode;
        mResultKind = kNoResult;
        mValues.clear();
        mKinds.clear();
        mHash = 0;
        return this;
    }

    // add SpvId or literal
    InstructionBuilder addWord(int word) {
        mValues.add(word);
        mKinds.add(kWord);
        return this;
    }

    // add SpvId or literal
    InstructionBuilder addWords(int[] words, int offset, int count) {
        mValues.addElements(mValues.size(), words, offset, count);
        for (int i = 0; i < count; i++) {
            mKinds.add(kWord);
        }
        return this;
    }

    InstructionBuilder addResult() {
        assert mResultKind == kNoResult;
        mValues.add(SPIRVCodeGenerator.NONE_ID);
        mKinds.add(kDefaultPrecisionResult);
        mResultKind = kDefaultPrecisionResult;
        return this;
    }

    InstructionBuilder addRelaxedResult() {
        assert mResultKind == kNoResult;
        mValues.add(SPIRVCodeGenerator.NONE_ID);
        mKinds.add(kRelaxedPrecisionResult);
        mResultKind = kRelaxedPrecisionResult;
        return this;
    }

    InstructionBuilder addResult(@Nonnull Type type) {
        return type.isRelaxedPrecision() ? addRelaxedResult() : addResult();
    }

    InstructionBuilder addUniqueResult() {
        assert mResultKind == kNoResult;
        mValues.add(SPIRVCodeGenerator.NONE_ID);
        mKinds.add(kUniqueResult);
        mResultKind = kUniqueResult;
        return this;
    }

    // Unlike a Result (where the result ID is always deduplicated to its first instruction) or a
    // UniqueResult (which always produces a new instruction), a KeyedResult allows an instruction
    // to be deduplicated among those that share the same `key`.
    InstructionBuilder addKeyedResult(int key) {
        assert key != SPIRVCodeGenerator.NONE_ID;
        assert mResultKind == kNoResult;
        mValues.add(key);
        mKinds.add(kKeyedResult);
        mResultKind = kKeyedResult;
        return this;
    }

    @Nonnull
    Instruction copy() {
        return new Instruction(
                mOpcode,
                mResultKind,
                mValues.toIntArray(),
                mHash);
    }

    @Override
    public int hashCode() {
        int h = mHash;

        if (h == 0) {
            h = mOpcode;
            h = 31 * h + mResultKind;
            int[] a = mValues.elements();
            int s = mValues.size();
            for (int i = 0; i < s; i++) {
                h = 31 * h + a[i];
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
                Arrays.equals(mValues.elements(), 0, mValues.size(),
                        key.mWords, 0, key.mWords.length);
    }
}
