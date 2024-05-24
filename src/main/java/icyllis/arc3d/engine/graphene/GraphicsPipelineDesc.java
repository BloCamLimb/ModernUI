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

package icyllis.arc3d.engine.graphene;

import icyllis.arc3d.engine.GeometryStep;
import icyllis.arc3d.engine.Key;

import javax.annotation.Nullable;
import java.util.Objects;

public final class GraphicsPipelineDesc {

    private final GeometryStep mGeometryStep;
    private final Key mPaintParamsKey;

    public GraphicsPipelineDesc(GeometryStep geometryStep) {
        this(geometryStep, null);
    }

    public GraphicsPipelineDesc(GeometryStep geometryStep, @Nullable Key paintParamsKey) {
        mGeometryStep = geometryStep;
        mPaintParamsKey = paintParamsKey;
    }

    public GeometryStep geomStep() {
        return mGeometryStep;
    }

    public Key getPaintParamsKey() {
        return mPaintParamsKey;
    }

    @Override
    public int hashCode() {
        int result = mGeometryStep.classID();
        result = 31 * result + (mPaintParamsKey != null ? mPaintParamsKey.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof GraphicsPipelineDesc desc) {
            return mGeometryStep.classID() == desc.mGeometryStep.classID() &&
                    Objects.equals(mPaintParamsKey, desc.mPaintParamsKey);
        }
        return false;
    }
}
