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

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.ToIntFunction;

/**
 * A draw pass represents a render pass, with limited and sorted draw commands.
 * <p>
 * Created immutable.
 */
public class DrawPass {

    private final ArrayList<GraphicsPipelineDesc> mPipelineDescs;

    @SharedPtr
    private ArrayList<GraphicsPipeline> mGraphicsPipelines = new ArrayList<>();

    private final DrawCommandList mCommandList;

    private ImageProxy[] mSampledImages;

    private DrawPass(DrawCommandList commandList,
                     ArrayList<GraphicsPipelineDesc> pipelineDescs) {
        mCommandList = commandList;
        mPipelineDescs = pipelineDescs;
    }

    public static DrawPass make(RecordingContext rContext,
                                DrawOpList drawOpList,
                                ImageProxy colorTarget,
                                byte loadStoreOps,
                                float[] clearColor) {

        var dynamicBufferManager = rContext.getDynamicBufferManager();


        var commandList = new DrawCommandList();


        var pipelineToIndexMap
                = new Object2IntOpenHashMap<GraphicsPipelineDesc>();
        var pipelineDescs = new ArrayList<GraphicsPipelineDesc>();
        ToIntFunction<GraphicsPipelineDesc> insertPipelineDesc = desc -> {
            pipelineDescs.add(desc);
            return pipelineDescs.size() - 1;
        };

        SortKey[] keys = new SortKey[drawOpList.numSteps()];
        int sortKeyIndex = 0;

        for (var op : drawOpList.mDrawOps) {


            for (int stepIndex = 0; stepIndex < op.mRenderer.numSteps(); stepIndex++) {
                var step = op.mRenderer.step(stepIndex);

                int pipelineIndex = pipelineToIndexMap.computeIfAbsent(new GraphicsPipelineDesc(step),
                        insertPipelineDesc);

                keys[sortKeyIndex++] = new SortKey(
                        op,
                        stepIndex,
                        pipelineIndex
                );
            }
        }


        assert sortKeyIndex == keys.length;
        // TimSort - stable
        Arrays.sort(keys);

        MeshDrawWriter drawWriter = new MeshDrawWriter(dynamicBufferManager,
                commandList);
        Rect2ic lastScissor = new Rect2i(0, 0, colorTarget.getWidth(), colorTarget.getHeight());
        int lastPipeline = -1;

        for (var key : keys) {
            var op = key.drawOpRef;
            var step = key.step();

            boolean pipelineChange = key.pipelineIndex() != lastPipeline;

            Rect2ic newScissor = !op.mScissorRect.equals(lastScissor)
                    ? op.mScissorRect : null;

            boolean dynamicStateChange = newScissor != null;

            if (pipelineChange) {
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
            if (pipelineChange) {
                commandList.bindGraphicsPipeline(key.pipelineIndex());
                lastPipeline = key.pipelineIndex();
            }
            if (dynamicStateChange) {
                if (newScissor != null) {
                    commandList.setScissor(newScissor);
                    lastScissor = newScissor;
                }
            }

            step.writeVertices(drawWriter, op, new float[]{1, 1, 1, 1});

            if (dynamicBufferManager.hasMappingFailed()) {
                return null;
            }
        }
        drawWriter.flush();


        DrawPass pass = new DrawPass(commandList, pipelineDescs);

        return pass;
    }

    @RawPtr
    public GraphicsPipeline getPipeline(int index) {
        return mGraphicsPipelines.get(index);
    }

    public DrawCommandList getCommandList() {
        return mCommandList;
    }

    public ImageProxy[] getSampledImages() {
        return mSampledImages;
    }

    public static final class SortKey implements Comparable<SortKey> {

        public static final long PAINTERS_ORDER_OFFSET = 48;
        public static final long PAINTERS_ORDER_MASK = (1 << 16) - 1;

        public static final long STENCIL_INDEX_OFFSET = 32;
        public static final long STENCIL_INDEX_MASK = (1 << 16) - 1;

        static {
            //noinspection ConstantValue
            assert DrawOrder.PAINTERS_ORDER_SHIFT == PAINTERS_ORDER_OFFSET &&
                    DrawOrder.STENCIL_INDEX_SHIFT == STENCIL_INDEX_OFFSET;
        }

        public static final int STEP_INDEX_OFFSET = 30;
        public static final int STEP_INDEX_MASK = (1 << 2) - 1;

        public static final int PIPELINE_INDEX_OFFSET = 0;
        public static final int PIPELINE_INDEX_MASK = (1 << 30) - 1;

        private long highOrderFlags;
        private long lowOrderFlags;
        private DrawOp drawOpRef;

        public SortKey(DrawOp drawOp,
                       int stepIndex,
                       int pipelineIndex) {
            // the higher 32 bits are just we want
            highOrderFlags = drawOp.mDrawOrder & 0xFFFFFFFF_00000000L;
            assert (stepIndex & STEP_INDEX_MASK) == stepIndex;
            highOrderFlags |= ((long) stepIndex << STEP_INDEX_OFFSET) | ((long) pipelineIndex << PIPELINE_INDEX_OFFSET);
            drawOpRef = drawOp;
        }

        public GeometryStep step() {
            return drawOpRef.mRenderer.step(
                    (int) ((highOrderFlags >>> STEP_INDEX_OFFSET) & STEP_INDEX_MASK));
        }

        public int pipelineIndex() {
            return (int) ((highOrderFlags >>> PIPELINE_INDEX_OFFSET) & PIPELINE_INDEX_MASK);
        }

        @Override
        public int compareTo(@Nonnull SortKey o) {
            int res = Long.compareUnsigned(highOrderFlags, o.highOrderFlags);
            return res != 0 ? res : Long.compareUnsigned(lowOrderFlags, o.lowOrderFlags);
        }
    }
}
