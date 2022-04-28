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

package icyllis.arcui.gl;

import icyllis.arcui.hgi.*;

import static org.lwjgl.opengl.GL45C.*;

public final class GLServer extends Server {

    private GLCaps mCaps;

    private int mDrawFramebuffer = 0;

    public GLServer(DirectContext context) {
        super(context);
    }

    public void bindFramebuffer(int target, int framebuffer) {
        glBindFramebuffer(target, framebuffer);
        if (target == GL_FRAMEBUFFER || target == GL_DRAW_FRAMEBUFFER) {
            mDrawFramebuffer = framebuffer;
        }
        onFramebufferChanged();
    }

    public void deleteFramebuffer(int framebuffer) {
        glDeleteFramebuffers(framebuffer);
        // Deleting the currently bound framebuffer rebinds to 0.
        if (mDrawFramebuffer == framebuffer) {
            onFramebufferChanged();
            mDrawFramebuffer = 0;
        }
    }

    private void onFramebufferChanged() {

    }

    @Override
    public GLCaps getCaps() {
        return mCaps;
    }
}
