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

package icyllis.akashigi.slang;

/**
 * Thread-safe class that loads shader modules.
 */
public class ModuleLoader {

    private static final ModuleLoader sInstance = new ModuleLoader();

    private final BuiltinTypes mBuiltinTypes = new BuiltinTypes();

    private final Module mRootModule;

    private ModuleLoader() {
        mRootModule = new Module();

        SymbolTable tab = new SymbolTable(/*builtin=*/true);
        BuiltinTypes t = mBuiltinTypes;

        tab.insert(t.mVoid);

        tab.insert(t.mBool);
        tab.insert(t.mBool2);
        tab.insert(t.mBool3);
        tab.insert(t.mBool4);

        tab.insert(t.mShort);
        tab.insert(t.mShort2);
        tab.insert(t.mShort3);
        tab.insert(t.mShort4);

        tab.insert(t.mUShort);
        tab.insert(t.mUShort2);
        tab.insert(t.mUShort3);
        tab.insert(t.mUShort4);

        tab.insert(t.mInt);
        tab.insert(t.mInt2);
        tab.insert(t.mInt3);
        tab.insert(t.mInt4);

        tab.insert(t.mUInt);
        tab.insert(t.mUInt2);
        tab.insert(t.mUInt3);
        tab.insert(t.mUInt4);

        tab.insert(t.mHalf);
        tab.insert(t.mHalf2);
        tab.insert(t.mHalf3);
        tab.insert(t.mHalf4);

        tab.insert(t.mFloat);
        tab.insert(t.mFloat2);
        tab.insert(t.mFloat3);
        tab.insert(t.mFloat4);

        tab.insert(t.mHalf2x2);
        tab.insert(t.mHalf2x3);
        tab.insert(t.mHalf2x4);
        tab.insert(t.mHalf3x2);
        tab.insert(t.mHalf3x3);
        tab.insert(t.mHalf3x4);
        tab.insert(t.mHalf4x2);
        tab.insert(t.mHalf4x3);
        tab.insert(t.mHalf4x4);

        tab.insert(t.mFloat2x2);
        tab.insert(t.mFloat2x3);
        tab.insert(t.mFloat2x4);
        tab.insert(t.mFloat3x2);
        tab.insert(t.mFloat3x3);
        tab.insert(t.mFloat3x4);
        tab.insert(t.mFloat4x2);
        tab.insert(t.mFloat4x3);
        tab.insert(t.mFloat4x4);

        mRootModule.mSymbols = tab;
    }
}
