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

package icyllis.arcui;

import org.jetbrains.annotations.ApiStatus;

public class RecordingContext {

    protected final ThreadSafeProxy mProxy;

    public RecordingContext(ThreadSafeProxy proxy) {
        mProxy = proxy;
    }

    /**
     * The 3D API backing this context
     */
    @ApiStatus.Internal
    public final int backend() {
        return mProxy.backend();
    }

    public ThreadSafeProxy threadSafeProxy() {
        return mProxy;
    }
}
