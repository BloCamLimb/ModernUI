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

import icyllis.modernui.graphics.Rect;
import icyllis.modernui.renderer.RenderPipeline;
import org.jetbrains.annotations.ApiStatus;

/**
 * Stage represents a logical screen/display that hosts an application window
 * and optional dialog windows (such as context menus, toasts, tooltips).
 *
 * @hidden
 */
@ApiStatus.Internal
public interface Stage extends WindowManager {

    /**
     * Returns the framebuffer width for this window in pixels.
     *
     * @return the framebuffer width
     */
    int getWidth();

    /**
     * Returns the framebuffer height for this window in pixels.
     *
     * @return the framebuffer height
     */
    int getHeight();

    /**
     * Called when any ViewRoot scheduleTraversals() is called, to post a composition callback.
     */
    void postComposition();

    /**
     * Called when any ViewRoot draw() is called, so that it's dirty and needs actual composition.
     */
    void markForComposition();

    RenderPipeline getRenderPipeline();
}
