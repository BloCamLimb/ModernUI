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

package icyllis.modernui.akashi.opengl;

import icyllis.modernui.core.Core;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL45C;

import javax.annotation.Nonnull;

/**
 * Represents OpenGL renderbuffer objects. Losing the reference to this object will
 * delete the renderbuffer automatically.
 */
public final class GLRenderbufferCompat extends GLObjectCompat {

    public GLRenderbufferCompat() {
    }

    @Override
    public int get() {
        if (ref == null) {
            ref = new Ref(this);
        }
        return ref.mId;
    }

    public void allocate(int internalFormat, int width, int height, int samples) {
        if (samples < 0) {
            throw new IllegalArgumentException();
        } else if (samples > 0) {
            GL45C.glNamedRenderbufferStorageMultisample(get(), samples, internalFormat, width, height);
        } else {
            GL45C.glNamedRenderbufferStorage(get(), internalFormat, width, height);
        }
    }

    public int getWidth() {
        return GL45C.glGetNamedRenderbufferParameteri(get(), GL30C.GL_RENDERBUFFER_WIDTH);
    }

    public int getHeight() {
        return GL45C.glGetNamedRenderbufferParameteri(get(), GL30C.GL_RENDERBUFFER_HEIGHT);
    }

    public int getInternalFormat() {
        return GL45C.glGetNamedRenderbufferParameteri(get(), GL30C.GL_RENDERBUFFER_INTERNAL_FORMAT);
    }

    private static final class Ref extends GLObjectCompat.Ref {

        private Ref(@Nonnull GLRenderbufferCompat owner) {
            super(owner, GL45C.glCreateRenderbuffers());
        }

        @Override
        public void run() {
            Core.executeOnRenderThread(() -> GL30C.glDeleteRenderbuffers(mId));
        }
    }
}
