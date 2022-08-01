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
import icyllis.arcui.engine.shading.*;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
        private final byte mCPUType;
        private final byte mGPUType;
        private final int mOffset;

        /**
         * Makes an attribute whose offset will be implicitly determined by the types and ordering
         * of an array attributes.
         *
         * @param name    the attrib raw name, UpperCamelCase, cannot be null or empty
         * @param cpuType see {@link VertexAttribType}
         * @param gpuType see {@link SLType}
         */
        public Attribute(String name, byte cpuType, byte gpuType) {
            assert (name != null && gpuType != SLType.VOID);
            mName = name;
            mCPUType = cpuType;
            mGPUType = gpuType;
            mOffset = IMPLICIT_OFFSET;
        }

        /**
         * Makes an attribute with an explicit offset.
         *
         * @param name    the attrib raw name, UpperCamelCase, cannot be null or empty
         * @param cpuType see {@link VertexAttribType}
         * @param gpuType see {@link SLType}
         * @param offset  N-aligned offset
         */
        public Attribute(String name, byte cpuType, byte gpuType, int offset) {
            assert (name != null && gpuType != SLType.VOID);
            assert (offset != IMPLICIT_OFFSET && alignOffset(offset) == offset);
            mName = name;
            mCPUType = cpuType;
            mGPUType = gpuType;
            mOffset = offset;
        }

        public final String name() {
            return mName;
        }

        /**
         * @see VertexAttribType
         */
        public final byte cpuType() {
            return mCPUType;
        }

        /**
         * @see SLType
         */
        public final byte gpuType() {
            return mGPUType;
        }

        /**
         * Returns the offset if attributes were specified with explicit offsets. Otherwise,
         * offsets (and total vertex stride) are implicitly determined from attribute order and
         * types. See {@link #IMPLICIT_OFFSET}.
         */
        public final int offset() {
            return mOffset;
        }

        /**
         * @return CPU size in bytes
         */
        public final int size() {
            return VertexAttribType.getSize(mCPUType);
        }

        @Nonnull
        public final ShaderVar asShaderVar() {
            return new ShaderVar(mName, mGPUType, ShaderVar.TYPE_MODIFIER_IN);
        }
    }

    /**
     * A set of attributes that can iterated. The iterator handles hides two pieces of complexity:
     * 1) It skips uninitialized attributes.
     * 2) It always returns an attribute with a known offset.
     */
    public static class AttributeSet implements Iterable<Attribute> {

        private Attribute[] mAttributes;
        private int mStride;

        public final int count() {
            return mAttributes.length;
        }

        public final int stride() {
            return mStride;
        }

        /**
         * Init with implicit offsets and stride. No attributes can have a predetermined stride.
         */
        public final void initImplicit(@Nonnull Attribute[] attrs) {
            mAttributes = attrs;
            mStride = 0;
            for (Attribute attr : attrs) {
                assert (attr != null);
                assert (attr.offset() == Attribute.IMPLICIT_OFFSET);
                mStride += Attribute.alignOffset(attr.size());
            }
            assert (attrs.length <= 0xFFFF);
            assert (mStride <= 0xFFFF);
        }

        /**
         * Init with explicit offsets and stride. All attributes must be initialized and have
         * an explicit offset aligned to 4 bytes and with no attribute crossing stride boundaries.
         */
        public final void initExplicit(@Nonnull Attribute[] attrs, int stride) {
            mAttributes = attrs;
            mStride = stride;
            assert (Attribute.alignOffset(mStride) == mStride);
            assert (assertExplicit(attrs));
            assert (attrs.length <= 0xFFFF);
            assert (mStride <= 0xFFFF);
        }

        private boolean assertExplicit(@Nonnull Attribute[] attrs) {
            for (Attribute attr : attrs) {
                assert (attr != null);
                assert (attr.offset() != Attribute.IMPLICIT_OFFSET);
                assert (Attribute.alignOffset(attr.offset()) == attr.offset());
                assert (attr.offset() + attr.size() <= mStride);
            }
            return true;
        }

        public final void addToKey(@Nonnull KeyBuilder b) {
            b.addBits(16, stride() & 0xFFFF, "stride");
            b.addBits(16, count() & 0xFFFF, "attribute count");
            int implicitOffset = 0;
            for (Attribute attr : mAttributes) {
                b.appendComment(attr.name());
                b.addBits(8, attr.cpuType() & 0xFF, "attrType");
                b.addBits(8, attr.gpuType() & 0xFF, "attrGpuType");
                int offset;
                if (attr.offset() != Attribute.IMPLICIT_OFFSET) {
                    offset = attr.offset();
                } else {
                    offset = implicitOffset;
                    implicitOffset += Attribute.alignOffset(attr.size());
                }
                b.addBits(16, offset & 0xFFFF, "attrOffset");
            }
        }

        @Nonnull
        @Override
        public final Iterator<Attribute> iterator() {
            return new Iter();
        }

        private class Iter implements Iterator<Attribute> {

            private int mIndex;
            private int mImplicitOffset;

            @Override
            public boolean hasNext() {
                return mIndex < count();
            }

            @Nonnull
            @Override
            public Attribute next() {
                try {
                    final Attribute ret, curr = mAttributes[mIndex++];
                    if (curr.offset() == Attribute.IMPLICIT_OFFSET) {
                        ret = new Attribute(curr.name(), curr.cpuType(), curr.gpuType(), mImplicitOffset);
                    } else {
                        ret = curr;
                    }
                    mImplicitOffset += Attribute.alignOffset(curr.size());
                    return ret;
                } catch (Exception e) {
                    throw new NoSuchElementException(e);
                }
            }
        }
    }

    // GPs that need to use either float or ubyte colors can just call this to get a correctly
    // configured Attribute struct
    @Nonnull
    protected static Attribute makeColorAttribute(String name, boolean wideColor) {
        return new Attribute(name,
                wideColor ? VertexAttribType.FLOAT4 : VertexAttribType.UBYTE4_NORM,
                SLType.HALF4);
    }

    private final AttributeSet mVertexAttributes = new AttributeSet();      // binding = 0
    private final AttributeSet mInstanceAttributes = new AttributeSet();    // binding = 1

    private int mTextureSamplerCnt;

    protected GeometryProcessor(int classID) {
        super(classID);
    }

    public final int numTextureSamplers() {
        return mTextureSamplerCnt;
    }

    public final int numVertexAttributes() {
        return mVertexAttributes.count();
    }

    public final AttributeSet vertexAttributes() {
        return mVertexAttributes;
    }

    public final int numInstanceAttributes() {
        return mInstanceAttributes.count();
    }

    public final AttributeSet instanceAttributes() {
        return mInstanceAttributes;
    }

    public final boolean hasVertexAttributes() {
        return mVertexAttributes.count() > 0;
    }

    public final boolean hasInstanceAttributes() {
        return mInstanceAttributes.count() > 0;
    }

    /**
     * A common practice is to populate the vertex/instance's memory using an implicit array of
     * structs. In this case, it is best to assert that: stride == sizeof(struct)
     */
    public final int vertexStride() {
        return mVertexAttributes.stride();
    }

    public final int instanceStride() {
        return mInstanceAttributes.stride();
    }

    protected final void setVertexAttributes(Attribute[] attrs, int stride) {
        mVertexAttributes.initExplicit(attrs, stride);
    }

    protected final void setInstanceAttributes(Attribute[] attrs, int stride) {
        mInstanceAttributes.initExplicit(attrs, stride);
    }

    protected final void setVertexAttributesWithImplicitOffsets(Attribute[] attrs) {
        mVertexAttributes.initImplicit(attrs);
    }

    protected final void setInstanceAttributesWithImplicitOffsets(Attribute[] attrs) {
        mInstanceAttributes.initImplicit(attrs);
    }

    protected final void setTextureSamplerCnt(int cnt) {
        assert (cnt >= 0);
        mTextureSamplerCnt = cnt;
    }

    public static abstract class ProgramImpl {

        public record EmitArgs(VertexGeoBuilder vertBuilder,
                               FPFragmentBuilder fragBuilder,
                               VaryingHandler varyingHandler,
                               UniformHandler uniformHandler,
                               ShaderCaps shaderCaps,
                               GeometryProcessor geomProc,
                               String outputColor,
                               String outputCoverage,
                               int[] texSamplers) {
        }
    }
}
