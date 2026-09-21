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
import icyllis.arc3d.granite.RecordingContext;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Looper;

public final class VulkanRenderPipeline extends RenderPipeline {

    private final VulkanManager mVulkanManager;

    public VulkanRenderPipeline(@NonNull Looper renderLooper,
                                @NonNull ImmediateContext immediateContext,
                                @NonNull VulkanManager vulkanManager) {
        super(renderLooper, immediateContext);
        mVulkanManager = vulkanManager;
    }

    @Override
    public WindowSurface createWindowSurface(long window) {
        return new VulkanWindowSurface(mUiRecordingContext, window, mVulkanManager);
    }
}
