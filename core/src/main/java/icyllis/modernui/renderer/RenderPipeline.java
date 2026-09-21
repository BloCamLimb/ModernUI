/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.renderer;

import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.Engine;
import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.granite.GraniteSurface;
import icyllis.arc3d.granite.RecordingContext;
import icyllis.arc3d.sketch.NullSurface;
import icyllis.arc3d.sketch.Surface;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Handler;
import icyllis.modernui.core.Looper;
import icyllis.modernui.core.Message;
import org.jetbrains.annotations.ApiStatus;

/**
 * The Arc3D Granite renderer, abstracted to support different graphics backends.
 * <p>
 * This class is used by multiple threads.
 *
 * @hidden
 */
@ApiStatus.Internal
public abstract class RenderPipeline {

    private static final int MSG_RENDER = 1;

    @RawPtr
    protected ImmediateContext mImmediateContext;
    @SharedPtr
    protected RecordingContext mUiRecordingContext;
    protected Handler mRenderHandler;

    // Render thread
    public RenderPipeline(@NonNull Looper renderLooper,
                          @NonNull ImmediateContext immediateContext) {
        mImmediateContext = immediateContext;
        mRenderHandler = new Handler(renderLooper, this::handleMessage);
    }

    // UI thread
    @RawPtr
    public RecordingContext initUiRecordingContext() {
        if (mImmediateContext != null && mUiRecordingContext == null) {
            mUiRecordingContext = RecordingContext.makeRecordingContext(mImmediateContext,
                    new RecordingContext.Options());
        }
        return mUiRecordingContext;
    }

    // UI thread
    @RawPtr
    public RecordingContext getUiRecordingContext() {
        return mUiRecordingContext;
    }

    // Render thread
    protected boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_RENDER -> doRender((FrameTask[]) msg.obj);
        }

        return true;
    }

    // UI thread
    @Nullable
    public abstract WindowSurface createWindowSurface(long window);

    // UI thread
    public boolean requiresPerSurfaceRecording() {
        return false;
    }

    // UI thread
    public void postRender(FrameTask[] tasks) {
        Message.obtain(mRenderHandler, MSG_RENDER, tasks)
                .sendToTarget();
    }

    // Render thread
    public void doRender(FrameTask[] tasks) {
    }
}
