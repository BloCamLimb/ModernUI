/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

public final class GeometryRenderer {

    /**
     * The maximum number of render steps that any Renderer is allowed to have.
     * For example, Stencil-then-Cover method has multiple steps.
     */
    public static final int MAX_RENDER_STEPS = 4;

    // we have limited number of geometry steps, enumerate here
    private final GeometryStep mStep0;
    private final GeometryStep mStep1;
    private final GeometryStep mStep2;
    private final int mNumSteps;

    private final String mName;
    private final int mStepFlags;
    private final int mDepthStencilFlags;

    public GeometryRenderer(String name, GeometryStep step0) {
        mName = name;
        mStep0 = step0;
        mStep1 = mStep2 = null;
        mNumSteps = 1;
        mStepFlags = step0.mFlags;
        mDepthStencilFlags = step0.depthStencilFlags();
        assert (mStepFlags & GeometryStep.FLAG_PERFORM_SHADING) != 0;
    }

    public GeometryRenderer(String name, GeometryStep step0, GeometryStep step1) {
        mName = name;
        mStep0 = step0;
        mStep1 = step1;
        mStep2 = null;
        mNumSteps = 2;
        mStepFlags = step0.mFlags | step1.mFlags;
        mDepthStencilFlags = step0.depthStencilFlags() | step1.depthStencilFlags();
        assert (mStepFlags & GeometryStep.FLAG_PERFORM_SHADING) != 0;
    }

    public GeometryRenderer(String name, GeometryStep step0, GeometryStep step1, GeometryStep step2) {
        mName = name;
        mStep0 = step0;
        mStep1 = step1;
        mStep2 = step2;
        mNumSteps = 3;
        mStepFlags = step0.mFlags | step1.mFlags | step2.mFlags;
        mDepthStencilFlags = step0.depthStencilFlags() | step1.depthStencilFlags() | step2.depthStencilFlags();
        assert (mStepFlags & GeometryStep.FLAG_PERFORM_SHADING) != 0;
    }

    public GeometryStep step(int i) {
        assert i >= 0 && i < mNumSteps;
        return switch (i) {
            case 0 -> mStep0;
            case 1 -> mStep1;
            case 2 -> mStep2;
            default -> throw new IndexOutOfBoundsException(i);
        };
    }

    public int numSteps() {
        return mNumSteps;
    }

    public String name() {
        return mName;
    }

    public int depthStencilFlags() {
        return mDepthStencilFlags;
    }

    public boolean outsetBoundsForAA() {
        return (mStepFlags & GeometryStep.FLAG_OUTSET_BOUNDS_FOR_AA) != 0;
    }

    public boolean emitsPrimitiveColor() {
        return (mStepFlags & GeometryStep.FLAG_EMIT_PRIMITIVE_COLOR) != 0;
    }

    public boolean emitsCoverage() {
        return (mStepFlags & GeometryStep.FLAG_EMIT_COVERAGE) != 0;
    }
}
