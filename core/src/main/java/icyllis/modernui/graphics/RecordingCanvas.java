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

import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;

import javax.annotation.Nonnull;

/**
 * A Canvas implementation that records view system drawing operations for deferred rendering.
 * The drawing operations are directly converted to the data used by low-level graphics API.
 * These data are held by RecordingCanvas on the client side and use the native allocated memory.
 * <p>
 * This is obtained by calling {@link RenderNode#beginRecording()} and is valid until the matching
 * {@link RenderNode#endRecording()} is called. It must not be retained beyond that as it is
 * internally reused.
 * <p>
 * This class is different from and irrelevant to Android.
 */
public class RecordingCanvas {

    private static final Pool<RecordingCanvas> sPool = Pools.concurrent(25);

    private int mWidth;
    private int mHeight;

    private RecordingCanvas() {
    }

    @Nonnull
    static RecordingCanvas obtain(@Nonnull RenderNode node, int width, int height) {
        RecordingCanvas canvas = sPool.acquire();
        if (canvas == null) {
            canvas = new RecordingCanvas();
        }
        canvas.mWidth = width;
        canvas.mHeight = height;
        return canvas;
    }
}
