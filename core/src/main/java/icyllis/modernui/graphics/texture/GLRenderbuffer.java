/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.texture;

import icyllis.modernui.graphics.GLObject;

import javax.annotation.Nonnull;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Represents OpenGL renderbuffer objects. Losing the reference to this object will
 * delete the renderbuffer automatically.
 */
public final class GLRenderbuffer extends GLObject {

    public GLRenderbuffer() {
    }

    @Override
    public int get() {
        if (ref == null) {
            ref = new RenderbufferRef(this);
        }
        return ref.object;
    }

    public void allocate(int internalFormat, int width, int height, int samples) {
        if (samples < 0) {
            throw new IllegalArgumentException();
        } else if (samples > 0) {
            glNamedRenderbufferStorageMultisample(get(), samples, internalFormat, width, height);
        } else {
            glNamedRenderbufferStorage(get(), internalFormat, width, height);
        }
    }

    public int getInternalFormat() {
        return glGetNamedRenderbufferParameteri(get(), GL_RENDERBUFFER_INTERNAL_FORMAT);
    }

    private static final class RenderbufferRef extends Ref {

        private RenderbufferRef(@Nonnull GLRenderbuffer owner) {
            super(owner, glCreateRenderbuffers());
        }

        @Override
        public void run() {
            deleteRenderbufferAsync(object, this);
        }
    }
}
