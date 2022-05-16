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

package icyllis.arcui.engine;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Base class for operating server resources that may be shared by multiple
 * objects, in particular objects that are tracked by a command buffer.
 * Unlike {@link Resource}, such resources will NOT have a large memory
 * allocation, but a set of stable states instead.
 * <p>
 * When an existing owner wants to share a reference, it calls ref().
 * When an owner wants to release its reference, it calls unref(). When the
 * shared object's reference count goes to zero as the result of an unref()
 * call, its {@link #onFree()} is called. It is an error for the destructor
 * to be called explicitly (or via the object going out of scope on the
 * stack or calling close()) if getRefCnt() > 1.
 */
public abstract class ManagedResource {

    private static final VarHandle REF_CNT;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            REF_CNT = lookup.findVarHandle(ManagedResource.class, "mRefCnt", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("FieldMayBeFinal")
    private volatile int mRefCnt = 1;

    private final Server mServer;

    public ManagedResource(Server server) {
        mServer = server;
    }

    /**
     * @return true if this resource is uniquely referenced by the client and server pipeline
     */
    public final boolean unique() {
        // std::memory_order_acquire, maybe volatile?
        return (int) REF_CNT.getAcquire(this) == 1;
    }

    /**
     * Increases the reference count by 1 on the client and server pipeline.
     * It's an error to call this method if the reference count has already reached zero.
     */
    public final void ref() {
        assert mRefCnt > 0;
        // stronger than std::memory_order_relaxed
        REF_CNT.getAndAddRelease(this, 1);
    }

    /**
     * Decreases the reference count by 1 on the client and server pipeline.
     * It's an error to call this method if the reference count has already reached zero.
     */
    public final void unref() {
        assert mRefCnt > 0;
        // stronger than std::memory_order_acq_rel
        if ((int) REF_CNT.getAndAdd(this, -1) == 1) {
            onFree();
        }
    }

    /**
     * Called every time this resource is queued for use on the GPU (typically because
     * it was added to a command buffer).
     */
    public void onQueued() {
    }

    /**
     * Called every time this resource has finished its use on the GPU (typically because
     * the command buffer finished execution on the GPU.)
     */
    public void onFinished() {
    }

    /**
     * @return the server
     */
    protected final Server getServer() {
        return mServer;
    }

    /**
     * Override this method to invoke de-allocation of the underlying resource.
     */
    protected abstract void onFree();
}
