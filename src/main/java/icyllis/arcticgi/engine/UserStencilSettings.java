/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.engine;

/**
 * Engine uses the stencil buffer to implement complex clipping inside the
 * OpsTask class. The OpsTask makes a subset of the stencil buffer
 * bits available for other uses by external code (user bits). Client code can
 * modify these bits. OpsTask will ignore ref, mask, and writemask bits
 * provided by clients that fall outside the user range.
 * <p>
 * When code outside the OpsTask class uses the stencil buffer the contract
 * is as follows:
 * <p>
 * > Normal stencil funcs allow the client to pass / fail regardless of the
 * reserved clip bits.
 * > Additional functions allow a test against the clip along with a limited
 * set of tests against the user bits.
 * > Client can assume all user bits are zero initially.
 * > Client must ensure that after all its passes are finished it has only
 * written to the color buffer in the region inside the clip. Furthermore, it
 * must zero all user bits that were modified (both inside and outside the
 * clip).
 * <p>
 * This struct is a compile-time constant representation of user stencil settings. It describes in
 * abstract terms how a draw will use the stencil buffer. It gets ODR-used at runtime to define a
 * draw's stencil settings, and is later translated into concrete settings when the pipeline is
 * finalized.
 */
public final class UserStencilSettings {

    /**
     * StencilFlags
     */
    public static final short
            DISABLED_STENCIL_FLAG = (1),
            TEST_ALWAYS_PASSES_STENCIL_FLAG = (1 << 1),
            NO_MODIFY_STENCIL_STENCIL_FLAG = (1 << 2),
            NO_WRAP_OPS_STENCIL_FLAG = (1 << 3),
            SINGLE_SIDED_STENCIL_FLAG = (1 << 4),
            LAST_STENCIL_FLAG = SINGLE_SIDED_STENCIL_FLAG,
            ALL_STENCIL_FLAGS = LAST_STENCIL_FLAG | (LAST_STENCIL_FLAG - 1);

    /**
     * UserStencilTest
     */
    // Tests that respect the clip bit. If a stencil clip is not in effect, the "IfInClip" is
    // ignored and these only act on user bits.
    public static final short
            USER_STENCIL_TEST_ALWAYS_IF_IN_CLIP = 0,
            USER_STENCIL_TEST_EQUAL_IF_IN_CLIP = 1,
            USER_STENCIL_TEST_LESS_IF_IN_CLIP = 2,
            USER_STENCIL_TEST_LEQUAL_IF_IN_CLIP = 3;
    // Tests that ignore the clip bit. The client is responsible to ensure no color write occurs
    // outside the clip if it is in use.
    public static final short
            USER_STENCIL_TEST_ALWAYS = 4,
            USER_STENCIL_TEST_NEVER = 5,
            USER_STENCIL_TEST_GREATER = 6,
            USER_STENCIL_TEST_GEQUAL = 7,
            USER_STENCIL_TEST_LESS = 8,
            USER_STENCIL_TEST_LEQUAL = 9,
            USER_STENCIL_TEST_EQUAL = 10,
            USER_STENCIL_TEST_NOTEQUAL = 11;
    public static final short LAST_CLIPPED_STENCIL_TEST = USER_STENCIL_TEST_LEQUAL_IF_IN_CLIP;
    public static final int USER_STENCIL_TEST_COUNT = 1 + USER_STENCIL_TEST_NOTEQUAL;

    /**
     * UserStencilOp
     */
    public static final byte
            USER_STENCIL_OP_KEEP = 0;
    // Ops that only modify user bits. These must not be paired with ops that modify the clip bit.
    public static final byte
            USER_STENCIL_OP_ZERO = 1,
            USER_STENCIL_OP_REPLACE = 2, // Replace stencil value with fRef (only the bits enabled in fWriteMask).
            USER_STENCIL_OP_INVERT = 3,
            USER_STENCIL_OP_INC_WRAP = 4,
            USER_STENCIL_OP_DEC_WRAP = 5;
    // These two should only be used if wrap ops are not supported, or if the math is guaranteed
    // to not overflow. The user bits may or may not clamp, depending on the state of non-user bits.
    public static final byte
            USER_STENCIL_OP_INC_MAYBE_CLAMP = 6,
            USER_STENCIL_OP_DEC_MAYBE_CLAMP = 7;
    // Ops that only modify the clip bit. These must not be paired with ops that modify user bits.
    public static final byte
            USER_STENCIL_OP_ZERO_CLIP_BIT = 8,
            USER_STENCIL_OP_SET_CLIP_BIT = 9,
            USER_STENCIL_OP_INVERT_CLIP_BIT = 10;
    // Ops that modify both clip and user bits. These can only be paired with kKeep or each other.
    public static final byte
            USER_STENCIL_OP_SET_CLIP_AND_REPLACE_USER_BITS = 11,
            USER_STENCIL_OP_ZERO_CLIP_AND_USER_BITS = 12;
    public static final byte LAST_USER_ONLY_STENCIL_OP = USER_STENCIL_OP_DEC_MAYBE_CLAMP;
    public static final byte LAST_CLIP_ONLY_STENCIL_OP = USER_STENCIL_OP_INVERT_CLIP_BIT;
    public static final int USER_STENCIL_OP_COUNT = 1 + USER_STENCIL_OP_ZERO_CLIP_AND_USER_BITS;

    public final short mCWFlags; // cwFlagsForDraw = fCWFlags[hasStencilClip].
    public final short mCWFlags2; // cwFlagsForDraw = fCWFlags[hasStencilClip].
    public final StencilFaceSettings mCWFace;
    public final short mCCWFlags; // ccwFlagsForDraw = fCCWFlags[hasStencilClip].
    public final short mCCWFlags2; // ccwFlagsForDraw = fCCWFlags[hasStencilClip].
    public final StencilFaceSettings mCCWFace;

    // single sided
    public UserStencilSettings(short ref, short test, short testMask,
                               byte passOp, byte failOp, short writeMask) {
        mCWFlags = (short) (flags(test, passOp, failOp, false) | SINGLE_SIDED_STENCIL_FLAG);
        mCWFlags2 = (short) (flags(test, passOp, failOp, true) | SINGLE_SIDED_STENCIL_FLAG);
        mCWFace = new StencilFaceSettings(ref, test, effectiveTestMask(test, testMask), passOp, failOp,
                effectiveWriteMask(test, passOp, failOp, writeMask));
        mCCWFlags = mCWFlags;
        mCCWFlags2 = mCWFlags2;
        mCCWFace = mCWFace; // alias
    }

    // double sided
    public UserStencilSettings(short cwRef, short ccwRef,
                               short cwTest, short ccwTest,
                               short cwTestMask, short ccwTestMask,
                               byte cwPassOp, byte ccwPassOp,
                               byte cwFailOp, byte ccwFailOp,
                               short cwWriteMask, short ccwWriteMask) {
        mCWFlags = flags(cwTest, cwPassOp, cwFailOp, false);
        mCWFlags2 = flags(cwTest, cwPassOp, cwFailOp, true);
        mCWFace = new StencilFaceSettings(cwRef, cwTest, effectiveTestMask(cwTest, cwTestMask), cwPassOp, cwFailOp,
                effectiveWriteMask(cwTest, cwPassOp, cwFailOp, cwWriteMask));
        mCCWFlags = flags(ccwTest, ccwPassOp, ccwFailOp, false);
        mCCWFlags2 = flags(ccwTest, ccwPassOp, ccwFailOp, true);
        mCCWFace = new StencilFaceSettings(ccwRef, ccwTest, effectiveTestMask(ccwTest, ccwTestMask), ccwPassOp,
                ccwFailOp,
                effectiveWriteMask(ccwTest, ccwPassOp, ccwFailOp, ccwWriteMask));
    }

    public short flags(boolean hasStencilClip) {
        return (short) ((hasStencilClip ? mCWFlags2 : mCWFlags) & (hasStencilClip ? mCCWFlags2 : mCCWFlags));
    }

    public boolean isDisabled(boolean hasStencilClip) {
        return (flags(hasStencilClip) & DISABLED_STENCIL_FLAG) != 0;
    }

    public boolean testAlwaysPasses(boolean hasStencilClip) {
        return (flags(hasStencilClip) & TEST_ALWAYS_PASSES_STENCIL_FLAG) != 0;
    }

    public boolean isDoubleSided(boolean hasStencilClip) {
        return (flags(hasStencilClip) & SINGLE_SIDED_STENCIL_FLAG) == 0;
    }

    public boolean usesWrapOp(boolean hasStencilClip) {
        return (flags(hasStencilClip) & NO_WRAP_OPS_STENCIL_FLAG) == 0;
    }

    private static boolean testAlwaysPasses(short test, boolean hasStencilClip) {
        return (!hasStencilClip && USER_STENCIL_TEST_ALWAYS_IF_IN_CLIP == test) ||
                USER_STENCIL_TEST_ALWAYS == test;
    }

    private static boolean doesNotModifyStencil(short test, byte passOp, byte failOp, boolean hasStencilClip) {
        return (USER_STENCIL_TEST_NEVER == test || USER_STENCIL_OP_KEEP == passOp) &&
                (testAlwaysPasses(test, hasStencilClip) || USER_STENCIL_OP_KEEP == failOp);
    }

    private static boolean isDisabled(short test, byte passOp, byte failOp, boolean hasStencilClip) {
        return testAlwaysPasses(test, hasStencilClip) && doesNotModifyStencil(test, passOp, failOp, hasStencilClip);
    }

    private static boolean usesWrapOps(byte passOp, byte failOp) {
        return USER_STENCIL_OP_INC_WRAP == passOp || USER_STENCIL_OP_DEC_WRAP == passOp ||
                USER_STENCIL_OP_INC_WRAP == failOp || USER_STENCIL_OP_DEC_WRAP == failOp;
    }

    private static boolean testIgnoresRef(short test) {
        return (USER_STENCIL_TEST_ALWAYS_IF_IN_CLIP == test || USER_STENCIL_TEST_ALWAYS == test ||
                USER_STENCIL_TEST_NEVER == test);
    }

    private static short flags(short test, byte passOp, byte failOp, boolean hasStencilClip) {
        return (short) ((isDisabled(test, passOp, failOp, hasStencilClip) ? DISABLED_STENCIL_FLAG : 0) |
                (testAlwaysPasses(test, hasStencilClip) ? TEST_ALWAYS_PASSES_STENCIL_FLAG : 0) |
                (doesNotModifyStencil(test, passOp, failOp, hasStencilClip) ? NO_MODIFY_STENCIL_STENCIL_FLAG : 0) |
                (usesWrapOps(passOp, failOp) ? 0 : NO_WRAP_OPS_STENCIL_FLAG));
    }

    private static short effectiveTestMask(short test, short testMask) {
        return testIgnoresRef(test) ? 0 : testMask;
    }

    private static short effectiveWriteMask(short test, byte passOp, byte failOp, short writeMask) {
        // We don't modify the mask differently when hasStencilClip=false because either the entire
        // face gets disabled in that case (e.g. Test=kAlwaysIfInClip, PassOp=kKeep), or else the
        // effective mask stays the same either way.
        return doesNotModifyStencil(test, passOp, failOp, true) ? 0 : writeMask;
    }
}
