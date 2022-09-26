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

package icyllis.arcticgi.engine.ops;

import icyllis.arcticgi.core.Rect2f;
import icyllis.arcticgi.core.Rect2i;
import icyllis.arcticgi.engine.*;

import java.util.ArrayList;

//TODO
public class OpsTask extends RenderTask {

    private final ArrayList<Op> mOps = new ArrayList<>();

    public OpsTask(DrawingManager drawingManager, SurfaceProxyView targetView) {
    }

    @Override
    public boolean execute(OpFlushState flushState) {

        for (Op op : mOps) {
            op.onExecute(flushState, new Rect2f(op.getLeft(), op.getTop(), op.getRight(), op.getBottom()));
        }

        return false;
    }

    public void addOp(Op op) {
        mOps.add(op);
    }

    public static class OpChain {

        private Op mHead;
        private Op mTail;

        private final Rect2i mScissor = new Rect2i();

        private boolean mStencil;

        private final Rect2f mBounds = new Rect2f();

        public OpChain(Op op, Rect2i scissor, boolean stencil) {
            mHead = op;
            mTail = op;
            mScissor.set(scissor);
            mStencil = stencil;

            mBounds.set(op.getLeft(), op.getTop(), op.getRight(), op.getBottom());

            assert invalidate();
        }

        public boolean isEmpty() {
            return mHead == null;
        }

        public Op appendOp(Op op, Rect2i scissor, boolean stencil) {
            assert op.isChainHead() && op.isChainTail();
            assert !isEmpty();

            if (mHead.getClass() != op.getClass()) {
                return op;
            }

            return null;
        }

        private boolean invalidate() {
            if (mHead != null) {
                assert mTail != null;
                assert mHead.validateChain(mTail);
            }
            return true;
        }
    }
}
