/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine.ops;

/**
 * Base class for Ops that draw. These ops can draw into an op list's RenderTarget.
 */
public abstract class DrawOp extends Op {

    protected DrawOp(int classID) {
        super(classID);
    }

    /**
     * Called before setting up the AppliedClip and before finalize. This information is required
     * to determine how to compute a AppliedClip from a Clip for this op.
     */
    public abstract boolean usesMSAA();

    /**
     * Called after finalize, at which point every op should know whether it will need stencil.
     */
    public abstract boolean usesStencil();

    //TODO more methods
}
