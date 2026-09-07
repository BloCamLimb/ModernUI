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

package icyllis.modernui.view;

import icyllis.modernui.annotation.UiThread;
import icyllis.modernui.renderer.RenderPipeline;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;

/**
 * @hidden
 */
@ApiStatus.Internal
public class Compositor {

    final Choreographer mChoreographer;

    boolean mCompositionScheduled;

    final Runnable mCompositionRunnable = this::doComposition;

    RenderPipeline mRenderPipeline;

    final ArrayList<WindowStage> mAllStages = new ArrayList<>();

    @UiThread
    public Compositor() {
        mChoreographer = Choreographer.getInstance();
    }

    public void scheduleComposition() {
        if (!mCompositionScheduled) {
            mCompositionScheduled = true;

            mChoreographer.postCallback(Choreographer.CALLBACK_COMMIT, mCompositionRunnable, null);
        }
    }

    public void addWindowStage(WindowStage stage) {
        mAllStages.add(stage);
    }

    void doComposition() {
        if (!mCompositionScheduled) {
            return;
        }
        mCompositionScheduled = false;

    }

    public RenderPipeline getRenderPipeline() {
        return mRenderPipeline;
    }
}
