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

import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.SharedPtr;
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

    private ArrayList<GraphicsPipelineDesc> mPipelineDescriptors;

    @SharedPtr
    private ArrayList<GraphicsPipeline> mGraphicsPipelines = new ArrayList<>();

    private final DrawCommandList mCommandList = new DrawCommandList();

    private ImageProxy[] mSampledImages;

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

        int lastPipeline = -1;

        for (var key : keys) {
            var op = key.drawOpRef;
            var step = key.step();

            boolean pipelineChange = key.pipelineIndex() != lastPipeline;


            if (pipelineChange) {
                drawWriter.newPipelineState(
                        step.vertexStride(),
                        step.instanceStride()
                );
            } else {
                //??
            }


            step.writeVertices(drawWriter, op, null);
        }


        DrawPass pass = new DrawPass();
        pass.mPipelineDescriptors = pipelineDescs;

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

        public static final long STEP_INDEX_OFFSET = 30;
        public static final long STEP_INDEX_MASK = (1 << 2) - 1;

        public static final long PIPELINE_INDEX_OFFSET = 0;
        public static final long PIPELINE_INDEX_MASK = (1 << 30) - 1;

        private long highOrderFlags;
        private long lowOrderFlags;
        private DrawOp drawOpRef;

        public SortKey(DrawOp drawOp,
                       int stepIndex,
                       int pipelineIndex) {

            highOrderFlags = ((long) stepIndex << STEP_INDEX_OFFSET) | ((long) pipelineIndex << PIPELINE_INDEX_OFFSET);
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
