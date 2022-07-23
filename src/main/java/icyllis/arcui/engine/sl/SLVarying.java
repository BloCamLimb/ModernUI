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

package icyllis.arcui.engine.sl;

import icyllis.arcui.core.SLType;
import icyllis.arcui.engine.ShaderVar;

public class SLVarying {

    byte mType;
    // initialized by GLSLVaryingHandler
    String mVsOut;
    String mFsIn;

    /**
     * @param type see {@link icyllis.arcui.core.SLType}
     */
    public SLVarying(byte type) {
        // Metal doesn't support varying matrices, so we disallow them everywhere for consistency
        assert !SLType.isMatrixType(type);
        mType = type;
    }

    /**
     * @param type see {@link icyllis.arcui.core.SLType}
     */
    public void reset(byte type) {
        // Metal doesn't support varying matrices, so we disallow them everywhere for consistency
        assert !SLType.isMatrixType(type);
        mType = type;
        mVsOut = null;
        mFsIn = null;
    }

    public byte getType() {
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
        return new ShaderVar(vsOut(), mType, ShaderVar.TYPE_MODIFIER_OUT);
    }

    public ShaderVar fsInVar() {
        assert isInFragmentShader();
        return new ShaderVar(fsIn(), mType, ShaderVar.TYPE_MODIFIER_IN);
    }
}
