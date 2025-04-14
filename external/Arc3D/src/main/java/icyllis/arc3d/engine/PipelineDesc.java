/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

/**
 * Abstract class that provides key and full information about a graphics pipeline
 * or a compute pipeline, except for render pass information.
 * <p>
 * Subclass must implement {@link #hashCode()} and {@link #equals(Object)}.
 */
//TODO to be reviewed
public abstract class PipelineDesc {

    public static final int NO_DYNAMIC_STATE = 0;
    public static final int DYNAMIC_COLOR_BLEND_STATE = 1;

    //TODO temporary, to be reviewed
    public static final class UniformBlockInfo {
        // ShaderFlags
        public final int mVisibility;
        public final int mBinding;
        public final String mBlockName;

        public UniformBlockInfo(int visibility, int binding, String blockName) {
            mVisibility = visibility;
            mBinding = binding;
            mBlockName = blockName;
        }
    }

    public static final class SamplerInfo {
        // ShaderFlags
        public final int mVisibility;
        public final int mBinding;
        public final String mName;

        public SamplerInfo(int visibility, int binding, String name) {
            mVisibility = visibility;
            mBinding = binding;
            mName = name;
        }
    }

    public static final class GraphicsPipelineInfo {
        public byte mPrimitiveType;
        public VertexInputLayout mInputLayout;
        public String mInputLayoutLabel;
        //TODO replace full 'source' with IR + main() source
        public String mVertSource;
        public String mVertLabel;
        public String mFragSource;
        public String mFragLabel;
        public BlendInfo mBlendInfo;
        public DepthStencilSettings mDepthStencilSettings;
        public UniformBlockInfo[] mUniformBlockInfos;
        public SamplerInfo[] mSamplerInfos;
        public String mPipelineLabel;
    }

    /**
     * Generates all info used to create graphics pipeline.
     */
    public GraphicsPipelineInfo createGraphicsPipelineInfo(Device device) {
        throw new IllegalStateException();
    }

    public byte getPrimitiveType() {
        return 0;
    }

    public BlendInfo getBlendInfo() {
        return null;
    }

    public DepthStencilSettings getDepthStencilSettings() {
        return null;
    }

    /**
     * Returns a bitfield that represents dynamic states of this pipeline.
     * These dynamic states must be supported by the backend.
     * <p>
     * Viewport and scissor are always dynamic states.
     */
    //TODO not implemented yet, meaningful in Vulkan
    public int getDynamicStates() {
        return NO_DYNAMIC_STATE;
    }

    /**
     * Makes a deep copy of this desc, it must be immutable before return.
     * No need to make its fields visible to all threads.
     * If this desc is already immutable then subclass may return this.
     * <p>
     * The {@link #hashCode()} and {@link #equals(Object)} of this and return
     * value must be consistent.
     */
    public abstract PipelineDesc copy();
}
