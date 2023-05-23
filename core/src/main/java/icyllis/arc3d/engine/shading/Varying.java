/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine.shading;

import icyllis.arc3d.engine.ShaderDataType;
import icyllis.arc3d.engine.ShaderVar;

/**
 * Shader stage inputs and outputs.
 */
public final class Varying {

    byte mType;
    String mVsOut = null;
    String mFsIn = null;

    /**
     * @param type see {@link ShaderDataType}
     */
    public Varying(byte type) {
        // Metal doesn't support varying matrices, so we disallow them everywhere for consistency
        assert (type != ShaderDataType.kVoid && !ShaderDataType.isMatrixType(type));
        mType = type;
    }

    /**
     * @param type see {@link ShaderDataType}
     */
    public void reset(byte type) {
        // Metal doesn't support varying matrices, so we disallow them everywhere for consistency
        assert (type != ShaderDataType.kVoid && !ShaderDataType.isMatrixType(type));
        mType = type;
        mVsOut = null;
        mFsIn = null;
    }

    /**
     * @return see {@link ShaderDataType}
     */
    public byte type() {
        return mType;
    }

    // XXX: we have no geometry shader
    public boolean isInVertexShader() {
        return true;
    }

    // XXX: we have no geometry shader
    public boolean isInFragmentShader() {
        return true;
    }

    public String vsOut() {
        assert isInVertexShader();
        return mVsOut;
    }

    public String fsIn() {
        assert isInFragmentShader();
        return mFsIn;
    }

    public ShaderVar vsOutVar() {
        assert isInVertexShader();
        return new ShaderVar(mVsOut, mType, ShaderVar.kOut_TypeModifier);
    }

    public ShaderVar fsInVar() {
        assert isInFragmentShader();
        return new ShaderVar(mFsIn, mType, ShaderVar.kIn_TypeModifier);
    }
}
