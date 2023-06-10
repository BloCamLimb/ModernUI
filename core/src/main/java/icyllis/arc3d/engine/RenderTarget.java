/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.engine;

import javax.annotation.Nonnull;

/**
 * Common interface between RenderTexture and {@link RenderSurface}.
 */
public interface RenderTarget extends Surface {

    /**
     * Returns the number of samples per pixel in color buffers (one if non-MSAA).
     *
     * @return the number of samples, greater than (multi-sampled) or equal to one
     */
    int getSampleCount();

    /**
     * Returns the underlying object who owns the framebuffers of this RT.
     *
     * @return raw ptr to the framebuffer set associated with the RT, non-null
     */
    @Nonnull
    FramebufferSet getFramebufferSet();
}
