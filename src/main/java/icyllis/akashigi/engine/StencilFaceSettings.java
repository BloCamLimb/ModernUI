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

package icyllis.akashigi.engine;

/**
 * StencilFaceSettings
 */
public final class StencilFaceSettings {

    public short mRef;        // Reference value for stencil test and ops.
    public short mTest;       // Stencil test function, where fRef is on the left side.
    public short mTestMask;   // Bitwise "and" to perform on fRef and stencil values before testing.
    // (e.g. (fRef & fTestMask) < (stencil & fTestMask))
    public byte mPassOp;   // Op to perform when the test passes.
    public byte mFailOp;   // Op to perform when the test fails.
    public short mWriteMask;  // Indicates which bits in the stencil buffer should be updated.
    // (e.g. stencil = (newValue & fWriteMask) | (stencil & ~fWriteMask))

    public StencilFaceSettings() {
    }

    public StencilFaceSettings(short ref, short test, short testMask, byte passOp, byte failOp, short writeMask) {
        mRef = ref;
        mTest = test;
        mTestMask = testMask;
        mPassOp = passOp;
        mFailOp = failOp;
        mWriteMask = writeMask;
    }
}
