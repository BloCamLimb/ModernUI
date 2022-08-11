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

package icyllis.arcui.opengl;

import icyllis.arcui.engine.*;
import icyllis.arcui.engine.shading.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GLProgramBuilder extends ProgramBuilder {

    private final GLServer mServer;
    private final GLVaryingHandler mVaryingHandler;
    private final GLUniformHandler mUniformHandler;

    public GLProgramBuilder(GLServer server, ProgramDesc desc, ProgramInfo programInfo) {
        super(desc, programInfo);
        mServer = server;
        mVaryingHandler = new GLVaryingHandler(this);
        mUniformHandler = new GLUniformHandler(this);
    }

    @Nullable
    static GLProgram createProgram(DirectContext dContext,
                                   final ProgramDesc desc,
                                   final ProgramInfo programInfo) {
        return null;
    }

    @Override
    public Caps caps() {
        return mServer.getCaps();
    }

    @Override
    public UniformHandler uniformHandler() {
        return mUniformHandler;
    }

    @Override
    public VaryingHandler varyingHandler() {
        return mVaryingHandler;
    }

    @Nonnull
    private GLProgram createProgram(int programID) {
        return GLProgram.make(mServer,
                programID,
                mUniformHandler.mUniforms,
                mUniformHandler.mCurrentOffset,
                mUniformHandler.mSamplers,
                mGPImpl);
    }
}
