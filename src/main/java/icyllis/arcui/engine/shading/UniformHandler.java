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

package icyllis.arcui.engine.shading;

import icyllis.arcui.core.SLType;
import icyllis.arcui.engine.*;
import icyllis.arcui.engine.shading.ProgramDataManager.UniformHandle;

import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Abstract class that builds uniform blocks.
 * <p>
 * The uniform blocks are generally defined as:
 * <pre><code>
 * // Anonymous block
 * layout(std140, binding = 0) uniform CommonBlock {
 *     vec4 u_OrthoProj;        // required
 *     mat3 u_ModelView;        // required
 *     vec4 u_DstTextureCoords; // optional
 * }
 * layout(std140, binding = 1) uniform EffectBlock {
 *     // per-effect uniforms...
 * }
 * </code></pre>
 */
public abstract class UniformHandler {

    // Keep consistent with Skia
    public static final String NO_MANGLE_PREFIX = "sk_";

    public static class UniformInfo {

        public ShaderVar mVariable;
        public int mVisibility;
        public Processor mOwner;
        public String mRawName;

        public UniformInfo() {
        }
    }

    /**
     * Marks an integer as an opaque handle to a sampler resource.
     */
    @Retention(RetentionPolicy.SOURCE)
    public @interface SamplerHandle {
    }

    protected final ProgramBuilder mProgramBuilder;

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
     *
     * @param visibility see ShaderFlag
     * @param type       see {@link SLType}
     * @param name       the raw name (pre-mangling), cannot be null or empty
     * @return UniformHandle
     */
    @UniformHandle
    public final int addUniform(Processor owner,
                                int visibility,
                                byte type,
                                String name) {
        assert (owner != null && name != null && !name.isEmpty());
        assert (visibility != 0);
        assert (type >= 0 && type <= SLType.LAST);
        assert (!SLType.isCombinedSamplerType(type));
        boolean mangleName = !name.startsWith(NO_MANGLE_PREFIX);
        return internalAddUniformArray(owner, visibility, type, name, mangleName, ShaderVar.NON_ARRAY);
    }

    /**
     * @param visibility see ShaderFlag
     * @param type       see {@link SLType}
     * @param name       the raw name (pre-mangling), cannot be null or empty
     * @param arrayCount the number of elements, cannot be zero
     * @return UniformHandle
     */
    @UniformHandle
    public final int addUniformArray(Processor owner,
                                     int visibility,
                                     byte type,
                                     String name,
                                     int arrayCount) {
        assert (owner != null && name != null && !name.isEmpty());
        assert (visibility != 0);
        assert (type >= 0 && type <= SLType.LAST);
        assert (!SLType.isCombinedSamplerType(type));
        assert (arrayCount >= 1);
        boolean mangleName = !name.startsWith(NO_MANGLE_PREFIX);
        return internalAddUniformArray(owner, visibility, type, name, mangleName, arrayCount);
    }

    @UniformHandle
    protected abstract int internalAddUniformArray(Processor owner,
                                                   int visibility,
                                                   byte type,
                                                   String name,
                                                   boolean mangleName,
                                                   int arrayCount);

    /**
     * @param u UniformHandle
     */
    public abstract ShaderVar getUniformVariable(@UniformHandle int u);

    /**
     * Shortcut for getUniformVariable(u).getName()
     *
     * @param u UniformHandle
     */
    public final String getUniformName(@UniformHandle int u) {
        return getUniformVariable(u).getName();
    }

    public abstract int numUniforms();

    public abstract UniformInfo uniform(int idx);

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
                u.mVisibility |= EngineTypes.ShaderFlag_Vertex;
                return u.mVariable;
            }
        }
        // Uniform not found; it's better to return a void variable than to assert because sample
        // matrices that are uniform are treated the same for most of the code. When the sample
        // matrix expression can't be found as a uniform, we can infer it's a constant.
        return null;
    }

    /**
     * Returns the base alignment in bytes taken up in UBO for SLTypes.
     *
     * @param type     see {@link SLType}
     * @param nonArray true for a single element, false for an array of elements
     * @param std430   true for std430 layout, false for std140 layout
     * @return base alignment
     */
    public static int getAlignmentMask(byte type, boolean nonArray, boolean std430) {
        switch (type) {
            case SLType.BOOL:   // fall through
            case SLType.INT:    // fall through
            case SLType.UINT:   // fall through
            case SLType.FLOAT:  // fall through
                return std430 || nonArray ? Float.BYTES - 1 : 4 * Float.BYTES - 1; // N - 1
            case SLType.BVEC2:  // fall through
            case SLType.IVEC2:  // fall through
            case SLType.UVEC2:  // fall through
            case SLType.VEC2:   // fall through
                return std430 || nonArray ? 2 * Float.BYTES - 1 : 4 * Float.BYTES - 1; // 2N - 1
            case SLType.BVEC3:  // fall through
            case SLType.BVEC4:  // fall through
            case SLType.IVEC3:  // fall through
            case SLType.IVEC4:  // fall through
            case SLType.UVEC3:  // fall through
            case SLType.UVEC4:  // fall through
            case SLType.VEC3:   // fall through
            case SLType.VEC4:   // fall through
            case SLType.MAT3:   // fall through
            case SLType.MAT4:   // fall through
                return 4 * Float.BYTES - 1; // 4N - 1
            case SLType.MAT2:
                return std430 ? 2 * Float.BYTES - 1 : 4 * Float.BYTES - 1; // as an array of Vec2

            // This query is only valid for certain types.
            case SLType.VOID:
            case SLType.SAMPLER2D:
            case SLType.TEXTURE2D:
            case SLType.SAMPLER:
            case SLType.SUBPASSINPUT:
                throw new IllegalStateException(String.valueOf(type));
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Returns the size in bytes taken up in UBO for SLTypes.
     * This includes paddings between components, but does not include paddings at the end of the element.
     *
     * @param type   see {@link SLType}
     * @param std430 true for std430 layout, false for std140 layout
     * @return size in bytes
     */
    public static int getSize(byte type, boolean std430) {
        switch (type) {
            case SLType.FLOAT:
                return Float.BYTES;
            case SLType.VEC2:
                return 2 * Float.BYTES;
            case SLType.VEC3:
                return 3 * Float.BYTES;
            case SLType.VEC4:
                return 4 * Float.BYTES;
            case SLType.BOOL:   // fall through
            case SLType.INT:    // fall through
            case SLType.UINT:
                return Integer.BYTES;
            case SLType.BVEC2:  // fall through
            case SLType.IVEC2:  // fall through
            case SLType.UVEC2:
                return 2 * Integer.BYTES;
            case SLType.BVEC3:  // fall through
            case SLType.IVEC3:  // fall through
            case SLType.UVEC3:
                return 3 * Integer.BYTES;
            case SLType.BVEC4:  // fall through
            case SLType.IVEC4:  // fall through
            case SLType.UVEC4:
                return 4 * Integer.BYTES;
            case SLType.MAT2:
                return std430 ? 2 * 2 * Float.BYTES : 2 * 4 * Float.BYTES;
            case SLType.MAT3:
                return 3 * 4 * Float.BYTES;
            case SLType.MAT4:
                return 4 * 4 * Float.BYTES;

            // This query is only valid for certain types.
            case SLType.VOID:
            case SLType.SAMPLER2D:
            case SLType.TEXTURE2D:
            case SLType.SAMPLER:
            case SLType.SUBPASSINPUT:
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
     * @param std430     true for std430 layout, false for std140 layout
     * @return the aligned offset for the new uniform
     */
    public static int getAlignedOffset(int offset,
                                       byte type,
                                       int arrayCount,
                                       boolean std430) {
        assert (type >= 0 && type <= SLType.LAST);
        assert (arrayCount >= ShaderVar.NON_ARRAY);
        int alignmentMask = getAlignmentMask(type, arrayCount == ShaderVar.NON_ARRAY, std430);
        return (offset + alignmentMask) & ~alignmentMask;
    }

    public static int getAlignedStride(byte type,
                                       int arrayCount,
                                       boolean std430) {
        assert (type >= 0 && type <= SLType.LAST);
        assert (arrayCount >= ShaderVar.NON_ARRAY);
        if (arrayCount == ShaderVar.NON_ARRAY) {
            return getSize(type, std430);
        } else {
            final int elementSize;
            if (std430) {
                elementSize = getSize(type, true);
            } else { // std140, round up to Vec4
                // currently, values greater than 16 are already multiples of 16, so just use max
                elementSize = Math.max(getSize(type, false), 4 * Float.BYTES);
            }
            assert ((elementSize & (4 * Float.BYTES - 1)) == 0);
            return elementSize * arrayCount;
        }
    }
}
