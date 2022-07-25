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

import javax.annotation.Nullable;

public abstract class UniformHandler {

    static final String NO_MANGLE_PREFIX = "sk_";

    public static class UniformInfo {

        public ShaderVar mVariable;
        public int mVisibility;
        public Processor mOwner;
        public String mRawName;
    }

    protected final ProgramBuilder mProgramBuilder;

    public UniformHandler(ProgramBuilder programBuilder) {
        mProgramBuilder = programBuilder;
    }

    /**
     * Add a uniform variable to the current program, that has visibility in one or more shaders.
     * visibility is a bitfield of ShaderFlag values indicating from which shaders the uniform
     * should be accessible. At least one bit must be set. Geometry shader uniforms are not
     * supported at this time. The actual uniform name will be mangled. If outName is not nullptr
     * then it will refer to the final uniform name after return. Use the addUniformArray variant
     * to add an array of uniforms.
     * <p>
     * The resolved name can be retrieved by {@link #getUniformName(int)} with the handle.
     *
     * @param visibility see ShaderFlag
     * @param type       see {@link SLType}
     * @param name       the raw name (pre-mangling), cannot be null or empty
     * @return UniformHandle
     */
    public final int addUniform(Processor owner,
                                int visibility,
                                byte type,
                                String name) {
        assert !SLType.isCombinedSamplerType(type);
        return addUniformArray(owner, visibility, type, name, ShaderVar.NON_ARRAY);
    }

    /**
     * @param visibility see ShaderFlag
     * @param type       see {@link SLType}
     * @param name       the raw name (pre-mangling), cannot be null or empty
     * @param arrayCount see {@link ShaderVar}
     * @return UniformHandle
     */
    public final int addUniformArray(Processor owner,
                                     int visibility,
                                     byte type,
                                     String name,
                                     int arrayCount) {
        assert !SLType.isCombinedSamplerType(type);
        boolean mangle = !name.startsWith(NO_MANGLE_PREFIX);
        return internalAddUniformArray(owner, visibility, type, name, mangle, arrayCount);
    }

    protected abstract int internalAddUniformArray(Processor owner,
                                                   int visibility,
                                                   byte type,
                                                   String name,
                                                   boolean mangleName,
                                                   int arrayCount);

    /**
     * @param u UniformHandle
     */
    public abstract ShaderVar getUniformVariable(int u);

    /**
     * Shortcut for getUniformVariable(u).getName()
     *
     * @param u UniformHandle
     */
    public final String getUniformName(int u) {
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
                u.mVisibility |= EngineTypes.SHADER_FLAG_VERTEX;
                return u.mVariable;
            }
        }
        // Uniform not found; it's better to return a void variable than to assert because sample
        // matrices that are uniform are treated the same for most of the code. When the sample
        // matrix expression can't be found as a uniform, we can infer it's a constant.
        return null;
    }
}
