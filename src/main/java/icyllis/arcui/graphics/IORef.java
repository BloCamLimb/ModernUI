/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.graphics;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Base class for GrGpuResource. Provides the hooks for resources to interact with the cache.
 * Separated out as a base class to isolate the ref-cnting behavior and provide friendship without
 * exposing all of GrGpuResource.
 * <p>
 * PRIOR to the last ref being removed DERIVED::notifyARefCntWillBeZero() will be called
 * (static poly morphism using CRTP). It is legal for additional ref's to be added
 * during this time. AFTER the ref count reaches zero DERIVED::notifyARefCntIsZero() will be
 * called.
 */
public abstract class IORef {

    private static final AtomicIntegerFieldUpdater<IORef> REF_CNT_AIF =
            AtomicIntegerFieldUpdater.newUpdater(IORef.class, "mRefCnt");
    private static final AtomicIntegerFieldUpdater<IORef> COMMAND_BUFFER_USAGE_CNT_AIF =
            AtomicIntegerFieldUpdater.newUpdater(IORef.class, "mCommandBufferUsageCnt");

    private static final VarHandle REF_CNT;
    private static final VarHandle COMMAND_BUFFER_USAGE_CNT;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            REF_CNT = lookup.findVarHandle(IORef.class, "mRefCnt", int.class);
            COMMAND_BUFFER_USAGE_CNT = lookup.findVarHandle(IORef.class, "mCommandBufferUsageCnt", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private volatile int mRefCnt = 2;
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private volatile int mCommandBufferUsageCnt = 2;

    public IORef() {
    }

    private static int realRefCnt(int rawCnt) {
        return rawCnt != 2 && rawCnt != 4 && (rawCnt & 1) != 0 ? 0 : rawCnt >>> 1;
    }

    /**
     * Like {@link #realRefCnt(int)} but throws if refCnt == 0
     */
    private static int toLiveRealRefCnt(int rawCnt) {
        if (rawCnt == 2 || rawCnt == 4 || (rawCnt & 1) == 0) {
            return rawCnt >>> 1;
        }
        // odd rawCnt => already deallocated
        throw new IllegalStateException();
    }
}
