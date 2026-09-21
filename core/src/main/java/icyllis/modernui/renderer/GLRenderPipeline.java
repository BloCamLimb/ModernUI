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

import icyllis.arc3d.engine.ImmediateContext;
import icyllis.arc3d.granite.Recording;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Looper;
import org.lwjgl.system.MemoryUtil;

import static icyllis.modernui.util.Log.LOGGER;

public final class GLRenderPipeline extends RenderPipeline {

    private final GLManager mGLManager;

    public GLRenderPipeline(@NonNull Looper renderLooper,
                            @NonNull ImmediateContext immediateContext,
                            GLManager glManager) {
        super(renderLooper, immediateContext);
        mGLManager = glManager;
    }

    @Override
    public WindowSurface createWindowSurface(long window) {
        return new GLWindowSurface(mUiRecordingContext, window, mGLManager);
    }

    @Override
    public boolean requiresPerSurfaceRecording() {
        // each window has framebuffer 0, but they are different
        return true;
    }

    @Override
    public void doRender(FrameTask[] tasks) {
        ImmediateContext context = mImmediateContext;
        for (var task : tasks) {
            GLWindowSurface surface = (GLWindowSurface) task.surface;
            Recording recording = task.surfaceBoundRecording;
            mGLManager.makeCurrent(surface != null ? surface.getWindow() : MemoryUtil.NULL);
            if (recording != null) {
                boolean added = context.addTask(recording);
                recording.close();
                task.surfaceBoundRecording = null;
                if (added) {
                    context.submit();
                } else {
                    LOGGER.error("Failed to add draw commands");
                }
            }
            if (surface != null) {
                mGLManager.swapBuffers(surface.getWindow());
            }
        }
    }
}
