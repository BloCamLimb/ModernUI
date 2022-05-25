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

import icyllis.arcui.core.RefCnt;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class abstracts a task that targets a single {@link SurfaceProxy}, participates in the
 * {@link DrawingManager}'s DAG, and implements the onExecute method to modify its target proxy's
 * contents. (e.g., an opsTask that executes a command buffer, a task to regenerate mipmaps, etc.)
 */
public abstract class RenderTask extends RefCnt {

    private static final AtomicInteger sNextID = new AtomicInteger(1);

    @Override
    protected void onFree() {
        // the default implementation is NO-OP
    }
}
