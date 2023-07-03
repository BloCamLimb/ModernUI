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

package icyllis.arc3d.opengl;

import icyllis.arc3d.engine.ManagedResource;

import javax.annotation.Nonnull;

import static icyllis.arc3d.opengl.GLCore.glDeleteProgram;

/**
 * Represents OpenGL programs.
 */
public final class GLProgram extends ManagedResource {

    private int mProgram;

    public GLProgram(@Nonnull GLEngine engine,
                     int program) {
        super(engine);
        assert (program != 0);
        mProgram = program;
    }

    @Override
    protected void deallocate() {
        if (mProgram != 0) {
            glDeleteProgram(mProgram);
        }
        discard();
    }

    public void discard() {
        mProgram = 0;
    }

    public int getProgram() {
        return mProgram;
    }
}
