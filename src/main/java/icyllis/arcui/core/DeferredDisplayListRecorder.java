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

package icyllis.arcui.core;

import icyllis.arcui.engine.RecordingContext;

/**
 * This class is intended to be used as:
 * <ul>
 *   <li>Get an {@link SurfaceCharacterization} representing the intended gpu-backed destination {@link Surface}</li>
 *   <li>Create a {@link DeferredDisplayListRecorder}</li>
 *   <li>Get the canvas and render into it</li>
 *   <li>Snap off and hold on to an {@link DeferredDisplayList}</li>
 *   <li>Once your app actually needs the pixels, call Surface::draw(DeferredDisplayList*)</li>
 * </ul>
 * <p>
 * This class never accesses the GPU but performs all the cpu work it can. It
 * is thread-safe (i.e., one can break a scene into tiles and perform their cpu-side
 * work in parallel ahead of time).
 */
public final class DeferredDisplayListRecorder implements AutoCloseable {

    private final SurfaceCharacterization mCharacterization;

    private RecordingContext mContext;

    public DeferredDisplayListRecorder(SurfaceCharacterization c) {
        mCharacterization = c;
        if (c != null) {
            mContext = RecordingContext.makeDeferred(c.getContextInfo());
        }
    }

    @Override
    public void close() {
        if (mContext != null) {
            mContext.close();
        }
        mContext = null;
    }
}
