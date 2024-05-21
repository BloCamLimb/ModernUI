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
import icyllis.arc3d.engine.graphene.DrawOp;
import icyllis.arc3d.engine.graphene.MeshDrawWriter;
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
public abstract class GeometryStep extends Processor {

    /**
     * GPs that need to use either float or ubyte colors can just call this to get a correctly
     * configured Attribute struct
     */
    @Nonnull
    protected static VertexInputLayout.Attribute makeColorAttribute(String name, boolean wideColor) {
        return new VertexInputLayout.Attribute(
                name,
                wideColor
                        ? VertexAttribType.kFloat4
                        : VertexAttribType.kUByte4_norm,
                SLDataType.kFloat4
        );
    }

    private int mVertexAttributesMask;      // binding = 0, divisor = 0
    private int mInstanceAttributesMask;    // binding = 1, divisor = 1

    protected GeometryStep(int classID) {
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
    public final Iterable<VertexInputLayout.Attribute> vertexAttributes() {
        assert (mVertexAttributesMask == 0 || allVertexAttributes() != null);
        if (mVertexAttributesMask == 0) {
            return Collections.emptySet();
        }
        VertexInputLayout.AttributeSet attrs = allVertexAttributes();
        if (mVertexAttributesMask == attrs.mAllMask) {
            return attrs;
        }
        return new Iterable<>() {
            @Nonnull
            @Override
            public Iterator<VertexInputLayout.Attribute> iterator() {
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
    public final Iterable<VertexInputLayout.Attribute> instanceAttributes() {
        assert (mInstanceAttributesMask == 0 || allInstanceAttributes() != null);
        if (mInstanceAttributesMask == 0) {
            return Collections.emptySet();
        }
        VertexInputLayout.AttributeSet attrs = allInstanceAttributes();
        if (mInstanceAttributesMask == attrs.mAllMask) {
            return attrs;
        }
        return new Iterable<>() {
            @Nonnull
            @Override
            public Iterator<VertexInputLayout.Attribute> iterator() {
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
        VertexInputLayout.AttributeSet vertexAttributes = allVertexAttributes();
        if (vertexAttributes != null) {
            vertexAttributes.appendToKey(b, mVertexAttributesMask);
        }
        VertexInputLayout.AttributeSet instanceAttributes = allInstanceAttributes();
        if (instanceAttributes != null) {
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
     * Returns a shared {@link VertexInputLayout.AttributeSet} containing all per-vertex attributes for
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
    protected abstract VertexInputLayout.AttributeSet allVertexAttributes();

    /**
     * Sets per-vertex attributes mask, which is used to control which of them are used
     * by this GeometryProcessor instance. Note: Call this in subclasses constructor.
     *
     * @param mask a bit mask determining which attributes to use, can be zero
     */
    protected final void setVertexAttributes(int mask) {
        VertexInputLayout.AttributeSet attrs = allVertexAttributes();
        assert attrs != null;
        mVertexAttributesMask |= mask & attrs.mAllMask; // sanitize
    }

    /**
     * Returns a shared {@link VertexInputLayout.AttributeSet} containing all per-instance attributes for
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
    protected abstract VertexInputLayout.AttributeSet allInstanceAttributes();

    /**
     * Sets per-instance attributes mask, which is used to control which of them are used
     * by this GeometryProcessor instance. Note: Call this in subclasses constructor.
     *
     * @param mask a bit mask determining which attributes to use, can be zero
     */
    protected final void setInstanceAttributes(int mask) {
        VertexInputLayout.AttributeSet attrs = allInstanceAttributes();
        assert attrs != null;
        mInstanceAttributesMask |= mask & attrs.mAllMask; // sanitize
    }

    public void writeVertices(MeshDrawWriter writer, DrawOp drawOp, float[] solidColor) {
    }

    /**
     * Every {@link GeometryStep} must be capable of creating a subclass of ProgramImpl. The
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
                                   GeometryStep geomProc,
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
                                     GeometryStep geomProc);

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
                                           GeometryStep geomProc,
                                           String outputColor,
                                           String outputCoverage,
                                           @SamplerHandle int[] texSamplers,
                                           ShaderVar localPos,
                                           ShaderVar worldPos);
    }
}
