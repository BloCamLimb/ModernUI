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

package icyllis.arcui.hgi;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Ref-counted object that calls a callback from its destructor.
 */
public abstract class ReleaseCallback {

    private static final VarHandle REF_CNT;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            REF_CNT = lookup.findVarHandle(ReleaseCallback.class, "mRefCnt", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("FieldMayBeFinal")
    private volatile int mRefCnt = 1;

    /**
     * @return true if this resource is uniquely referenced
     */
    public final boolean unique() {
        // std::memory_order_acquire, maybe volatile?
        return (int) REF_CNT.getAcquire(this) == 1;
    }

    /**
     * Increases the reference count by 1.
     * It's an error to call this method if the reference count has already reached zero.
     */
    public final void ref() {
        assert mRefCnt > 0;
        // stronger than std::memory_order_relaxed
        REF_CNT.getAndAddRelease(this, 1);
    }

    /**
     * Decreases the reference count by 1 .
     * It's an error to call this method if the reference count has already reached zero.
     */
    public final void unref() {
        assert mRefCnt > 0;
        // stronger than std::memory_order_acq_rel
        if ((int) REF_CNT.getAndAdd(this, -1) == 1) {
            onRelease();
        }
    }

    public abstract void onRelease();
}
