/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite.trash.ops;

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.engine.*;
import org.jspecify.annotations.NonNull;

@Deprecated
public final class ClearOp extends Op {

    private static final int COLOR_BUFFER = 0x1;
    private static final int STENCIL_BUFFER = 0x2;

    private int mBuffer;
    private int mScissorLeft, mScissorTop, mScissorRight, mScissorBottom;
    private float mColorRed, mColorGreen, mColorBlue, mColorAlpha;
    private boolean mInsideMask;

    private ClearOp(int buffer, int scissorLeft, int scissorTop, int scissorRight, int scissorBottom,
                    float colorRed, float colorGreen, float colorBlue, float colorAlpha, boolean insideMask) {
        mBuffer = buffer;
        mScissorLeft = scissorLeft;
        mScissorTop = scissorTop;
        mScissorRight = scissorRight;
        mScissorBottom = scissorBottom;
        mColorRed = colorRed;
        mColorGreen = colorGreen;
        mColorBlue = colorBlue;
        mColorAlpha = colorAlpha;
        mInsideMask = insideMask;
        set(scissorLeft, scissorTop, scissorRight, scissorBottom);
        setBoundsFlags(false, false);
    }

    @NonNull
    public static Op makeColor(int left, int top, int right, int bottom,
                               float red, float green, float blue, float alpha) {
        return new ClearOp(COLOR_BUFFER, left, top, right, bottom, red, green, blue, alpha, false);
    }

    @NonNull
    public static Op makeStencil(int left, int top, int right, int bottom,
                                 boolean insideMask) {
        return new ClearOp(STENCIL_BUFFER, left, top, right, bottom, 0, 0, 0, 0, insideMask);
    }

    @Override
    public void onPrePrepare(RecordingContext context,
                             ImageProxyView writeView,
                             int pipelineFlags) {
    }

    @Override
    public void onPrepare(OpFlushState state,
                          ImageProxyView writeView,
                          int pipelineFlags) {
    }

    @Override
    public void onExecute(OpFlushState state, Rect2f chainBounds) {
        if ((mBuffer & COLOR_BUFFER) != 0) {
            state.getOpsRenderPass().clearColor(mScissorLeft, mScissorTop, mScissorRight, mScissorBottom,
                    mColorRed, mColorGreen, mColorBlue, mColorAlpha);
        }
        if ((mBuffer & STENCIL_BUFFER) != 0) {
            state.getOpsRenderPass().clearStencil(mScissorLeft, mScissorTop, mScissorRight, mScissorBottom,
                    mInsideMask);
        }
    }
}
