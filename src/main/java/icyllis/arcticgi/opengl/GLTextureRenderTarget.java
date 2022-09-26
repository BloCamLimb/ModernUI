/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.opengl;

import icyllis.arcticgi.core.SharedPtr;

public class GLTextureRenderTarget extends GLRenderTarget {

    // the main color buffer, raw ptr
    // if this texture is deleted, then this render target is deleted as well
    // always null for wrapped render targets
    private GLTexture mColorBuffer;
    // the renderbuffer used as MSAA color buffer
    // always null for wrapped render targets
    @SharedPtr
    private GLAttachment mMSAAColorBuffer;

    public GLTextureRenderTarget(GLServer server,
                                 int width, int height,
                                 int format,
                                 int sampleCount,
                                 int framebuffer,
                                 int resolveFramebuffer,
                                 GLTexture colorBuffer,
                                 GLAttachment msaaColorBuffer) {
        super(server, width, height, format, sampleCount, framebuffer, resolveFramebuffer);
    }

    @Override
    public GLTexture getColorBuffer() {
        return mColorBuffer;
    }
}
