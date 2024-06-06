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

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.ToIntFunction;

/**
 * A draw pass represents a render pass, with limited and sorted draw commands.
 * <p>
 * Created immutable.
 */
public class DrawPass {

    //TODO move to somewhere else
    public static final int GEOMETRY_UNIFORM_BINDING = 0;
    public static final int FRAGMENT_UNIFORM_BINDING = 1;

    /**
     * An invalid index for {@link UniformTracker}.
     *
     * @see DrawList#MAX_RENDER_STEPS
     */
    public static final int INVALID_INDEX = 1 << 16;

    private final DrawCommandList mCommandList;

    private final ArrayList<GraphicsPipelineDesc> mPipelineDescs;
    private final IntArrayList mSamplerDescs;

    private final ObjectArrayList<@SharedPtr ImageViewProxy> mSampledImages;

    private @SharedPtr GraphicsPipeline[] mPipelines;
    private @SharedPtr Sampler[] mSamplers;

    private DrawPass(DrawCommandList commandList,
                     ArrayList<GraphicsPipelineDesc> pipelineDescs,
                     ObjectArrayList<@SharedPtr ImageViewProxy> sampledImages,
                     IntArrayList samplerDescs) {
        mCommandList = commandList;
        mPipelineDescs = pipelineDescs;
        mSampledImages = sampledImages;
        mSamplerDescs = samplerDescs;
    }

    public static DrawPass make(RecordingContext rContext,
                                DrawList drawList,
                                ImageViewProxy colorTarget,
                                byte loadStoreOps,
                                float[] clearColor) {

        var bufferManager = rContext.getDynamicBufferManager();

        if (bufferManager.hasMappingFailed()) {
            return null;
        }


        var commandList = new DrawCommandList();


        var pipelineToIndexMap
                = new Object2IntOpenHashMap<GraphicsPipelineDesc>();
        var pipelineDescs = new ArrayList<GraphicsPipelineDesc>();
        ToIntFunction<GraphicsPipelineDesc> insertPipelineDesc = desc -> {
            pipelineDescs.add(desc);
            return pipelineDescs.size() - 1;
        };

        var passBounds = new Rect2f();

        var geometryUniformTracker = new UniformTracker();
        var fragmentUniformTracker = new UniformTracker();

        var textureDataGatherer = new TextureDataGatherer();
        var textureTracker = new TextureTracker();

        SortKey[] keys = new SortKey[drawList.numSteps()];
        int keyIndex = 0;

        try (var uniformDataGatherer = new UniformDataGatherer(
                UniformDataGatherer.Std140Layout);
             var uniformDataCache = new UniformDataCache()) {

            for (var draw : drawList.mDraws) {

                for (int stepIndex = 0; stepIndex < draw.mRenderer.numSteps(); stepIndex++) {
                    var step = draw.mRenderer.step(stepIndex);

                    int pipelineIndex = pipelineToIndexMap.computeIfAbsent(new GraphicsPipelineDesc(step),
                            insertPipelineDesc);

                    uniformDataGatherer.reset();
                    textureDataGatherer.reset();
                    step.writeUniformsAndTextures(draw, uniformDataGatherer, textureDataGatherer);

                    var geometryUniforms = uniformDataCache.insert(uniformDataGatherer.finish());
                    var geometryTextures = textureDataGatherer.finish();

                    var geometryUniformIndex = geometryUniformTracker.trackUniforms(
                            pipelineIndex,
                            geometryUniforms
                    );

                    keys[keyIndex++] = new SortKey(
                            draw,
                            stepIndex,
                            pipelineIndex,
                            geometryUniformIndex,
                            INVALID_INDEX,
                            geometryTextures
                    );
                }

                passBounds.joinNoCheck(draw.mDrawBounds);
            }

            if (!geometryUniformTracker.writeUniforms(bufferManager) ||
                    !fragmentUniformTracker.writeUniforms(bufferManager)) {
                return null;
            }
        }

        assert keyIndex == keys.length;
        // TimSort - stable
        Arrays.sort(keys);

        MeshDrawWriter drawWriter = new MeshDrawWriter(bufferManager,
                commandList);
        Rect2ic lastScissor = new Rect2i(0, 0, colorTarget.getWidth(), colorTarget.getHeight());
        int lastPipelineIndex = INVALID_INDEX;

        for (var key : keys) {
            var draw = key.mDraw;
            var step = key.step();
            int pipelineIndex = key.pipelineIndex();

            boolean pipelineStateChange = pipelineIndex != lastPipelineIndex;

            Rect2ic newScissor = !draw.mScissorRect.equals(lastScissor)
                    ? draw.mScissorRect : null;
            boolean geometryBindingChange = geometryUniformTracker.setCurrentUniforms(
                    pipelineIndex, key.geometryUniformIndex()
            );
            boolean fragmentBindingChange = fragmentUniformTracker.setCurrentUniforms(
                    pipelineIndex, key.fragmentUniformIndex()
            );
            boolean textureBindingChange = textureTracker.setCurrentTextures(key.mTextures);

            boolean dynamicStateChange = newScissor != null ||
                    geometryBindingChange ||
                    fragmentBindingChange ||
                    textureBindingChange;

            if (pipelineStateChange) {
                drawWriter.newPipelineState(
                        step.vertexBinding(),
                        step.instanceBinding(),
                        step.vertexStride(),
                        step.instanceStride()
                );
            } else if (dynamicStateChange) {
                drawWriter.newDynamicState();
            }

            // Make state changes before accumulating new draw data
            if (pipelineStateChange) {
                commandList.bindGraphicsPipeline(pipelineIndex);
                lastPipelineIndex = pipelineIndex;
            }
            if (dynamicStateChange) {
                if (newScissor != null) {
                    commandList.setScissor(newScissor);
                    lastScissor = newScissor;
                }
                if (geometryBindingChange) {
                    geometryUniformTracker.bindUniforms(
                            GEOMETRY_UNIFORM_BINDING,
                            commandList
                    );
                }
                if (fragmentBindingChange) {
                    fragmentUniformTracker.bindUniforms(
                            FRAGMENT_UNIFORM_BINDING,
                            commandList
                    );
                }
                if (textureBindingChange) {
                    textureTracker.bindTextures(commandList);
                }
            }

            step.writeMesh(drawWriter, draw, new float[]{1, 1, 1, 1});

            if (bufferManager.hasMappingFailed()) {
                return null;
            }
        }
        // Finish recording draw calls for any collected data at the end of the loop
        drawWriter.flush();
        commandList.finish();


        DrawPass pass = new DrawPass(commandList,
                pipelineDescs,
                textureDataGatherer.mIndexToTexture,
                textureDataGatherer.mIndexToSampler);

        return pass;
    }

    @RawPtr
    public GraphicsPipeline getPipeline(int index) {
        return mPipelines[index];
    }

    public DrawCommandList getCommandList() {
        return mCommandList;
    }

    public List<@SharedPtr ImageViewProxy> getSampledImages() {
        return mSampledImages;
    }

    public boolean prepare(ResourceProvider resourceProvider,
                           RenderPassDesc renderPassDesc) {
        mPipelines = new GraphicsPipeline[mPipelineDescs.size()];
        for (int i = 0; i < mPipelineDescs.size(); i++) {
            var pipeline = resourceProvider.findOrCreateGraphicsPipeline(
                    mPipelineDescs.get(i),
                    renderPassDesc
            );
            if (pipeline == null) {
                return false;
            }
            mPipelines[i] = pipeline;
        }
        mPipelineDescs.clear();

        return true;
    }

    public boolean execute(CommandBuffer commandBuffer) {
        var cmdList = getCommandList();
        var p = cmdList.mPrimitives;
        var oa = cmdList.mPointers.elements();
        int oi = 0;
        while (p.hasRemaining()) {
            switch (p.getInt()) {
                case DrawCommandList.CMD_BIND_GRAPHICS_PIPELINE -> {
                    int pipelineIndex = p.getInt();
                    if (!commandBuffer.bindGraphicsPipeline(getPipeline(pipelineIndex))) {
                        return false;
                    }
                }
                case DrawCommandList.CMD_DRAW -> {
                    int vertexCount = p.getInt();
                    int baseVertex = p.getInt();
                    commandBuffer.draw(vertexCount, baseVertex);
                }
                case DrawCommandList.CMD_DRAW_INDEXED -> {
                    int indexCount = p.getInt();
                    int baseIndex = p.getInt();
                    int baseVertex = p.getInt();
                    commandBuffer.drawIndexed(indexCount, baseIndex, baseVertex);
                }
                case DrawCommandList.CMD_DRAW_INSTANCED -> {
                    int instanceCount = p.getInt();
                    int baseInstance = p.getInt();
                    int vertexCount = p.getInt();
                    int baseVertex = p.getInt();
                    commandBuffer.drawInstanced(instanceCount, baseInstance, vertexCount, baseVertex);
                }
                case DrawCommandList.CMD_DRAW_INDEXED_INSTANCED -> {
                    int indexCount = p.getInt();
                    int baseIndex = p.getInt();
                    int instanceCount = p.getInt();
                    int baseInstance = p.getInt();
                    int baseVertex = p.getInt();
                    commandBuffer.drawIndexedInstanced(indexCount, baseIndex, instanceCount, baseInstance, baseVertex);
                }
                case DrawCommandList.CMD_BIND_INDEX_BUFFER -> {
                    int indexType = p.getInt();
                    long offset = p.getLong();
                    commandBuffer.bindIndexBuffer(indexType, (Buffer) oa[oi++], offset);
                }
                case DrawCommandList.CMD_BIND_VERTEX_BUFFER -> {
                    int binding = p.getInt();
                    long offset = p.getLong();
                    commandBuffer.bindVertexBuffer(binding, (Buffer) oa[oi++], offset);
                }
                case DrawCommandList.CMD_SET_SCISSOR -> {
                    int left = p.getInt();
                    int top = p.getInt();
                    int right = p.getInt();
                    int bottom = p.getInt();
                    commandBuffer.setScissor(left, top, right, bottom);
                }
                case DrawCommandList.CMD_BIND_UNIFORM_BUFFER -> {
                    int binding = p.getInt();
                    long offset = p.getLong();
                    long size = p.getLong();
                    commandBuffer.bindUniformBuffer(binding, (Buffer) oa[oi++], offset, size);
                }
                case DrawCommandList.CMD_BIND_TEXTURES -> {
                    int[] textures = (int[]) oa[oi++];
                    for (int i = 0; i < textures.length; i += 2) {
                        int binding = i >> 1;
                        var textureView = mSampledImages.get(textures[i]);
                        var sampler = mSamplers[textures[i|1]];
                        commandBuffer.bindTextureSampler(binding,
                                textureView.getImage(),
                                sampler,
                                textureView.getSwizzle());
                    }
                }
            }
        }
        //TODO track resources
        return true;
    }

    public void close() {
        if (mPipelines != null) {
            for (int i = 0; i < mPipelines.length; i++) {
                mPipelines[i] = RefCnt.move(mPipelines[i]);
            }
        }
    }

    public static final class SortKey implements Comparable<SortKey> {

        public static final int PAINTERS_ORDER_OFFSET = 32;
        public static final int PAINTERS_ORDER_MASK = (1 << 16) - 1;

        public static final int STENCIL_INDEX_OFFSET = 16;
        public static final int STENCIL_INDEX_MASK = (1 << 16) - 1;

        static {
            //noinspection ConstantValue
            assert DrawOrder.PAINTERS_ORDER_SHIFT == PAINTERS_ORDER_OFFSET &&
                    DrawOrder.STENCIL_INDEX_SHIFT == STENCIL_INDEX_OFFSET;
        }

        // 64-62 step, 62-34 pipeline, 34-17 geometry, 17-0 fragment
        public static final int STEP_INDEX_OFFSET = 62;
        public static final int STEP_INDEX_MASK = (1 << 2) - 1;

        public static final int PIPELINE_INDEX_OFFSET = 34;
        public static final int PIPELINE_INDEX_MASK = (1 << 28) - 1;

        // requires one extra bit
        public static final int GEOMETRY_UNIFORM_INDEX_OFFSET = 17;
        public static final int GEOMETRY_UNIFORM_INDEX_MASK = (1 << 17) - 1;
        public static final int FRAGMENT_UNIFORM_INDEX_OFFSET = 0;
        public static final int FRAGMENT_UNIFORM_INDEX_MASK = (1 << 17) - 1;

        private final Draw mDraw;
        private final int mOrderKey;
        private final long mPipelineKey;
        private final int[] mTextures;

        public SortKey(Draw draw,
                       int stepIndex,
                       int pipelineIndex,
                       int geometryUniformIndex,
                       int fragmentUniformIndex,
                       int[] textures) {
            mDraw = draw;
            // the 16-48 bits are just we want
            mOrderKey = (int) (draw.mDrawOrder >>> DrawOrder.STENCIL_INDEX_SHIFT);
            assert (stepIndex & STEP_INDEX_MASK) == stepIndex;
            mPipelineKey = ((long) stepIndex << STEP_INDEX_OFFSET) |
                    ((long) pipelineIndex << PIPELINE_INDEX_OFFSET) |
                    ((long) geometryUniformIndex << GEOMETRY_UNIFORM_INDEX_OFFSET) |
                    ((long) fragmentUniformIndex << FRAGMENT_UNIFORM_INDEX_OFFSET);
            mTextures = textures;
        }

        public GeometryStep step() {
            return mDraw.mRenderer.step(
                    (int) ((mPipelineKey >>> STEP_INDEX_OFFSET) & STEP_INDEX_MASK));
        }

        public int pipelineIndex() {
            return (int) ((mPipelineKey >>> PIPELINE_INDEX_OFFSET) & PIPELINE_INDEX_MASK);
        }

        public int geometryUniformIndex() {
            return (int) ((mPipelineKey >>> GEOMETRY_UNIFORM_INDEX_OFFSET) & GEOMETRY_UNIFORM_INDEX_MASK);
        }

        public int fragmentUniformIndex() {
            return (int) ((mPipelineKey >>> FRAGMENT_UNIFORM_INDEX_OFFSET) & FRAGMENT_UNIFORM_INDEX_MASK);
        }

        @Override
        public int compareTo(@Nonnull SortKey o) {
            int res = Integer.compareUnsigned(mOrderKey, o.mOrderKey);
            if (res != 0) return res;
            res = Long.compareUnsigned(mPipelineKey, o.mPipelineKey);
            if (res != 0) return res;
            return Arrays.compare(mTextures, o.mTextures);
        }
    }
}
