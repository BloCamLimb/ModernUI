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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;

/**
 * A draw pass represents a render pass, with limited and sorted draw commands.
 * <p>
 * Created immutable.
 */
public class DrawPass implements AutoCloseable {

    //TODO move to somewhere else
    public static final int GEOMETRY_UNIFORM_BINDING = 0;
    public static final int FRAGMENT_UNIFORM_BINDING = 1;

    /**
     * Depth buffer is 16-bit, ensure no overflow.
     * DrawList is almost sorted, TimSort will be fast.
     */
    public static final int MAX_RENDER_STEPS = (1 << 16) - 1;
    /**
     * An invalid index for {@link UniformTracker}, also for pipeline index.
     *
     * @see #MAX_RENDER_STEPS
     */
    public static final int INVALID_INDEX = MAX_RENDER_STEPS + 1;

    private final DrawCommandList mCommandList;

    private final Rect2i mBounds;

    private final ObjectArrayList<GraphicsPipelineDesc> mPipelineDescs;
    private final ObjectArrayList<SamplerDesc> mSamplerDescs;

    private final ObjectArrayList<@SharedPtr ImageViewProxy> mTextures;

    private volatile @SharedPtr GraphicsPipeline[] mPipelines;
    private volatile @SharedPtr Sampler[] mSamplers;

    private DrawPass(DrawCommandList commandList, Rect2i bounds,
                     ObjectArrayList<GraphicsPipelineDesc> pipelineDescs,
                     ObjectArrayList<SamplerDesc> samplerDescs,
                     ObjectArrayList<@SharedPtr ImageViewProxy> textures) {
        mCommandList = commandList;
        mBounds = bounds;
        mPipelineDescs = pipelineDescs;
        mSamplerDescs = samplerDescs;
        mTextures = textures;
    }

    // backing store's width/height may not equal to device's width/height
    // currently we use the backing dimensions for scissor and viewport
    @Nullable
    public static DrawPass make(RecordingContext rContext,
                                DrawList drawList,
                                ImageViewProxy colorTarget,
                                ImageInfo deviceInfo) {

        var bufferManager = rContext.getDynamicBufferManager();

        if (bufferManager.hasMappingFailed()) {
            return null;
        }


        var commandList = new DrawCommandList();


        var pipelineToIndex = new HashMap<GraphicsPipelineDesc, Integer>();
        var indexToPipeline = new ObjectArrayList<GraphicsPipelineDesc>();
        Function<GraphicsPipelineDesc, Integer> pipelineAccumulator = desc -> {
            int index = indexToPipeline.size();
            indexToPipeline.add(desc.copy()); // store immutable descriptors
            return index;
        };

        var passBounds = new Rect2f();

        var geometryUniformTracker = new UniformTracker();
        var fragmentUniformTracker = new UniformTracker();

        SortKey[] keys = new SortKey[drawList.numSteps()];
        int keyIndex = 0;

        try (var textureDataGatherer = new TextureDataGatherer()) {
            var textureTracker = new TextureTracker();

            int surfaceHeight = colorTarget.getHeight();
            int surfaceOrigin = colorTarget.getOrigin();

            try (var uniformDataCache = new UniformDataCache();
                 var uniformDataGatherer = new UniformDataGatherer(
                         UniformDataGatherer.Std140Layout)) {

                var paintParamsKeyBuilder = new KeyBuilder();
                var lookupDesc = new GraphicsPipelineDesc();

                float projX = 2.0f / colorTarget.getWidth();
                float projY = -1.0f;
                float projZ = 2.0f / surfaceHeight;
                float projW = -1.0f;
                if (surfaceOrigin == Engine.SurfaceOrigin.kLowerLeft) {
                    projZ = -projZ;
                    projW = -projW;
                }

                for (var draw : drawList.mDraws) {

                    for (int stepIndex = 0; stepIndex < draw.mRenderer.numSteps(); stepIndex++) {
                        var step = draw.mRenderer.step(stepIndex);

                        paintParamsKeyBuilder.clear();
                        BlendMode finalBlendMode = null;

                        textureDataGatherer.reset();

                        // collect geometry data
                        uniformDataGatherer.reset();
                        // first add the 2D orthographic projection
                        uniformDataGatherer.write4f(projX, projY, projZ, projW);
                        step.writeUniformsAndTextures(draw, uniformDataGatherer, textureDataGatherer);
                        var geometryUniforms = uniformDataCache.insert(uniformDataGatherer.finish());

                        // collect fragment data and pipeline key
                        uniformDataGatherer.reset();
                        if (step.performsShading() && draw.mPaintParams != null) {
                            if (!(step.handlesSolidColor() && draw.mPaintParams.isSolidColor())) {
                                // Add fragment stages if this is the step that performs shading,
                                // and not a depth-only draw, and cannot simplify for solid color draw
                                draw.mPaintParams.toKey(null,
                                        paintParamsKeyBuilder,
                                        uniformDataGatherer,
                                        textureDataGatherer);
                                assert !paintParamsKeyBuilder.isEmpty();
                            }
                            finalBlendMode = draw.mPaintParams.getFinalBlendMode();
                        }
                        var fragmentUniforms = uniformDataCache.insert(uniformDataGatherer.finish());

                        // geometry texture samplers and then fragment texture samplers
                        // we build shader code and set binding points in this order as well
                        var textures = textureDataGatherer.finish();

                        int pipelineIndex = pipelineToIndex.computeIfAbsent(
                                lookupDesc.set(step, paintParamsKeyBuilder, finalBlendMode),
                                pipelineAccumulator);

                        var geometryUniformIndex = geometryUniformTracker.trackUniforms(
                                pipelineIndex,
                                geometryUniforms
                        );
                        var fragmentUniformIndex = fragmentUniformTracker.trackUniforms(
                                pipelineIndex,
                                fragmentUniforms
                        );

                        keys[keyIndex++] = new SortKey(
                                draw,
                                stepIndex,
                                pipelineIndex,
                                geometryUniformIndex,
                                fragmentUniformIndex,
                                textures
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
            Rect2ic lastScissor = new Rect2i(0, 0, deviceInfo.width(), deviceInfo.height());
            int lastPipelineIndex = INVALID_INDEX;
            float[] cachedSolidColor = new float[4];

            commandList.setScissor(lastScissor, surfaceHeight, surfaceOrigin);

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
                        commandList.setScissor(newScissor, surfaceHeight, surfaceOrigin);
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

                float[] solidColor = null;
                if (step.performsShading() && draw.mPaintParams != null) {
                    if (step.handlesSolidColor() &&
                            draw.mPaintParams.getSolidColor(deviceInfo, cachedSolidColor)) {
                        solidColor = cachedSolidColor;
                    }
                }

                step.writeMesh(drawWriter, draw, solidColor);

                if (bufferManager.hasMappingFailed()) {
                    return null;
                }
            }
            // Finish recording draw calls for any collected data at the end of the loop
            drawWriter.flush();
            commandList.finish();

            var bounds = new Rect2i();
            passBounds.roundOut(bounds);

            return new DrawPass(commandList, bounds,
                    indexToPipeline,
                    textureDataGatherer.detachSamplers(),
                    textureDataGatherer.detachTextures());
        }
    }

    public Rect2ic getBounds() {
        return mBounds;
    }

    public DrawCommandList getCommandList() {
        return mCommandList;
    }

    public boolean prepare(ResourceProvider resourceProvider,
                           RenderPassDesc renderPassDesc) {
        @SharedPtr GraphicsPipeline[] pipelines = new GraphicsPipeline[mPipelineDescs.size()];
        try {
            for (int i = 0; i < mPipelineDescs.size(); i++) {
                @SharedPtr
                var pipeline = resourceProvider.findOrCreateGraphicsPipeline(
                        mPipelineDescs.get(i),
                        renderPassDesc
                );
                if (pipeline == null) {
                    return false;
                }
                pipelines[i] = pipeline;
            }
        } finally {
            // We must release the objects that have already been created.
            mPipelines = pipelines;
            // The DrawPass may be long-lived on a Recording and we no longer need the GraphicPipelineDescs
            // once we've created pipelines, so we drop the storage for them here.
            mPipelineDescs.clear();
        }

        @SharedPtr Sampler[] samplers = new Sampler[mSamplerDescs.size()];
        try {
            for (int i = 0; i < mSamplerDescs.size(); i++) {
                @SharedPtr
                var sampler = resourceProvider.findOrCreateCompatibleSampler(
                        mSamplerDescs.get(i)
                );
                if (sampler == null) {
                    return false;
                }
                samplers[i] = sampler;
            }
        } finally {
            // We must release the objects that have already been created.
            mSamplers = samplers;
            // The DrawPass may be long-lived on a Recording and we no longer need the SamplerDescs
            // once we've created Samplers, so we drop the storage for them here.
            mSamplerDescs.clear();
        }

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
                    if (!commandBuffer.bindGraphicsPipeline(mPipelines[pipelineIndex])) {
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
                    int x = p.getInt();
                    int y = p.getInt();
                    int width = p.getInt();
                    int height = p.getInt();
                    commandBuffer.setScissor(x, y, width, height);
                }
                case DrawCommandList.CMD_BIND_UNIFORM_BUFFER -> {
                    int binding = p.getInt();
                    long offset = p.getLong();
                    long size = p.getLong();
                    commandBuffer.bindUniformBuffer(binding, (Buffer) oa[oi++], offset, size);
                }
                case DrawCommandList.CMD_BIND_TEXTURES -> {
                    int numBindings = p.getInt();
                    for (int binding = 0; binding < numBindings; binding++) {
                        @RawPtr
                        var texture = mTextures.get(p.getInt());
                        @RawPtr
                        var sampler = mSamplers[p.getInt()];
                        commandBuffer.bindTextureSampler(binding,
                                texture.getImage(),
                                sampler,
                                texture.getSwizzle());
                    }
                }
            }
        }
        //TODO track resources
        return true;
    }

    @Override
    public void close() {
        if (mPipelines != null) {
            for (int i = 0; i < mPipelines.length; i++) {
                mPipelines[i] = RefCnt.move(mPipelines[i]);
            }
        }
        if (mSamplers != null) {
            for (int i = 0; i < mSamplers.length; i++) {
                mSamplers[i] = RefCnt.move(mSamplers[i]);
            }
        }
        mTextures.forEach(RefCnt::unref);
        mTextures.clear();
    }

    /**
     * The sorting is used to minimize state change.
     * <p>
     * Sorting order:
     * painter's order, stencil disjoint set index,
     * render step index, pipeline index, geometry uniform index,
     * fragment uniform index, texture and sampler binding
     */
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

        // 52-50 step, 50-34 pipeline, 34-17 geometry uniform, 17-0 fragment uniform
        public static final int STEP_INDEX_OFFSET = 50;
        public static final int STEP_INDEX_MASK = (1 << 2) - 1;

        public static final int PIPELINE_INDEX_OFFSET = 34;
        public static final int PIPELINE_INDEX_MASK = (1 << 16) - 1;

        // requires one extra bit to represent invalid index
        public static final int GEOMETRY_UNIFORM_INDEX_OFFSET = 17;
        public static final int GEOMETRY_UNIFORM_INDEX_MASK = (1 << 17) - 1;
        public static final int FRAGMENT_UNIFORM_INDEX_OFFSET = 0;
        public static final int FRAGMENT_UNIFORM_INDEX_MASK = (1 << 17) - 1;

        private final Draw mDraw;
        // 32-16 painter's order, 16-0 stencil disjoint set index
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
