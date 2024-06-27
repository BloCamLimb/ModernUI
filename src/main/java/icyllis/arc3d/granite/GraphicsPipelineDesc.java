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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

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

    private FragmentNode createNode(ShaderCodeSource codeSource,
                                    int[] currentStageIndex) {
        assert currentStageIndex[0] < mPaintParamsKey.size();
        int index = currentStageIndex[0]++;
        int id = mPaintParamsKey.get(index);

        FragmentStage stage = codeSource.findStage(id);
        if (stage == null) {
            return null;
        }

        FragmentNode[] children = stage.mNumChildren > 0
                ? new FragmentNode[stage.mNumChildren]
                : FragmentNode.NO_CHILDREN;
        for (int i = 0; i < stage.mNumChildren; i++) {
            FragmentNode child = createNode(codeSource, currentStageIndex);
            if (child == null) {
                return null;
            }
            children[i] = child;
        }

        return new FragmentNode(
                stage,
                children,
                id,
                index
        );
    }

    public FragmentNode[] getRootNodes(ShaderCodeSource codeSource) {
        final int keySize = mPaintParamsKey.size();

        var roots = new ObjectArrayList<FragmentNode>(7);
        int[] currentIndex = {0};
        while (currentIndex[0] < keySize) {
            FragmentNode root = createNode(codeSource, currentIndex);
            if (root == null) {
                return FragmentNode.NO_CHILDREN;
            }
            roots.add(root);
        }

        return roots.toArray(FragmentNode.NO_CHILDREN);
    }

    @Override
    public byte getPrimitiveType() {
        return mGeometryStep.primitiveType();
    }

    @Override
    public GraphicsPipelineInfo createGraphicsPipelineInfo(Device device) {
        var info = new GraphicsPipelineInfo();
        info.mPrimitiveType = getPrimitiveType();
        info.mPipelineLabel = mGeometryStep.name();
        info.mInputLayout = mGeometryStep.getInputLayout();
        PipelineBuilder pipelineBuilder = new PipelineBuilder(device, this);
        pipelineBuilder.build();
        info.mVertSource = pipelineBuilder.getVertCode();
        info.mFragSource = pipelineBuilder.getFragCode();
        return info;
    }

    @Override
    public BlendInfo getBlendInfo() {
        return BlendInfo.SRC_OVER;
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
