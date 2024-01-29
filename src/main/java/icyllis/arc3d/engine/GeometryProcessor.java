/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.shading.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;

import static icyllis.arc3d.engine.Engine.*;
import static icyllis.arc3d.engine.shading.UniformHandler.SamplerHandle;

/**
 * The GeometryProcessor represents some kind of geometric primitive. This includes the shape
 * of the primitive and the inherent color of the primitive. The GeometryProcessor is
 * responsible for providing a color and coverage input into the rendering pipeline. Through
 * optimization, Engine may decide a different color, no color, and / or no coverage are required
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
@Immutable
public abstract class GeometryProcessor extends Processor {

    /**
     * Describes a vertex or instance attribute.
     */
    @Immutable
    public static class Attribute {

        public static final int IMPLICIT_OFFSET = -1;

        /**
         * It must be N-aligned for all types, where N is sizeof(float).
         */
        public static int alignOffset(int offset) {
            return MathUtil.align4(offset);
        }

        private final String mName;
        private final byte mSrcType;
        private final byte mDstType;
        private final int mOffset;

        /**
         * Makes an attribute whose offset will be implicitly determined by the types and ordering
         * of an array attributes.
         *
         * @param name    the attrib name, cannot be null or empty
         * @param srcType the data type in vertex buffer, see {@link VertexAttribType}
         * @param dstType the data type in vertex shader, see {@link SLDataType}
         */
        public Attribute(@Nonnull String name, byte srcType, byte dstType) {
            if (name.isEmpty() || name.startsWith("_")) {
                throw new IllegalArgumentException();
            }
            if (srcType < 0 || srcType > VertexAttribType.kLast) {
                throw new IllegalArgumentException();
            }
            if (SLDataType.locations(dstType) <= 0) {
                throw new IllegalArgumentException();
            }
            mName = name;
            mSrcType = srcType;
            mDstType = dstType;
            mOffset = IMPLICIT_OFFSET;
        }

        /**
         * Makes an attribute with an explicit offset.
         *
         * @param name    the attrib name, UpperCamelCase, cannot be null or empty
         * @param srcType the data type in vertex buffer, see {@link VertexAttribType}
         * @param dstType the data type in vertex shader, see {@link SLDataType}
         * @param offset  N-aligned offset
         */
        public Attribute(@Nonnull String name, byte srcType, byte dstType, int offset) {
            if (name.isEmpty() || name.startsWith("_")) {
                throw new IllegalArgumentException();
            }
            if (srcType < 0 || srcType > VertexAttribType.kLast) {
                throw new IllegalArgumentException();
            }
            if (SLDataType.locations(dstType) <= 0) {
                throw new IllegalArgumentException();
            }
            if (offset < 0 || offset >= 32768 || alignOffset(offset) != offset) {
                throw new IllegalArgumentException();
            }
            mName = name;
            mSrcType = srcType;
            mDstType = dstType;
            mOffset = offset;
        }

        public final String name() {
            return mName;
        }

        /**
         * @return the data type in vertex buffer, see {@link VertexAttribType}
         */
        public final byte srcType() {
            return mSrcType;
        }

        /**
         * @return the data type in vertex shader, see {@link SLDataType}
         */
        public final byte dstType() {
            return mDstType;
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
         * @return the size of the source data in bytes
         */
        public final int size() {
            return VertexAttribType.size(mSrcType);
        }

        /**
         * @return the number of locations
         */
        public final int locations() {
            return SLDataType.locations(mDstType);
        }

        /**
         * @return the total size for this attribute in bytes
         */
        public final int stride() {
            int size = size();
            int count = locations();
            assert (size > 0 && count > 0);
            return size * count;
        }

        @Nonnull
        public final ShaderVar asShaderVar() {
            return new ShaderVar(mName, mDstType, ShaderVar.kIn_TypeModifier);
        }
    }

    /**
     * A set of attributes that can iterated.
     */
    @Immutable
    public static class AttributeSet implements Iterable<Attribute> {

        private final Attribute[] mAttributes;
        private final int mStride;

        final int mAllMask;

        private AttributeSet(@Nonnull Attribute[] attributes, int stride) {
            int offset = 0;
            for (Attribute attr : attributes) {
                if (attr.offset() != Attribute.IMPLICIT_OFFSET) {
                    // offset must be not descending no matter what the mask is
                    if (attr.offset() < offset) {
                        throw new IllegalArgumentException();
                    }
                    offset = attr.offset();
                    assert Attribute.alignOffset(offset) == offset;
                }
            }
            mAttributes = attributes;
            mStride = stride;
            mAllMask = ~0 >>> (Integer.SIZE - mAttributes.length);
        }

        /**
         * Create an attribute set with an implicit stride. Each attribute can either
         * have an implicit offset or an explicit offset aligned to 4 bytes. No attribute
         * can cross stride boundaries.
         * <p>
         * Note: GPU does not reorder vertex attributes, so when a vertex attribute has an
         * explicit offset, the subsequent implicit offsets will start from there.
         */
        @Nonnull
        public static AttributeSet makeImplicit(@Nonnull Attribute... attrs) {
            if (attrs.length == 0 || attrs.length > Integer.SIZE) {
                throw new IllegalArgumentException();
            }
            return new AttributeSet(attrs, Attribute.IMPLICIT_OFFSET);
        }

        /**
         * Create an attribute set with an explicit stride. Each attribute can either
         * have an implicit offset or an explicit offset aligned to 4 bytes. No attribute
         * can cross stride boundaries.
         * <p>
         * Note: GPU does not reorder vertex attributes, so when a vertex attribute has an
         * explicit offset, the subsequent implicit offsets will start from there.
         */
        @Nonnull
        public static AttributeSet makeExplicit(int stride, @Nonnull Attribute... attrs) {
            if (attrs.length == 0 || attrs.length > Integer.SIZE) {
                throw new IllegalArgumentException();
            }
            if (stride <= 0 || stride > 32768) {
                throw new IllegalArgumentException();
            }
            if (Attribute.alignOffset(stride) != stride) {
                throw new IllegalArgumentException();
            }
            return new AttributeSet(attrs, stride);
        }

        final int stride(int mask) {
            if (mStride != Attribute.IMPLICIT_OFFSET) {
                return mStride;
            }
            final int rawCount = mAttributes.length;
            int stride = 0;
            for (int i = 0, bit = 1; i < rawCount; i++, bit <<= 1) {
                final Attribute attr = mAttributes[i];
                if ((mask & bit) != 0) {
                    if (attr.offset() != Attribute.IMPLICIT_OFFSET) {
                        stride = attr.offset();
                    }
                    stride += Attribute.alignOffset(attr.stride());
                }
            }
            return stride;
        }

        final int numLocations(int mask) {
            final int rawCount = mAttributes.length;
            int locations = 0;
            for (int i = 0, bit = 1; i < rawCount; i++, bit <<= 1) {
                final Attribute attr = mAttributes[i];
                if ((mask & bit) != 0) {
                    locations += attr.locations();
                }
            }
            return locations;
        }

        final void appendToKey(@Nonnull KeyBuilder b, int mask) {
            final int rawCount = mAttributes.length;
            // max attribs is no less than 16
            b.addBits(6, rawCount, "attribute count");
            int offset = 0;
            for (int i = 0, bit = 1; i < rawCount; i++, bit <<= 1) {
                final Attribute attr = mAttributes[i];
                if ((mask & bit) != 0) {
                    b.appendComment(attr.name());
                    b.addBits(8, attr.srcType() & 0xFF, "attrType");
                    b.addBits(8, attr.dstType() & 0xFF, "attrGpuType");
                    if (attr.offset() != Attribute.IMPLICIT_OFFSET) {
                        offset = attr.offset();
                    }
                    assert (offset >= 0 && offset < 32768);
                    b.addBits(16, offset, "attrOffset");
                    offset += Attribute.alignOffset(attr.stride());
                } else {
                    b.appendComment("unusedAttr");
                    b.addBits(8, 0xFF, "attrType");
                    b.addBits(8, 0xFF, "attrGpuType");
                    b.addBits(16, 0xFFFF, "attrOffset");
                }
            }
            final int stride;
            if (mStride == Attribute.IMPLICIT_OFFSET) {
                stride = offset;
            } else {
                stride = mStride;
                if (stride < offset) {
                    throw new IllegalStateException();
                }
            }
            // max stride is no less than 2048
            assert (stride > 0 && stride <= 32768);
            assert (Attribute.alignOffset(stride) == stride);
            b.addBits(16, stride, "stride");
        }

        @Nonnull
        @Override
        public Iterator<Attribute> iterator() {
            return new Iter(mAllMask);
        }

        private class Iter implements Iterator<Attribute> {

            private final int mMask;

            private int mIndex;
            private int mOffset;

            Iter(int mask) {
                mMask = mask;
            }

            @Override
            public boolean hasNext() {
                forward();
                return mIndex < mAttributes.length;
            }

            @Nonnull
            @Override
            public Attribute next() {
                forward();
                try {
                    final Attribute ret, curr = mAttributes[mIndex++];
                    if (curr.offset() == Attribute.IMPLICIT_OFFSET) {
                        ret = new Attribute(curr.name(), curr.srcType(), curr.dstType(), mOffset);
                    } else {
                        ret = curr;
                        mOffset = curr.offset();
                    }
                    mOffset += Attribute.alignOffset(curr.stride());
                    return ret;
                } catch (IndexOutOfBoundsException e) {
                    throw new NoSuchElementException(e);
                }
            }

            private void forward() {
                while (mIndex < mAttributes.length && (mMask & (1 << mIndex)) == 0) {
                    mIndex++; // skip unused
                }
            }
        }
    }

    /**
     * GPs that need to use either float or ubyte colors can just call this to get a correctly
     * configured Attribute struct
     */
    @Nonnull
    protected static Attribute makeColorAttribute(String name, boolean wideColor) {
        return new Attribute(
                name,
                wideColor
                        ? VertexAttribType.kFloat4
                        : VertexAttribType.kUByte4_norm,
                SLDataType.kFloat4
        );
    }

    private int mVertexAttributesMask;      // binding = 0, divisor = 0
    private int mInstanceAttributesMask;    // binding = 1, divisor = 1

    protected GeometryProcessor(int classID) {
        super(classID);
    }

    /**
     * Returns a primitive topology for render passes. If the return values of
     * different instances are different, they must be reflected in the key,
     * see {@link #appendToKey(KeyBuilder)}.
     *
     * @see PrimitiveType
     */
    public abstract byte primitiveType();

    /**
     * Currently, GP is limited to one texture sampler at most.
     */
    public int numTextureSamplers() {
        return 0;
    }

    /**
     * Used to capture the properties of the TextureProxies required/expected by a GeometryProcessor
     * along with an associated SamplerState. The actual proxies used are stored in either the
     * fixed or dynamic state arrays. TextureSamplers don't perform any coord manipulation to account
     * for texture origin.
     *
     * @see SamplerState
     */
    public int textureSamplerState(int i) {
        throw new IndexOutOfBoundsException(i);
    }

    /**
     * @see Swizzle
     */
    public short textureSamplerSwizzle(int i) {
        throw new IndexOutOfBoundsException(i);
    }

    /**
     * Returns true if {@link #numVertexAttributes()} will return non-zero.
     *
     * @return true if there are per-vertex attributes
     */
    public final boolean hasVertexAttributes() {
        assert (mVertexAttributesMask == 0 || allVertexAttributes() != null);
        return mVertexAttributesMask != 0;
    }

    /**
     * Returns the number of used per-vertex attributes (input variables).
     * Note: attribute of a matrix type counts as just one.
     *
     * @see #numVertexLocations()
     */
    public final int numVertexAttributes() {
        return Integer.bitCount(mVertexAttributesMask);
    }

    /**
     * Returns the number of used per-vertex attribute locations (slots).
     * An attribute (variable) may take up multiple consecutive locations.
     *
     * @see SLDataType#locations(byte)
     * @see #numVertexAttributes()
     */
    public final int numVertexLocations() {
        assert (mVertexAttributesMask == 0 || allVertexAttributes() != null);
        return mVertexAttributesMask == 0
                ? 0
                : allVertexAttributes().numLocations(mVertexAttributesMask);
    }

    /**
     * Returns an iterable of used per-vertex attributes. It's safe to call even if there's no attribute.
     * The iterator handles hides two pieces of complexity:
     * <ol>
     * <li>It skips unused attributes (see mask in {@link #setVertexAttributes(int)}).</li>
     * <li>It always returns an attribute with a known offset.</li>
     * </ol>
     */
    @Nonnull
    public final Iterable<Attribute> vertexAttributes() {
        assert (mVertexAttributesMask == 0 || allVertexAttributes() != null);
        if (mVertexAttributesMask == 0) {
            return Collections.emptySet();
        }
        AttributeSet attrs = allVertexAttributes();
        if (mVertexAttributesMask == attrs.mAllMask) {
            return attrs;
        }
        return new Iterable<>() {
            @Nonnull
            @Override
            public Iterator<Attribute> iterator() {
                return allVertexAttributes().new Iter(mVertexAttributesMask);
            }
        };
    }

    /**
     * Returns the number of bytes from one vertex to the next vertex, including paddings.
     * A common practice is to populate the vertex's memory using an implicit array of
     * structs. In this case, it is best to assert that: stride == sizeof(struct).
     */
    public final int vertexStride() {
        assert (mVertexAttributesMask == 0 || allVertexAttributes() != null);
        return mVertexAttributesMask == 0
                ? 0
                : allVertexAttributes().stride(mVertexAttributesMask);
    }

    /**
     * Returns true if {@link #numInstanceAttributes()} will return non-zero.
     *
     * @return true if there are per-instance attributes
     */
    public final boolean hasInstanceAttributes() {
        assert (mInstanceAttributesMask == 0 || allInstanceAttributes() != null);
        return mInstanceAttributesMask != 0;
    }

    /**
     * Returns the number of used per-instance attributes (input variables).
     * Note: attribute of a matrix type counts as just one.
     *
     * @see #numInstanceLocations()
     */
    public final int numInstanceAttributes() {
        return Integer.bitCount(mInstanceAttributesMask);
    }

    /**
     * Returns the number of used per-instance attribute locations. (slots).
     * An attribute (variable) may take up multiple consecutive locations.
     *
     * @see SLDataType#locations(byte)
     * @see #numInstanceAttributes()
     */
    public final int numInstanceLocations() {
        assert (mInstanceAttributesMask == 0 || allInstanceAttributes() != null);
        return mInstanceAttributesMask == 0
                ? 0
                : allInstanceAttributes().numLocations(mInstanceAttributesMask);
    }

    /**
     * Returns an iterable of used per-instance attributes. It's safe to call even if there's no attribute.
     * The iterator handles hides two pieces of complexity:
     * <ol>
     * <li>It skips unused attributes (see mask in {@link #setInstanceAttributes(int)}).</li>
     * <li>It always returns an attribute with a known offset.</li>
     * </ol>
     */
    @Nonnull
    public final Iterable<Attribute> instanceAttributes() {
        assert (mInstanceAttributesMask == 0 || allInstanceAttributes() != null);
        if (mInstanceAttributesMask == 0) {
            return Collections.emptySet();
        }
        AttributeSet attrs = allInstanceAttributes();
        if (mInstanceAttributesMask == attrs.mAllMask) {
            return attrs;
        }
        return new Iterable<>() {
            @Nonnull
            @Override
            public Iterator<Attribute> iterator() {
                return allInstanceAttributes().new Iter(mInstanceAttributesMask);
            }
        };
    }

    /**
     * Returns the number of bytes from one instance to the next instance, including paddings.
     * A common practice is to populate the instance's memory using an implicit array of
     * structs. In this case, it is best to assert that: stride == sizeof(struct).
     */
    public final int instanceStride() {
        assert (mInstanceAttributesMask == 0 || allInstanceAttributes() != null);
        return mInstanceAttributesMask == 0
                ? 0
                : allInstanceAttributes().stride(mInstanceAttributesMask);
    }

    /**
     * Appends a key on the KeyBuilder that reflects any variety in the code that the
     * geometry processor subclass can emit.
     *
     * @see #makeProgramImpl(ShaderCaps)
     */
    public abstract void appendToKey(@Nonnull KeyBuilder b);

    public final void appendAttributesToKey(@Nonnull KeyBuilder b) {
        AttributeSet vertexAttributes = allVertexAttributes();
        if (vertexAttributes != null) {
            b.appendComment("vertex attributes");
            vertexAttributes.appendToKey(b, mVertexAttributesMask);
        }
        AttributeSet instanceAttributes = allInstanceAttributes();
        if (instanceAttributes != null) {
            b.appendComment("instance attributes");
            instanceAttributes.appendToKey(b, mInstanceAttributesMask);
        }
    }

    /**
     * Returns a new instance of the appropriate implementation class for the given
     * GeometryProcessor. This method is called only when the specified key does not
     * exist in the program cache.
     *
     * @see #appendToKey(KeyBuilder)
     */
    @Nonnull
    public abstract ProgramImpl makeProgramImpl(ShaderCaps caps);

    /**
     * Returns a shared {@link AttributeSet} containing all per-vertex attributes for
     * the GeometryProcess class. Returns null if there is no per-vertex attributes.
     * <p>
     * For the same GeometryProcess instance, the implementation must ensure that the
     * return value always remains the same. If the return value is not null, then
     * {@link #setVertexAttributes(int)} must be called within the constructor.
     * In addition, for the same GeometryProcess class, the implementation should ensure
     * that the nullability of the return value always remains the same.
     *
     * @see #setVertexAttributes(int)
     */
    @Nullable
    protected abstract AttributeSet allVertexAttributes();

    /**
     * Sets per-vertex attributes mask, which is used to control which of them are used
     * by this GeometryProcessor instance. Note: Call this in subclasses constructor.
     *
     * @param mask a bit mask determining which attributes to use, can be zero
     */
    protected final void setVertexAttributes(int mask) {
        AttributeSet attrs = allVertexAttributes();
        assert attrs != null;
        mVertexAttributesMask |= mask & attrs.mAllMask; // sanitize
    }

    /**
     * Returns a shared {@link AttributeSet} containing all per-instance attributes for
     * the GeometryProcess class. Returns null if there is no per-instance attributes.
     * <p>
     * For the same GeometryProcess instance, the implementation must ensure that the
     * return value always remains the same. If the return value is not null, then
     * {@link #setInstanceAttributes(int)} must be called within the constructor.
     * In addition, for the same GeometryProcess class, the implementation should ensure
     * that the nullability of the return value always remains the same.
     *
     * @see #setInstanceAttributes(int)
     */
    @Nullable
    protected abstract AttributeSet allInstanceAttributes();

    /**
     * Sets per-instance attributes mask, which is used to control which of them are used
     * by this GeometryProcessor instance. Note: Call this in subclasses constructor.
     *
     * @param mask a bit mask determining which attributes to use, can be zero
     */
    protected final void setInstanceAttributes(int mask) {
        AttributeSet attrs = allInstanceAttributes();
        assert attrs != null;
        mInstanceAttributesMask |= mask & attrs.mAllMask; // sanitize
    }

    /**
     * Every {@link GeometryProcessor} must be capable of creating a subclass of ProgramImpl. The
     * ProgramImpl emits the shader code that implements the GeometryProcessor, is attached to the
     * generated backend API pipeline/program and used to extract uniform data from
     * GeometryProcessor instances.
     */
    public static abstract class ProgramImpl {

        /**
         * A helper for setting the matrix on a uniform handle initialized through
         * writeOutputPosition or writeLocalCoord. Automatically handles elided uniforms,
         * scale+translate matrices, and state tracking (if provided state pointer is non-null).
         *
         * @param matrix the matrix to set, must be immutable
         * @param state  the current state
         * @return new state, eiter matrix or state
         */
        //TODO move to other places
        protected static Matrix setTransform(@Nonnull UniformDataManager pdm,
                                             @UniformHandler.UniformHandle int uniform,
                                             @Nonnull Matrix matrix,
                                             @Nullable Matrix state) {
            if (uniform == Engine.INVALID_RESOURCE_HANDLE ||
                    (state != null && state.equals(matrix))) {
                // No update needed
                return state;
            }
            if (matrix.isScaleTranslate()) {
                // ComputeMatrixKey and writeX() assume the uniform is a float4 (can't assert since nothing
                // is exposed on a handle, but should be caught lower down).
                pdm.set4f(uniform, matrix.getScaleX(), matrix.getTranslateX(),
                        matrix.getScaleY(), matrix.getTranslateY());
            } else {
                pdm.setMatrix3f(uniform, matrix);
            }
            return matrix;
        }

        protected static void writePassthroughWorldPosition(
                VertexGeomBuilder vertBuilder,
                ShaderVar inPos,
                ShaderVar outPos) {
            assert (inPos.getType() == SLDataType.kFloat2 || inPos.getType() == SLDataType.kFloat3);
            vertBuilder.codeAppendf("vec%d _worldPos = %s;\n",
                    SLDataType.vectorDim(inPos.getType()),
                    inPos.getName());
            outPos.set("_worldPos", inPos.getType());
        }

        /**
         * Helpers for adding code to write the transformed vertex position. The first simple version
         * just writes a variable named by 'posName' into the position output variable with the
         * assumption that the position is 2D. The second version transforms the input position by a
         * view matrix and the output variable is 2D or 3D depending on whether the view matrix is
         * perspective.
         *
         * @param inPos the local variable or the attribute, type must be either vec2 or vec3
         */
        protected static void writeWorldPosition(VertexGeomBuilder vertBuilder,
                                                 ShaderVar inPos,
                                                 String matrixName,
                                                 ShaderVar outPos) {
            assert (inPos.getType() == SLDataType.kFloat2 || inPos.getType() == SLDataType.kFloat3);

            if (inPos.getType() == SLDataType.kFloat3) {
                // A float3 stays a float3 whether the matrix adds perspective
                vertBuilder.codeAppendf("vec3 _worldPos = %s * %s;\n",
                        matrixName,
                        inPos.getName());
                outPos.set("_worldPos", SLDataType.kFloat3);
            } else {
                // A float2 is promoted to a float3 if we add perspective via the matrix
                vertBuilder.codeAppendf("vec3 _worldPos = %s * vec3(%s, 1.0);\n",
                        matrixName,
                        inPos.getName());
                outPos.set("_worldPos", SLDataType.kFloat3);
            }
        }

        /**
         * Emits the code from this geometry processor into the shaders. For any FP in the pipeline that
         * has its input coords implemented by the GP as a varying, the varying will be accessible in
         * the returned map and should be used when the FP code is emitted. The FS variable containing
         * the GP's output local coords is also returned.
         */
        public final void emitCode(VertexGeomBuilder vertBuilder,
                                   FPFragmentBuilder fragBuilder,
                                   VaryingHandler varyingHandler,
                                   UniformHandler uniformHandler,
                                   ShaderCaps shaderCaps,
                                   GeometryProcessor geomProc,
                                   String outputColor,
                                   String outputCoverage,
                                   @SamplerHandle int[] texSamplers) {
            final var localPos = new ShaderVar();
            final var worldPos = new ShaderVar();
            onEmitCode(vertBuilder,
                    fragBuilder,
                    varyingHandler,
                    uniformHandler,
                    shaderCaps,
                    geomProc,
                    outputColor,
                    outputCoverage,
                    texSamplers,
                    localPos,
                    worldPos);

            // Emit the vertex position to the hardware in the normalized device coordinates it expects.
            assert (worldPos.getType() == SLDataType.kFloat2 ||
                    worldPos.getType() == SLDataType.kFloat3);
            vertBuilder.emitNormalizedPosition(worldPos);
            if (worldPos.getType() == SLDataType.kFloat2) {
                varyingHandler.setNoPerspective();
            }
        }

        /**
         * A ProgramImpl instance can be reused with any GeometryProcessor that produces the same key.
         * This function reads data from a GeometryProcessor and updates any uniform variables
         * required by the shaders created in emitCode(). The GeometryProcessor parameter is
         * guaranteed to be of the same type and to have an identical processor key as the
         * GeometryProcessor that created this ProgramImpl.
         */
        public abstract void setData(UniformDataManager manager,
                                     GeometryProcessor geomProc);

        /**
         * The local pos is used to specify the output variable storing draw's local position. It can
         * be either a vec2 or a vec3, or void. It can only be void when no FP needs local coordinates.
         * This variable can be an attribute or local variable, but should not itself be a varying.
         * PipelineBuilder automatically determines if this must be passed to the FP.
         * <p>
         * The world pos is used to specify the output variable storing its world (device) position.
         * It can either be a vec2 or a vec3 (in order to handle perspective).
         */
        protected abstract void onEmitCode(VertexGeomBuilder vertBuilder,
                                           FPFragmentBuilder fragBuilder,
                                           VaryingHandler varyingHandler,
                                           UniformHandler uniformHandler,
                                           ShaderCaps shaderCaps,
                                           GeometryProcessor geomProc,
                                           String outputColor,
                                           String outputCoverage,
                                           @SamplerHandle int[] texSamplers,
                                           ShaderVar localPos,
                                           ShaderVar worldPos);
    }
}
