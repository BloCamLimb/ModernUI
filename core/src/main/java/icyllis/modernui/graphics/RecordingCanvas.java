/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import icyllis.modernui.math.MathUtil;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Modern UI implementation to Canvas.
 */
public class RecordingCanvas extends Canvas {

    private static final Pool<RecordingCanvas> sPool = Pools.concurrent(15);

    private final Deque<Matrix4> mMatrixStack = new ArrayDeque<>();

    private RenderNode mNode;

    private RecordingCanvas() {
    }

    @Nonnull
    static RecordingCanvas obtain(@Nonnull RenderNode node) {
        RecordingCanvas canvas = sPool.acquire(RecordingCanvas::new);
        canvas.mNode = node;
        return canvas;
    }

    void recycle() {
        mNode = null;
        sPool.release(this);
    }

    @Nonnull
    public Matrix4 getCurrentMatrix() {
        return mMatrixStack.getFirst();
    }

    @Override
    public int save() {
        int saveCount = getSaveCount();
        mMatrixStack.push(getCurrentMatrix().copy());
        return saveCount;
    }

    @Override
    public void restore() {
        mMatrixStack.pop();
        if (mMatrixStack.isEmpty()) {
            throw new IllegalStateException("Underflow in restore");
        }
    }

    @Override
    public int getSaveCount() {
        return mMatrixStack.size();
    }

    @Override
    public void restoreToCount(int saveCount) {
        if (saveCount < 1) {
            throw new IllegalArgumentException("Underflow in restoreToCount");
        }
        Deque<?> stack = mMatrixStack;
        while (stack.size() > saveCount) {
            stack.pop();
        }
    }

    @Override
    public void translate(float dx, float dy) {
        if (dx != 1.0f && dy != 1.0f)
            getCurrentMatrix().translate(dx, dy, 0);
    }

    @Override
    public void scale(float sx, float sy) {
        if (sx != 1.0f && sy != 1.0f)
            getCurrentMatrix().scale(sx, sy, 1);
    }

    @Override
    public void rotate(float degrees) {
        if (degrees != 0.0f)
            getCurrentMatrix().rotateZ(MathUtil.toRadians(degrees));
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, @Nonnull Paint paint) {
        //GLWrapper.glDrawArraysInstanced();
        //GLWrapper.glDrawArraysInstancedBaseInstance();
    }
}
