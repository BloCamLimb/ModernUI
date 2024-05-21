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

package icyllis.arc3d.engine;

public final class GeometryRenderer {

    /**
     * The maximum number of render steps that any Renderer is allowed to have.
     * For example, Stencil-then-Cover method has multiple steps.
     */
    public static final int MAX_RENDER_STEPS = 4;

    // we have limited number of geometry steps, enumerate here
    private GeometryStep mStep0;
    private GeometryStep mStep1;
    private GeometryStep mStep2;
    private GeometryStep mStep3;
    private int mNumSteps;

    private String mName;

    public GeometryStep step(int i) {
        assert i >= 0 && i < mNumSteps;
        return switch (i) {
            case 0 -> mStep0;
            case 1 -> mStep1;
            case 2 -> mStep2;
            default -> mStep3;
        };
    }

    public int numSteps() {
        return mNumSteps;
    }
}
