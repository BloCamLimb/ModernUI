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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * Descriptor of a graphics pipeline in Granite Renderer.
 */
public final class GraphicsPipelineDesc extends PipelineDesc {

    private GeometryStep mGeometryStep;
    private Key mPaintParamsKey;
    @Nullable
    private BlendMode mFinalBlendMode;
    private boolean mUseFastSolidColor;

    public GraphicsPipelineDesc() {
    }

    public GraphicsPipelineDesc(GeometryStep geometryStep,
                                Key paintParamsKey,
                                @Nullable BlendMode finalBlendMode,
                                boolean useFastSolidColor) {
        mGeometryStep = geometryStep;
        mPaintParamsKey = paintParamsKey;
        mFinalBlendMode = finalBlendMode;
        mUseFastSolidColor = useFastSolidColor;
    }

    public GraphicsPipelineDesc set(GeometryStep geometryStep,
                                    KeyBuilder paintParamsKey,
                                    @Nullable BlendMode finalBlendMode,
                                    boolean useFastSolidColor) {
        mGeometryStep = geometryStep;
        mPaintParamsKey = paintParamsKey;
        mFinalBlendMode = finalBlendMode;
        mUseFastSolidColor = useFastSolidColor;
        return this;
    }

    public GeometryStep geomStep() {
        return mGeometryStep;
    }

    public Key getPaintParamsKey() {
        return mPaintParamsKey;
    }

    @Nullable
    public BlendMode getFinalBlendMode() {
        return mFinalBlendMode;
    }

    public boolean usesFastSolidColor() {
        return mUseFastSolidColor;
    }

    public boolean mayRequireLocalCoords() {
        return !mUseFastSolidColor &&
                (mPaintParamsKey.size() != 1 ||
                        mPaintParamsKey.get(0) != FragmentStage.kSolidColorShader_BuiltinStageID);
    }

    private FragmentNode createNode(ShaderCodeSource codeSource,
                                    StringBuilder label,
                                    int[] currentStageIndex) {
        assert currentStageIndex[0] < mPaintParamsKey.size();
        int index = currentStageIndex[0]++;
        int id = mPaintParamsKey.get(index);

        FragmentStage stage = codeSource.findStage(id);
        if (stage == null) {
            return null;
        }

        String name = stage.name();
        if (name.endsWith("Shader")) {
            label.append(name, 0, name.length() - 6);
        } else {
            label.append(name);
        }

        FragmentNode[] children;
        if (stage.mNumChildren > 0) {
            children = new FragmentNode[stage.mNumChildren];
            label.append(" [ ");
            for (int i = 0; i < stage.mNumChildren; i++) {
                FragmentNode child = createNode(codeSource, label, currentStageIndex);
                if (child == null) {
                    return null;
                }
                children[i] = child;
            }
            label.append("]");
        } else {
            children = FragmentNode.NO_CHILDREN;
        }
        label.append(" ");

        return new FragmentNode(
                stage,
                children,
                id,
                index
        );
    }

    public FragmentNode[] getRootNodes(ShaderCodeSource codeSource,
                                       StringBuilder label) {
        final int keySize = mPaintParamsKey.size();

        var roots = new ObjectArrayList<FragmentNode>(7);
        int[] currentIndex = {0};
        while (currentIndex[0] < keySize) {
            FragmentNode root = createNode(codeSource, label, currentIndex);
            if (root == null) {
                label.setLength(0);
                return FragmentNode.NO_CHILDREN;
            }
            roots.add(root);
        }

        return roots.toArray(FragmentNode.NO_CHILDREN);
    }

    public boolean isDepthOnlyPass() {
        boolean depthOnly = mPaintParamsKey.isEmpty() && !mUseFastSolidColor;
        assert depthOnly == (mFinalBlendMode == null);
        return depthOnly;
    }

    @Override
    public GraphicsPipelineInfo createGraphicsPipelineInfo(Device device) {
        PipelineBuilder pipelineBuilder = new PipelineBuilder(device, this);
        return pipelineBuilder.build();
    }

    @Override
    public byte getPrimitiveType() {
        return mGeometryStep.primitiveType();
    }

    @Override
    public BlendInfo getBlendInfo() {
        if (mFinalBlendMode != null) {
            var info = BlendInfo.getSimpleBlendInfo(mFinalBlendMode);
            return info != null ? info : BlendInfo.BLEND_SRC_OVER;
        } else {
            return BlendInfo.BLEND_DST;
        }
    }

    @Override
    public DepthStencilSettings getDepthStencilSettings() {
        return mGeometryStep.depthStencilSettings();
    }

    @Override
    public GraphicsPipelineDesc copy() {
        if (mPaintParamsKey instanceof KeyBuilder keyBuilder) {
            // at most one, no recursive copy
            return new GraphicsPipelineDesc(mGeometryStep, keyBuilder.toStorageKey(),
                    mFinalBlendMode, mUseFastSolidColor);
        }
        return this;
    }

    @Override
    public int hashCode() {
        int result = mGeometryStep.uniqueID();
        result = 31 * result + mPaintParamsKey.hashCode();
        result = 31 * result + Objects.hashCode(mFinalBlendMode);
        result = 31 * result + Boolean.hashCode(mUseFastSolidColor);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof GraphicsPipelineDesc desc) {
            return mGeometryStep.uniqueID() == desc.mGeometryStep.uniqueID() &&
                    mUseFastSolidColor == desc.mUseFastSolidColor &&
                    Objects.equals(mPaintParamsKey, desc.mPaintParamsKey) &&
                    Objects.equals(mFinalBlendMode, desc.mFinalBlendMode);
        }
        return false;
    }
}
