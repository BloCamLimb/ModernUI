/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine.trash.ops;

import icyllis.arc3d.engine.*;

/**
 * Base class for {@link Op Ops} that draw. These ops can draw into an {@link OpsRenderPass}'s
 * {@link GpuRenderTarget}.
 */
@Deprecated
public abstract class DrawOp extends Op {

    public DrawOp() {
    }

    /**
     * Returns whether the op will draw stencil.
     */
    public boolean usesStencil() {
        return false;
    }
}
