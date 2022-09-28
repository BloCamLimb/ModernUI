/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.engine.shading;

import icyllis.akashigi.core.SLType;
import icyllis.akashigi.engine.*;
import icyllis.akashigi.engine.shading.ProgramDataManager.UniformHandle;

import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * Abstract class that builds uniforms.
 * <p>
 * The uniform blocks are generally defined as:
 * <pre><code>
 * // Anonymous block
 * layout(std140, binding = 0) uniform RenderBlock {
 *     layout(offset = 0) vec4 u_OrthoProj;
 * }
 * layout(std140, binding = 1) uniform EffectBlock {
 *     // per-effect uniforms...
 * }</code></pre>
 * Per-effect uniforms are updated more frequently (generally, each draw op).
 * We should limit the UBO size to 128 bytes.
 */
public abstract class UniformHandler {

    /**
     * The Render Block are shared across pipelines.
     */
    public static final String RENDER_PREFIX = "u_";

    /**
     * Builtin uniforms that are in Render Block.
     */
    public static final String ORTHO_PROJ_NAME = "u_OrthoProj";

    public static class UniformInfo {

        public ShaderVar mVariable;
        public int mVisibility;
        public Processor mOwner;
        public String mRawName;

        /**
         * The offset using std140 layout, only valid for non-samplers.
         */
        public int mOffset;
    }

    /**
     * Marks an integer as an opaque handle to a sampler resource.
     */
    @Retention(RetentionPolicy.SOURCE)
    public @interface SamplerHandle {
    }

    /**
     * Layouts.
     */
    public static final boolean Std140Layout = false;
    public static final boolean Std430Layout = true;

    /**
     * Binding a descriptor set invalidates all higher index descriptor sets. We must bind
     * in the order of this enumeration. Samplers are after Uniforms because Ops can specify
     * GP textures as dynamic state, meaning they get rebound for each draw in a pipeline while
     * uniforms are bound once before all the draws. We bind input attachments after samplers
     * so those also need to be rebound if we bind new samplers.
     */
    public static final int MAIN_DESC_SET = 0;
    public static final int SAMPLER_DESC_SET = 1;
    public static final int INPUT_DESC_SET = 2;

    /**
     * The bindings for the main descriptor set.
     */
    public static final int RENDER_BINDING = 0;
    public static final int EFFECT_BINDING = 1; // will use Push Constants if possible

    public static final String RENDER_BLOCK_NAME = "RenderBlock";
    public static final String EFFECT_BLOCK_NAME = "EffectBlock";

    /**
     * The bindings for the input descriptor set.
     */
    public static final int INPUT_BINDING = 0;

    /**
     * The indices for input attachments.
     */
    public static final int DST_INPUT_ATTACHMENT_INDEX = 0;

    protected final ProgramBuilder mProgramBuilder;

    // there are no getters for these uniforms, handled by implementations
    protected final ArrayList<UniformInfo> mRenderUniforms = new ArrayList<>();
    protected int mCurrentRenderOffset;

    protected UniformHandler(ProgramBuilder programBuilder) {
        mProgramBuilder = programBuilder;
    }

    /**
     * Add a uniform variable to the current program, that has visibility in one or more shaders.
     * visibility is a bitfield of ShaderFlag values indicating from which shaders the uniform
     * should be accessible. At least one bit must be set. Geometry shader uniforms are not
     * supported at this time. The actual uniform name will be mangled. The final uniform name
     * can be retrieved by {@link #getUniformName(int)} with the UniformHandle. Use the
     * {@link #addUniformArray(Processor, int, byte, String, int)} variant to add an array of
     * uniforms.
     * <p>
     * If the name starts with {@link #RENDER_PREFIX}, the uniform will be assigned to Render
     * Block rather than Effect Block, which may be shared across stages and pipelines. Also,
     * the UniformHandle and its data manager may be only visible and internally handled by
     * implementations.
     *
     * @param owner      the raw ptr to owner, may be null
     * @param visibility combination of ShaderFlags can be zero as placeholder
     * @param type       see {@link SLType}
     * @param name       the raw name (pre-mangling), cannot be null or empty
     * @return UniformHandle either from Render Block or Effect Block
     */
    @UniformHandle
    public final int addUniform(Processor owner,
                                int visibility,
                                byte type,
                                String name) {
        assert (name != null && !name.isEmpty());
        assert ((visibility & ~(Engine.Vertex_ShaderFlag | Engine.Fragment_ShaderFlag)) == 0);
        assert (SLType.checkSLType(type));
        assert (!SLType.isCombinedSamplerType(type));
        return internalAddUniformArray(owner, visibility, type, name, ShaderVar.NonArray);
    }

    /**
     * Array version of {@link #addUniform(Processor, int, byte, String)}.
     *
     * @param owner      the raw ptr to owner, may be null
     * @param visibility combination of ShaderFlags, can be zero as placeholder
     * @param type       see {@link SLType}
     * @param name       the raw name (pre-mangling), cannot be null or empty
     * @param arrayCount the number of elements, cannot be zero
     * @return UniformHandle either from Render Block or Effect Block
     */
    @UniformHandle
    public final int addUniformArray(Processor owner,
                                     int visibility,
                                     byte type,
                                     String name,
                                     int arrayCount) {
        assert (name != null && !name.isEmpty());
        assert ((visibility & ~(Engine.Vertex_ShaderFlag | Engine.Fragment_ShaderFlag)) == 0);
        assert (SLType.checkSLType(type));
        assert (!SLType.isCombinedSamplerType(type));
        assert (arrayCount >= 1);
        return internalAddUniformArray(owner, visibility, type, name, arrayCount);
    }

    /**
     * @param handle UniformHandle from Effect Block
     */
    public abstract ShaderVar getUniformVariable(@UniformHandle int handle);

    /**
     * Shortcut for getUniformVariable(handle).getName()
     *
     * @param handle UniformHandle from Effect Block
     */
    public final String getUniformName(@UniformHandle int handle) {
        return getUniformVariable(handle).getName();
    }

    public abstract int numUniforms();

    public abstract UniformInfo uniform(int index);

    // Looks up a uniform that was added by 'owner' with the given 'rawName' (pre-mangling).
    // If there is no such uniform, null is returned.
    @Nullable
    public final ShaderVar getUniformMapping(Processor owner, String rawName) {
        for (int i = numUniforms() - 1; i >= 0; i--) {
            final UniformInfo u = uniform(i);
            if (u.mOwner == owner && u.mRawName.equals(rawName)) {
                return u.mVariable;
            }
        }
        return null;
    }

    // Like getUniformMapping(), but if the uniform is found it also marks it as accessible in
    // the vertex shader.
    @Nullable
    public final ShaderVar liftUniformToVertexShader(Processor owner, String rawName) {
        for (int i = numUniforms() - 1; i >= 0; i--) {
            final UniformInfo u = uniform(i);
            if (u.mOwner == owner && u.mRawName.equals(rawName)) {
                u.mVisibility |= Engine.Vertex_ShaderFlag;
                return u.mVariable;
            }
        }
        // Uniform not found; it's better to return a void variable than to assert because sample
        // matrices that are uniform are treated the same for most of the code. When the sample
        // matrix expression can't be found as a uniform, we can infer it's a constant.
        return null;
    }

    @UniformHandle
    protected int internalAddUniformArray(Processor owner,
                                          int visibility,
                                          byte type,
                                          String name,
                                          int arrayCount) {
        assert (SLType.canBeUniformValue(type));

        // handled by implementations
        assert (name.startsWith(RENDER_PREFIX));
        // reserved name
        assert (!name.contains("__"));

        // use std140 layout for render UBO
        int offset = getAlignedOffset(mCurrentRenderOffset, type, arrayCount, Std140Layout);
        mCurrentRenderOffset += getAlignedStride(type, arrayCount, Std140Layout);

        int handle = mRenderUniforms.size();

        String layoutQualifier = "offset = " + offset;

        var tempInfo = new UniformInfo();
        tempInfo.mVariable = new ShaderVar(name,
                type,
                ShaderVar.TypeModifier_None,
                arrayCount,
                layoutQualifier,
                "");
        tempInfo.mVisibility = visibility;
        tempInfo.mOwner = owner;
        tempInfo.mRawName = name;
        tempInfo.mOffset = offset;

        mRenderUniforms.add(tempInfo);
        return handle;
    }

    @SamplerHandle
    protected abstract int addSampler(BackendFormat backendFormat,
                                      int samplerState,
                                      short swizzle,
                                      String name);

    protected abstract String samplerVariable(@SamplerHandle int handle);

    protected abstract short samplerSwizzle(@SamplerHandle int handle);

    @SamplerHandle
    protected int addInputSampler(short swizzle, String name) {
        // vulkan only
        throw new UnsupportedOperationException();
    }

    protected String inputSamplerVariable(@SamplerHandle int handle) {
        // vulkan only
        throw new UnsupportedOperationException();
    }

    protected short inputSamplerSwizzle(@SamplerHandle int handle) {
        // vulkan only
        throw new UnsupportedOperationException();
    }

    /**
     * @param visibility one of ShaderFlags
     */
    protected abstract void appendUniformDecls(int visibility, StringBuilder out);

    /**
     * Returns the base alignment mask in bytes taken up in UBO for SLTypes.
     *
     * @param type     see {@link SLType}
     * @param nonArray true for a single scalar or vector, false for an array of scalars or vectors
     * @param layout   true for std430 layout, false for std140 layout
     * @return base alignment mask
     */
    public static int getAlignmentMask(byte type, boolean nonArray, boolean layout) {
        switch (type) {
            case SLType.Bool:   // fall through
            case SLType.Int:    // fall through
            case SLType.UInt:   // fall through
            case SLType.Float:  // fall through
                return layout == Std430Layout || nonArray ? 0x3 : 0xF; // N - 1
            case SLType.BVec2:  // fall through
            case SLType.IVec2:  // fall through
            case SLType.UVec2:  // fall through
            case SLType.Vec2:   // fall through
                return layout == Std430Layout || nonArray ? 0x7 : 0xF; // 2N - 1
            case SLType.BVec3:  // fall through
            case SLType.BVec4:  // fall through
            case SLType.IVec3:  // fall through
            case SLType.IVec4:  // fall through
            case SLType.UVec3:  // fall through
            case SLType.UVec4:  // fall through
            case SLType.Vec3:   // fall through
            case SLType.Vec4:   // fall through
            case SLType.Mat3:   // fall through
            case SLType.Mat4:   // fall through
                return 0xF; // 4N - 1
            case SLType.Mat2:
                return layout == Std430Layout ? 0x7 : 0xF; // as an array of Vec2

            // This query is only valid for certain types.
            case SLType.Void:
            case SLType.Sampler2D:
            case SLType.Texture2D:
            case SLType.Sampler:
            case SLType.SubpassInput:
                throw new IllegalStateException(String.valueOf(type));
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Returns the size in bytes taken up in UBO for SLTypes.
     * This includes paddings between components, but does not include paddings at the end of the element.
     *
     * @param type   see {@link SLType}
     * @param layout true for std430 layout, false for std140 layout
     * @return size in bytes
     * @see UniformDataManager
     */
    public static int getSize(byte type, boolean layout) {
        switch (type) {
            case SLType.Float:
                return Float.BYTES;
            case SLType.Vec2:
                return 2 * Float.BYTES;
            case SLType.Vec3:
                return 3 * Float.BYTES;
            case SLType.Vec4:
                return 4 * Float.BYTES;
            case SLType.Bool:   // fall through
            case SLType.Int:    // fall through
            case SLType.UInt:
                return Integer.BYTES;
            case SLType.BVec2:  // fall through
            case SLType.IVec2:  // fall through
            case SLType.UVec2:
                return 2 * Integer.BYTES;
            case SLType.BVec3:  // fall through
            case SLType.IVec3:  // fall through
            case SLType.UVec3:
                return 3 * Integer.BYTES;
            case SLType.BVec4:  // fall through
            case SLType.IVec4:  // fall through
            case SLType.UVec4:
                return 4 * Integer.BYTES;
            case SLType.Mat2:
                return layout == Std430Layout ? 2 * 2 * Float.BYTES : 2 * 4 * Float.BYTES;
            case SLType.Mat3:
                return 3 * 4 * Float.BYTES;
            case SLType.Mat4:
                return 4 * 4 * Float.BYTES;

            // This query is only valid for certain types.
            case SLType.Void:
            case SLType.Sampler2D:
            case SLType.Texture2D:
            case SLType.Sampler:
            case SLType.SubpassInput:
                throw new IllegalStateException(String.valueOf(type));
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Given the current offset into the UBO data, calculate the offset for the uniform we're trying to
     * add taking into consideration all alignment requirements. Use aligned offset plus
     * {@link #getAlignedStride(byte, int, boolean)} to get the offset to the end of the new uniform.
     *
     * @param offset     the current offset
     * @param type       see {@link SLType}
     * @param arrayCount see {@link ShaderVar}
     * @param layout     true for std430 layout, false for std140 layout
     * @return the aligned offset for the new uniform
     */
    public static int getAlignedOffset(int offset,
                                       byte type,
                                       int arrayCount,
                                       boolean layout) {
        assert (SLType.checkSLType(type));
        assert (arrayCount == ShaderVar.NonArray) || (arrayCount >= 1);
        int alignmentMask = getAlignmentMask(type, arrayCount == ShaderVar.NonArray, layout);
        return (offset + alignmentMask) & ~alignmentMask;
    }

    /**
     * @see UniformDataManager
     */
    public static int getAlignedStride(byte type,
                                       int arrayCount,
                                       boolean layout) {
        assert (SLType.checkSLType(type));
        assert (arrayCount == ShaderVar.NonArray) || (arrayCount >= 1);
        if (arrayCount == ShaderVar.NonArray) {
            return getSize(type, layout);
        } else {
            final int elementSize;
            if (layout == Std430Layout) {
                elementSize = getSize(type, Std430Layout);
            } else {
                // std140, round up to Vec4
                // currently, values greater than 16 are already multiples of 16, so just use max
                elementSize = Math.max(getSize(type, Std140Layout), 16);
            }
            assert ((elementSize & (0xF)) == 0);
            return elementSize * arrayCount;
        }
    }
}
