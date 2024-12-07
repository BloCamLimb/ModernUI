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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;

/**
 * A draw pass represents a render pass, with limited and sorted draw commands.
 * <p>
 * Created immutable.
 *
 * @see PipelineBuilder
 */
public class DrawPass implements AutoCloseable {

    public static final int GEOMETRY_UNIFORM_BLOCK_BINDING = 0; // i.e. RenderBlock/StepBlock
    public static final int FRAGMENT_UNIFORM_BLOCK_BINDING = 1; // i.e. PaintBlock/EffectBlock/ShadingBlock

    public static final String GEOMETRY_UNIFORM_BLOCK_NAME = "GeometryUniforms";
    public static final String FRAGMENT_UNIFORM_BLOCK_NAME = "FragmentUniforms";

    /**
     * Depth buffer is 16-bit, ensure no overflow.
     * The theoretic max for this value is 65535, but we see markedly better
     * performance with smaller values
     */
    public static final int MAX_RENDER_STEPS = (1 << 12);
    /**
     * An invalid index for {@link UniformTracker}, also for pipeline index.
     *
     * @see #MAX_RENDER_STEPS
     */
    public static final int INVALID_INDEX = MAX_RENDER_STEPS + 1;

    private final DrawCommandList mCommandList;

    private final Rect2i mBounds;
    private final int mDepthStencilFlags;

    private final ObjectArrayList<GraphicsPipelineDesc> mPipelineDescs;
    private final ObjectArrayList<SamplerDesc> mSamplerDescs;

    private final ObjectArrayList<@SharedPtr ImageViewProxy> mTextures;

    private volatile @SharedPtr GraphicsPipeline[] mPipelines;
    private volatile @SharedPtr Sampler[] mSamplers;

    private DrawPass(DrawCommandList commandList, Rect2i bounds, int depthStencilFlags,
                     ObjectArrayList<GraphicsPipelineDesc> pipelineDescs,
                     ObjectArrayList<SamplerDesc> samplerDescs,
                     ObjectArrayList<@SharedPtr ImageViewProxy> textures) {
        mCommandList = commandList;
        mBounds = bounds;
        mDepthStencilFlags = depthStencilFlags;
        mPipelineDescs = pipelineDescs;
        mSamplerDescs = samplerDescs;
        mTextures = textures;
    }

    /**
     * Backing store's width/height may not equal to device's width/height,
     * currently we use the backing dimensions for scissor and viewport.
     * All the parameters are raw pointers and read-only.
     * <p>
     * The first uniform variable in geometry block must be a projection vector,
     * see {@link PipelineBuilder}.
     */
    @Nullable
    public static DrawPass make(RecordingContext context,
                                ObjectArrayList<Draw> drawList,
                                int numSteps,
                                @RawPtr ImageViewProxy targetView,
                                ImageInfo deviceInfo) {

        var bufferManager = context.getDynamicBufferManager();

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

        var passBounds = new Rect2f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        int depthStencilFlags = Engine.DepthStencilFlags.kNone;

        var geometryUniformTracker = new UniformTracker();
        var fragmentUniformTracker = new UniformTracker();

        SortKey[] keys = new SortKey[numSteps];
        int keyIndex = 0;

        try (var textureDataGatherer = new TextureDataGatherer();
             var drawWriter = new MeshDrawWriter(bufferManager, commandList)) {
            var textureTracker = new TextureTracker();

            int surfaceHeight = targetView.getHeight();
            int surfaceOrigin = targetView.getOrigin();

            try (var uniformDataCache = new UniformDataCache();
                 var uniformDataGatherer = new UniformDataGatherer(
                         UniformDataGatherer.Std140Layout)) {

                var paintParamsKeyBuilder = new KeyBuilder();
                var lookupDesc = new GraphicsPipelineDesc();

                float projX = 2.0f / targetView.getWidth();
                float projY = -1.0f;
                float projZ = 2.0f / surfaceHeight;
                float projW = -1.0f;
                if (surfaceOrigin == Engine.SurfaceOrigin.kLowerLeft) {
                    projZ = -projZ;
                    projW = -projW;
                }

                var keyContext = new KeyContext(context, deviceInfo);

                for (var draw : drawList) {

                    for (int stepIndex = 0; stepIndex < draw.mRenderer.numSteps(); stepIndex++) {
                        var step = draw.mRenderer.step(stepIndex);

                        paintParamsKeyBuilder.clear();
                        BlendMode finalBlendMode = null;
                        boolean useFastSolidColor = false;

                        textureDataGatherer.reset();

                        // collect fragment data and pipeline key
                        uniformDataGatherer.reset();
                        if (step.performsShading() && draw.mPaintParams != null) {
                            if (!(step.handlesSolidColor() && draw.mPaintParams.isSolidColor())) {
                                // Add fragment stages if this is the step that performs shading,
                                // and not a depth-only draw, and cannot simplify for solid color draw
                                keyContext.reset(draw.mPaintParams);
                                draw.mPaintParams.appendToKey(keyContext,
                                        paintParamsKeyBuilder,
                                        uniformDataGatherer,
                                        textureDataGatherer);
                                assert !paintParamsKeyBuilder.isEmpty();
                            } else {
                                useFastSolidColor = true;
                            }
                            finalBlendMode = draw.mPaintParams.getFinalBlendMode();
                        }
                        var fragmentUniforms = uniformDataCache.insert(uniformDataGatherer.finish());

                        int pipelineIndex = pipelineToIndex.computeIfAbsent(
                                lookupDesc.set(step, paintParamsKeyBuilder, finalBlendMode, useFastSolidColor),
                                pipelineAccumulator);

                        // collect geometry data
                        uniformDataGatherer.reset();
                        // first add the 2D orthographic projection
                        uniformDataGatherer.write4f(projX, projY, projZ, projW);
                        step.writeUniformsAndTextures(context, draw, uniformDataGatherer, textureDataGatherer,
                                lookupDesc.mayRequireLocalCoords());
                        var geometryUniforms = uniformDataCache.insert(uniformDataGatherer.finish());

                        // geometry texture samplers and then fragment texture samplers
                        // we build shader code and set binding points in this order as well
                        var textures = textureDataGatherer.finish();

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
                    depthStencilFlags |= draw.mRenderer.depthStencilFlags();
                }

                if (!geometryUniformTracker.writeUniforms(bufferManager) ||
                        !fragmentUniformTracker.writeUniforms(bufferManager)) {
                    return null;
                }
            }

            assert keyIndex == keys.length;
            // TimSort - stable
            Arrays.sort(keys);

            Rect2ic lastScissor = new Rect2i(0, 0, deviceInfo.width(), deviceInfo.height());
            int lastPipelineIndex = INVALID_INDEX;
            float[] tmpSolidColor = new float[4];

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
                                GEOMETRY_UNIFORM_BLOCK_BINDING,
                                commandList
                        );
                    }
                    if (fragmentBindingChange) {
                        fragmentUniformTracker.bindUniforms(
                                FRAGMENT_UNIFORM_BLOCK_BINDING,
                                commandList
                        );
                    }
                    if (textureBindingChange) {
                        textureTracker.bindTextures(commandList);
                    }
                }

                float[] solidColor = null;
                var pipelineDesc = indexToPipeline.get(pipelineIndex);
                if (pipelineDesc.usesFastSolidColor()) {
                    assert draw.mPaintParams != null;
                    boolean res = draw.mPaintParams.getSolidColor(deviceInfo, tmpSolidColor);
                    assert res;
                    solidColor = tmpSolidColor;
                }

                step.writeMesh(drawWriter, draw, solidColor,
                        pipelineDesc.mayRequireLocalCoords());

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
                    depthStencilFlags,
                    indexToPipeline,
                    textureDataGatherer.detachSamplers(),
                    textureDataGatherer.detachTextures());
        }
    }

    public Rect2ic getBounds() {
        return mBounds;
    }

    public int getDepthStencilFlags() {
        return mDepthStencilFlags;
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

        if (!mSamplerDescs.isEmpty()) {
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
        }

        return true;
    }

    public boolean execute(CommandBuffer commandBuffer) {
        for (var pipeline : mPipelines) {
            commandBuffer.trackResource(RefCnt.create(pipeline));
        }
        if (mSamplers != null) {
            for (var sampler : mSamplers) {
                commandBuffer.trackResource(RefCnt.create(sampler));
            }
        }
        for (var texture : mTextures) {
            commandBuffer.trackCommandBufferResource(texture.refImage());
        }
        var cmdList = getCommandList();
        var p = cmdList.mPrimitives.elements();
        int i = 0;
        var oa = cmdList.mPointers.elements();
        int oi = 0;
        int lim = cmdList.mPrimitives.size();
        while (i < lim) {
            switch (p[i++]) {
                case DrawCommandList.CMD_BIND_GRAPHICS_PIPELINE -> {
                    int pipelineIndex = p[i];
                    if (!commandBuffer.bindGraphicsPipeline(mPipelines[pipelineIndex])) {
                        return false;
                    }
                    i += 1;
                }
                case DrawCommandList.CMD_DRAW -> {
                    int vertexCount = p[i];
                    int baseVertex = p[i + 1];
                    commandBuffer.draw(vertexCount, baseVertex);
                    i += 2;
                }
                case DrawCommandList.CMD_DRAW_INDEXED -> {
                    int indexCount = p[i];
                    int baseIndex = p[i + 1];
                    int baseVertex = p[i + 2];
                    commandBuffer.drawIndexed(indexCount, baseIndex, baseVertex);
                    i += 3;
                }
                case DrawCommandList.CMD_DRAW_INSTANCED -> {
                    int instanceCount = p[i];
                    int baseInstance = p[i + 1];
                    int vertexCount = p[i + 2];
                    int baseVertex = p[i + 3];
                    commandBuffer.drawInstanced(instanceCount, baseInstance, vertexCount, baseVertex);
                    i += 4;
                }
                case DrawCommandList.CMD_DRAW_INDEXED_INSTANCED -> {
                    int indexCount = p[i];
                    int baseIndex = p[i + 1];
                    int instanceCount = p[i + 2];
                    int baseInstance = p[i + 3];
                    int baseVertex = p[i + 4];
                    commandBuffer.drawIndexedInstanced(indexCount, baseIndex, instanceCount, baseInstance, baseVertex);
                    i += 5;
                }
                case DrawCommandList.CMD_BIND_INDEX_BUFFER -> {
                    int indexType = p[i];
                    long offset = p[i + 1];
                    commandBuffer.bindIndexBuffer(indexType, (Buffer) oa[oi++], offset);
                    i += 2;
                }
                case DrawCommandList.CMD_BIND_VERTEX_BUFFER -> {
                    int binding = p[i];
                    long offset = p[i + 1];
                    commandBuffer.bindVertexBuffer(binding, (Buffer) oa[oi++], offset);
                    i += 2;
                }
                case DrawCommandList.CMD_SET_SCISSOR -> {
                    int x = p[i];
                    int y = p[i + 1];
                    int width = p[i + 2];
                    int height = p[i + 3];
                    commandBuffer.setScissor(x, y, width, height);
                    i += 4;
                }
                case DrawCommandList.CMD_BIND_UNIFORM_BUFFER -> {
                    int binding = p[i];
                    long offset = p[i + 1];
                    long size = p[i + 2];
                    commandBuffer.bindUniformBuffer(binding, (Buffer) oa[oi++], offset, size);
                    i += 3;
                }
                case DrawCommandList.CMD_BIND_TEXTURES -> {
                    int numBindings = p[i++];
                    for (int binding = 0; binding < numBindings; binding++) {
                        @RawPtr
                        var texture = mTextures.get(p[i]);
                        @RawPtr
                        var sampler = mSamplers[p[i + 1]];
                        commandBuffer.bindTextureSampler(binding,
                                texture.getImage(),
                                sampler,
                                texture.getSwizzle());
                        i += 2;
                    }
                }
            }
        }
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
        public int compareTo(@NonNull SortKey o) {
            int res = Integer.compareUnsigned(mOrderKey, o.mOrderKey);
            if (res != 0) return res;
            res = Long.compareUnsigned(mPipelineKey, o.mPipelineKey);
            if (res != 0) return res;
            return Arrays.compare(mTextures, o.mTextures);
        }
    }
}
