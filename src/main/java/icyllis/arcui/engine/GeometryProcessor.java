/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine;

import icyllis.arcui.core.MathUtil;
import icyllis.arcui.core.SLType;

import javax.annotation.concurrent.Immutable;

/**
 * The GeometryProcessor represents some kind of geometric primitive.  This includes the shape
 * of the primitive and the inherent color of the primitive.  The GeometryProcessor is
 * responsible for providing a color and coverage input into the Arc UI rendering pipeline. Through
 * optimization, Arc UI may decide a different color, no color, and / or no coverage are required
 * from the GeometryProcessor, so the GeometryProcessor must be able to support this
 * functionality.
 * <p>
 * There are two feedback loops between the FragmentProcessors, the XferProcessor, and the
 * GeometryProcessor. These loops run on the CPU and to determine known properties of the final
 * color and coverage inputs to the XferProcessor in order to perform optimizations that preserve
 * correctness. The DrawOp seeds these loops with initial color and coverage, in its
 * getProcessorAnalysisInputs implementation. These seed values are processed by the
 * subsequent stages of the rendering pipeline and the output is then fed back into the DrawOp
 * in the applyPipelineOptimizations call, where the op can use the information to inform
 * decisions about GeometryProcessor creation.
 * <p>
 * Note that all derived classes should hide their constructors and provide a Make factory
 * function that takes an arena (except for Tesselation-specific classes). This is because
 * geometry processors can be created in either the record-time or flush-time arenas which
 * define their lifetimes (i.e., a DDLs life time in the first case and a single flush in
 * the second case).
 */
public abstract class GeometryProcessor extends Processor {

    /**
     * Describes a vertex or instance attribute.
     */
    @Immutable
    public static class Attribute {

        public static final int IMPLICIT_OFFSET = -1;

        public static int alignOffset(int offset) {
            return MathUtil.align4(offset);
        }

        private final String mName;
        /**
         * @see icyllis.arcui.engine.Types.VertexAttribType
         */
        private final byte mCPUType;
        /**
         * @see SLType
         */
        private final byte mGPUType;
        private final int mOffset;

        /**
         * Makes an attribute whose offset will be implicitly determined by the types and ordering
         * of an array attributes.
         *
         * @param cpuType see {@link Types.VertexAttribType}
         * @param gpuType see {@link SLType}
         */
        public Attribute(String name, byte cpuType, byte gpuType) {
            assert name != null && gpuType != SLType.VOID;
            mName = name;
            mCPUType = cpuType;
            mGPUType = gpuType;
            mOffset = IMPLICIT_OFFSET;
        }

        /**
         * Makes an attribute with an explicit offset.
         *
         * @param cpuType see {@link Types.VertexAttribType}
         * @param gpuType see {@link SLType}
         */
        public Attribute(String name, byte cpuType, byte gpuType, int offset) {
            assert name != null && gpuType != SLType.VOID;
            assert offset != IMPLICIT_OFFSET && alignOffset(offset) == offset;
            mName = name;
            mCPUType = cpuType;
            mGPUType = gpuType;
            mOffset = offset;
        }

        public String getName() {
            return mName;
        }

        public byte getCPUType() {
            return mCPUType;
        }

        public byte getGPUType() {
            return mGPUType;
        }

        /**
         * Returns the offset if attributes were specified with explicit offsets. Otherwise,
         * offsets (and total vertex stride) are implicitly determined from attribute order and
         * types. See {@link #IMPLICIT_OFFSET}.
         */
        public int getOffset() {
            return mOffset;
        }

        public int getSize() {
            return Types.VertexAttribType.getSize(mCPUType);
        }
    }
}
