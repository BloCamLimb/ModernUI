/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import icyllis.modernui.math.Matrix4;
import icyllis.modernui.math.Rect;
import icyllis.modernui.math.RectF;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;

public abstract class GLCanvas extends Canvas {

    // shared pools
    static final Pool<Save> sSavePool = Pools.concurrent(60);

    // see window
    static final Matrix4 RESET_MATRIX = Matrix4.makeTranslation(0, 0, -3000);

    // local MCRec stack
    final ArrayDeque<Save> mSaves = new ArrayDeque<>();

    final Matrix4 mLastMatrix = new Matrix4();
    float mLastSmooth;

    int mWidth;
    int mHeight;

    final Rect mTmpRect = new Rect();
    final RectF mTmpRectF = new RectF();

    GLCanvas() {
    }

    /**
     * Resets the clip bounds and matrix to root.
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     */
    public void reset(int width, int height) {
        while (mSaves.size() > 1) {
            sSavePool.release(mSaves.poll());
        }
        Save s = mSaves.element();
        s.mClip.set(0, 0, width, height);
        s.mMatrix.set(RESET_MATRIX);
        s.mClipRef = 0;
        s.mColorBuf = 0;
        mLastMatrix.setZero();
        mLastSmooth = -1;
        mWidth = width;
        mHeight = height;
    }

    /**
     * Resets the clip bounds and matrix to node.
     *
     * @param node   the parent node
     * @param width  the width in pixels
     * @param height the height in pixels
     */
    public void reset(@Nonnull GLCanvas node, int width, int height) {
        while (mSaves.size() > 1) {
            sSavePool.release(mSaves.poll());
        }
        getSave().set(node.getSave());
        mLastMatrix.setZero();
        mLastSmooth = -1;
        mWidth = width;
        mHeight = height;
    }

    /**
     * @inheritDoc
     */
    @Override
    public final int getSaveCount() {
        return mSaves.size();
    }

    /**
     * @inheritDoc
     */
    @Nonnull
    @Override
    public final Matrix4 getMatrix() {
        return mSaves.element().mMatrix;
    }

    @Nonnull
    Save getSave() {
        return mSaves.getFirst();
    }

    static final class Save {

        // maximum clip bounds transformed by model view matrix
        final Rect mClip = new Rect();

        // model view matrix
        final Matrix4 mMatrix = Matrix4.identity();

        // stencil reference
        int mClipRef;

        // stack depth of color buffers
        int mColorBuf;

        void set(@Nonnull Save s) {
            mClip.set(s.mClip);
            mMatrix.set(s.mMatrix);
            mClipRef = s.mClipRef;
            mColorBuf = s.mColorBuf;
        }

        // deep copy
        @Nonnull
        Save copy() {
            Save s = new Save();
            s.set(this);
            return s;
        }
    }
}
