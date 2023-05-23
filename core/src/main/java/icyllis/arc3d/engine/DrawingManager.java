/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.engine;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

public class DrawingManager {

    private final RecordingContext mContext;
    private final DirectContext mDirect;

    private final Reference2ObjectOpenHashMap<Object, RenderTask> mLastRenderTasks =
            new Reference2ObjectOpenHashMap<>();

    private final OpFlushState mFlushState;
    private final ResourceAllocator mResourceAllocator;

    private boolean mFlushing;

    public DrawingManager(RecordingContext context) {
        mContext = context;
        if (context instanceof DirectContext direct) {
            mDirect = direct;
            mFlushState = new OpFlushState(direct.getServer(), direct.getResourceProvider());
            mResourceAllocator = new ResourceAllocator(direct);
        } else {
            // deferred
            mDirect = null;
            mFlushState = null;
            mResourceAllocator = null;
        }
    }

    void destroy() {

    }

    RecordingContext getContext() {
        return mContext;
    }

    public void setLastRenderTask(SurfaceProxy proxy, RenderTask task) {
        if (task != null) {
            mLastRenderTasks.put(proxy.getUniqueID(), task);
        } else {
            mLastRenderTasks.remove(proxy);
        }
    }

    public RenderTask getLastRenderTask(SurfaceProxy proxy) {
        return mLastRenderTasks.get(proxy.getUniqueID());
    }
}
