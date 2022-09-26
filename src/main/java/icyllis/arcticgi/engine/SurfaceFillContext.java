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

import icyllis.arcticgi.core.RefCnt;
import icyllis.arcticgi.core.SharedPtr;
import icyllis.arcticgi.engine.ops.OpsTask;

public class SurfaceFillContext extends SurfaceContext {

    @SharedPtr
    private OpsTask mOpsTask;

    private final SurfaceProxyView mWriteView;

    public SurfaceFillContext(RecordingContext context,
                              SurfaceProxyView readView,
                              SurfaceProxyView writeView,
                              int colorInfo) {
        super(context, readView, colorInfo);
        mWriteView = writeView;
    }

    public OpsTask getOpsTask() {
        assert mContext.isOnOwnerThread();
        if (mOpsTask == null || mOpsTask.isClosed()) {
            return nextOpsTask();
        }
        return mOpsTask;
    }

    public OpsTask nextOpsTask() {
        OpsTask newOpsTask = getDrawingManager().newOpsTask(mWriteView);
        mOpsTask = RefCnt.move(mOpsTask, newOpsTask);
        return mOpsTask;
    }
}
