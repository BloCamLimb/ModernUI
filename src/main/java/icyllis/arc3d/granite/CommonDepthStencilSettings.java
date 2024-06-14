/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

import icyllis.arc3d.engine.DepthStencilSettings;

import static icyllis.arc3d.engine.DepthStencilSettings.*;

public final class CommonDepthStencilSettings {

    /*
     * DepthStencilSettings reusable by RenderSteps that can shade directly in a single pass, using
     * GREATER or GEQUAL depth tests depending on if they allow self-intersections.
     */

    public static final DepthStencilSettings kDirectDepthGreaterPass = new DepthStencilSettings(
            /*frontFace*/       null,
            /*backFace*/        null,
            /*depthCompareOp*/  COMPARE_OP_GREATER,
            /*depthWrite=*/     true,
            /*stencilTest=*/    false,
            /*depthTest=*/      true
    );

    public static final DepthStencilSettings kDirectDepthGEqualPass = new DepthStencilSettings(
            /*frontFace*/       null,
            /*backFace*/        null,
            /*depthCompareOp*/  COMPARE_OP_GEQUAL,
            /*depthWrite=*/     true,
            /*stencilTest=*/    false,
            /*depthTest=*/      true
    );


    /*
     * "stencil" pass DepthStencilSettings reusable for RenderSteps following some form of
     * stencil-then-cover multi-pass algorithm.
     */

    // Increments stencil value on clockwise triangles. Used for "winding" fill.
    public static final Face kIncrementCW = new Face(
            /*failOp*/      STENCIL_OP_KEEP,
            /*passOp*/      STENCIL_OP_INC_WRAP,
            /*depthFailOp*/ STENCIL_OP_KEEP,
            /*compareOp*/   COMPARE_OP_ALWAYS,
            /*reference*/   (short) 0,
            /*compareMask*/ (short) 0xffff,
            /*writeMask*/   (short) 0xffff
    );

    // Decrements stencil value on counterclockwise triangles. Used for "winding" fill.
    public static final Face kDecrementCCW = new Face(
            /*failOp*/      STENCIL_OP_KEEP,
            /*passOp*/      STENCIL_OP_DEC_WRAP,
            /*depthFailOp*/ STENCIL_OP_KEEP,
            /*compareOp*/   COMPARE_OP_ALWAYS,
            /*reference*/   (short) 0,
            /*compareMask*/ (short) 0xffff,
            /*writeMask*/   (short) 0xffff
    );

    // Toggles the bottom stencil bit. Used for "even-odd" fill.
    public static final Face kToggle = new Face(
            /*failOp*/      STENCIL_OP_KEEP,
            /*passOp*/      STENCIL_OP_INVERT,
            /*depthFailOp*/ STENCIL_OP_KEEP,
            /*compareOp*/   COMPARE_OP_ALWAYS,
            /*reference*/   (short) 0,
            /*compareMask*/ (short) 0xffff,
            /*writeMask*/   (short) 0x0001
    );

    // Stencil settings to use for a standard Redbook "stencil" pass corresponding to a "winding"
    // fill rule (regular or inverse is selected by a follow-up pass).
    public static final DepthStencilSettings kWindingStencilPass = new DepthStencilSettings(
            /*frontFace*/       kIncrementCW,
            /*backFace*/        kDecrementCCW,
            /*depthCompareOp*/  COMPARE_OP_GREATER,
            /*depthWrite=*/     false,  // The depth write will be handled by the covering pass
            /*stencilTest=*/    true,
            /*depthTest=*/      true
    );

    // Stencil settings to use for a standard Redbook "stencil" pass corresponding to an "even-odd"
    // fill rule (regular or inverse is selected by a follow-up pass).
    public static final DepthStencilSettings kEvenOddStencilPass = new DepthStencilSettings(
            /*frontFace*/       kToggle,
            /*backFace*/        kToggle,
            /*depthCompareOp*/  COMPARE_OP_GREATER,
            /*depthWrite=*/     false,  // The depth write will be handled by the covering pass
            /*stencilTest=*/    true,
            /*depthTest=*/      true
    );


    /*
     * "cover" pass DepthStencilSettings reusable for RenderSteps following some form of
     * stencil-then-cover multi-pass algorithm.
     */

    // Resets non-zero bits to 0, passes when not zero. We set depthFail to kZero because if we
    // encounter that case, the kNotEqual=0 stencil test passed, so it does need to be set back to 0
    // and the dsPass op won't be run. In practice, since the stencil steps will fail the same depth
    // test, the stencil value will likely not be non-zero, but best to be explicit.
    public static final Face kPassNonZero = new Face(
            /*failOp*/      STENCIL_OP_KEEP,
            /*passOp*/      STENCIL_OP_ZERO,
            /*depthFailOp*/ STENCIL_OP_ZERO,
            /*compareOp*/   COMPARE_OP_NOTEQUAL,
            /*reference*/   (short) 0,
            /*compareMask*/ (short) 0xffff,
            /*writeMask*/   (short) 0xffff
    );

    // Resets non-zero bits to 0, passes when zero.
    public static final Face kPassZero = new Face(
            /*failOp*/      STENCIL_OP_ZERO,
            /*passOp*/      STENCIL_OP_KEEP,
            /*depthFailOp*/ STENCIL_OP_KEEP,
            /*compareOp*/   COMPARE_OP_EQUAL,
            /*reference*/   (short) 0,
            /*compareMask*/ (short) 0xffff,
            /*writeMask*/   (short) 0xffff
    );

    // Stencil settings to use for a standard Redbook "cover" pass for a regular fill, assuming that the
    // stencil buffer has been modified by either kWindingStencilPass or kEvenOddStencilPass.
    public static final DepthStencilSettings kRegularCoverPass = new DepthStencilSettings(
            /*frontFace*/       kPassNonZero,
            /*backFace*/        kPassNonZero,
            /*depthCompareOp*/  COMPARE_OP_GREATER,
            /*depthWrite=*/     true,
            /*stencilTest=*/    true,
            /*depthTest=*/      true
    );

    // Stencil settings to use for a standard Redbook "cover" pass for inverse fills, assuming that the
    // stencil buffer has been modified by either kWindingStencilPass or kEvenOddStencilPass.
    public static final DepthStencilSettings kInverseCoverPass = new DepthStencilSettings(
            /*frontFace*/       kPassZero,
            /*backFace*/        kPassZero,
            /*depthCompareOp*/  COMPARE_OP_GREATER,
            /*depthWrite=*/     true,
            /*stencilTest=*/    true,
            /*depthTest=*/      true
    );
}
