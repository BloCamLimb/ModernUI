/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.opengl;

import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents OpenGL textures and renderbuffers.
 */
public abstract sealed class GLImage extends Image
        permits GLTexture, GLRenderbuffer {

    protected final GLImageDesc mDesc;

    protected GLImage(GLDevice device, GLImageDesc desc,
                      ImageMutableState mutableState) {
        super(device, desc, mutableState);
        mDesc = desc;
    }

    @Nonnull
    public GLImageDesc getGLDesc() {
        return mDesc;
    }

    @Nullable
    @Override
    protected ScratchKey computeScratchKey() {
        return new ScratchKey(mDesc);
    }

    public static final class ScratchKey implements IScratchKey {

        private final GLImageDesc mDesc;

        public ScratchKey(GLImageDesc desc) {
            mDesc = desc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ScratchKey that = (ScratchKey) o;

            return mDesc.equals(that.mDesc);
        }

        @Override
        public int hashCode() {
            return mDesc.hashCode();
        }
    }
}
