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

import icyllis.arc3d.core.Matrix4;

import java.util.ArrayList;

public class DrawList {

    private final ArrayList<Matrix4> mTransforms = new ArrayList<>();
    final ArrayList<Draw> mDraws = new ArrayList<>();
    private int mNumSteps;

    private Matrix4 getStableTransform(Matrix4 modelView) {
        Matrix4 last;
        if (mTransforms.isEmpty() || !(last = mTransforms.get(mTransforms.size() - 1)).equals(modelView)) {
            var copy = modelView.clone();
            mTransforms.add(copy);
            return copy;
        }
        return last;
    }

    public void recordDrawOp(Draw draw) {
        draw.mTransform = getStableTransform(draw.mTransform);

        mDraws.add(draw);

        mNumSteps += draw.mRenderer.numSteps();
    }

    public int numSteps() {
        return mNumSteps;
    }
}
