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

import icyllis.arc3d.core.BlendMode;
import icyllis.arc3d.engine.*;

import javax.annotation.Nullable;
import java.util.Objects;

public final class GraphicsPipelineDesc extends PipelineDesc {

    private GeometryStep mGeometryStep;
    private Key mPaintParamsKey;
    @Nullable
    private BlendMode mFinalBlendMode;

    public GraphicsPipelineDesc() {
    }

    public GraphicsPipelineDesc(GeometryStep geometryStep,
                                Key paintParamsKey,
                                @Nullable BlendMode finalBlendMode) {
        mGeometryStep = geometryStep;
        mPaintParamsKey = paintParamsKey;
        mFinalBlendMode = finalBlendMode;
    }

    public GraphicsPipelineDesc set(GeometryStep geometryStep,
                                    KeyBuilder paintParamsKey,
                                    @Nullable BlendMode finalBlendMode) {
        mGeometryStep = geometryStep;
        mPaintParamsKey = paintParamsKey;
        mFinalBlendMode = finalBlendMode;
        return this;
    }

    public GeometryStep geomStep() {
        return mGeometryStep;
    }

    public Key getPaintParamsKey() {
        return mPaintParamsKey;
    }

    @Override
    public byte getPrimitiveType() {
        return mGeometryStep.primitiveType();
    }

    @Override
    public GraphicsPipelineInfo createGraphicsPipelineInfo(Caps caps) {
        var info = new GraphicsPipelineInfo();
        info.mPrimitiveType = getPrimitiveType();
        info.mPipelineLabel = mGeometryStep.name();
        info.mInputLayout = mGeometryStep.getInputLayout();
        PipelineBuilder pipelineBuilder = new PipelineBuilder(caps, mGeometryStep);
        pipelineBuilder.build();
        info.mVertSource = pipelineBuilder.getVertCode();
        info.mFragSource = pipelineBuilder.getFragCode();
        return info;
    }

    @Override
    public GraphicsPipelineDesc copy() {
        if (mPaintParamsKey instanceof KeyBuilder keyBuilder) {
            // at most one, no recursive copy
            return new GraphicsPipelineDesc(mGeometryStep, keyBuilder.toStorageKey(), mFinalBlendMode);
        }
        return this;
    }

    @Override
    public int hashCode() {
        int result = mGeometryStep.classID();
        result = 31 * result + mPaintParamsKey.hashCode();
        result = 31 * result + Objects.hashCode(mFinalBlendMode);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof GraphicsPipelineDesc desc) {
            return mGeometryStep.classID() == desc.mGeometryStep.classID() &&
                    Objects.equals(mPaintParamsKey, desc.mPaintParamsKey) &&
                    Objects.equals(mFinalBlendMode, desc.mFinalBlendMode);
        }
        return false;
    }
}
