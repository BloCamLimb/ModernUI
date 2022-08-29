/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.engine;

import icyllis.arcticgi.core.Rect;
import icyllis.arcticgi.core.SharedPtr;

/**
 * The OpsRenderPass is a series of commands (draws, clears, and discards), which all target the
 * same render target. It is possible that these commands execute immediately (OpenGL), or get buffered
 * up for later execution (Vulkan). {@link icyllis.arcticgi.engine.ops.Op Ops} execute into a OpsRenderPass.
 */
//TODO
public class OpsRenderPass {

    protected void set(@SharedPtr Surface colorAttachment,
                       @SharedPtr Surface resolveAttachment,
                       @SharedPtr Surface stencilAttachment,
                       int origin,
                       Rect bounds,
                       int colorLoadOp,
                       int colorStoreOp,
                       float colorClearR,
                       float colorClearG,
                       float colorClearB,
                       float colorClearA,
                       int resolveLoadOp,
                       int resolveStoreOp,
                       float resolveClearR,
                       float resolveClearG,
                       float resolveClearB,
                       float resolveClearA,
                       int stencilLoadOp,
                       int stencilStoreOp) {

    }
}
