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

import icyllis.arc3d.granite.RecordingContext;
import icyllis.arc3d.sketch.Surface;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLVulkan;
import org.lwjgl.system.MemoryStack;

public final class VulkanWindowSurface extends WindowSurface {

    private long mVkSurface; // VkSurfaceKHR

    private Surface[] mSwapchainSurfaces;

    public VulkanWindowSurface(RecordingContext recordingContext, long window,
                               VulkanManager manager) {

        try (var stack = MemoryStack.stackPush()) {
            var pSurface = stack.mallocLong(1);
            var res = SDLVulkan.SDL_Vulkan_CreateSurface(
                    window, manager.getInstance(), null, pSurface);
            if (!res) {
                throw new IllegalStateException("Cannot create vulkan window surface: " +
                        SDLError.SDL_GetError());
            }
            mVkSurface = pSurface.get(0);
        }
    }

    @Override
    public int acquireNextImage(long swapchain) {
        return super.acquireNextImage(swapchain);
    }

    @Override
    public Surface getCurrentSurface() {
        return null;
    }
}
