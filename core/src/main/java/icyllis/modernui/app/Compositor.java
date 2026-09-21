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

package icyllis.modernui.app;

import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.Engine;
import icyllis.arc3d.granite.GraniteSurface;
import icyllis.arc3d.granite.RecordingContext;
import icyllis.arc3d.sketch.Surface;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.core.Choreographer;
import icyllis.modernui.renderer.FrameTask;
import icyllis.modernui.renderer.RenderPipeline;
import icyllis.modernui.renderer.WindowSurface;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;

/**
 * Composite all window stages and then submit to RHI thread.
 *
 * @hidden
 */
@SuppressWarnings({"ForLoopReplaceableByForEach", "ToArrayCallWithZeroLengthArrayArgument"})
@ApiStatus.Internal
public final class Compositor {

    final Choreographer mChoreographer;

    boolean mCompositionPosted;

    final Runnable mCompositionRunnable = this::doComposition;

    RenderPipeline mRenderPipeline;

    final ArrayList<WindowStage> mAllStages = new ArrayList<>();

    @UiThread
    public Compositor(@NonNull RenderPipeline renderPipeline) {
        mRenderPipeline = renderPipeline;
        mChoreographer = Choreographer.getInstance();
    }

    public void postComposition() {
        if (!mCompositionPosted) {
            mCompositionPosted = true;

            mChoreographer.postCallback(Choreographer.CALLBACK_COMMIT, mCompositionRunnable, null);
        }
    }

    public void addWindowStage(@NonNull WindowStage stage) {
        mAllStages.add(stage);
        stage.mCompositor = this;
        WindowSurface surface = mRenderPipeline.createWindowSurface(stage.getWindow());
        stage.setSurface(surface);
    }

    void doComposition() {
        if (!mCompositionPosted) {
            return;
        }
        mCompositionPosted = false;

        RecordingContext recordingContext = mRenderPipeline.getUiRecordingContext();
        if (recordingContext == null) {
            return;
        }
        ArrayList<FrameTask> frameTasks = new ArrayList<>();

        for (int i = 0; i < mAllStages.size(); i++) {
            WindowStage stage = mAllStages.get(i);
            if (!stage.checkForComposition()) {
                continue;
            }

            WindowSurface surface = stage.getSurface();

            Surface drawingSurface = surface.getCurrentSurface();

            stage.doComposition(drawingSurface.getCanvas());

            FrameTask task = new FrameTask();
            task.surface = surface;
            if (mRenderPipeline.requiresPerSurfaceRecording()) {
                task.surfaceBoundRecording = recordingContext.snap();
            }
            frameTasks.add(task);
        }

        if (!frameTasks.isEmpty()) {
            if (!mRenderPipeline.requiresPerSurfaceRecording()) {
                // all tasks go to the first window surface
                frameTasks.get(0).surfaceBoundRecording = recordingContext.snap();
            }
            mRenderPipeline.postRender(frameTasks.toArray(new FrameTask[frameTasks.size()]));
        }
    }

    @Nullable
    @SharedPtr
    public Surface createSurface(ImageInfo info) {
        var gpuContext = mRenderPipeline.getUiRecordingContext();
        if (gpuContext != null) {
            return GraniteSurface.makeRenderTarget(
                    gpuContext,
                    info,
                    false,
                    Engine.SurfaceOrigin.kUpperLeft,
                    null
            );
        }
        return null;
    }
}
