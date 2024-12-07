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
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents OpenGL textures and renderbuffers.
 */
public abstract sealed class GLImage extends Image
        permits GLTexture, GLRenderbuffer {

    protected GLImage(Context context,
                      boolean budgeted,
                      boolean wrapped,
                      GLImageDesc desc,
                      ImageMutableState mutableState) {
        super(context, budgeted, wrapped, desc, mutableState);
    }

    @NonNull
    public final GLImageDesc getGLDesc() {
        return (GLImageDesc) getDesc();
    }

    @Override
    protected GLDevice getDevice() {
        return (GLDevice) super.getDevice();
    }

    public int getTarget() {
        return getGLDesc().mTarget;
    }

    public int getFormat() {
        return getGLDesc().mFormat;
    }

    public abstract int getHandle();

    public static final class ResourceKey implements IResourceKey {

        private final GLImageDesc mDesc;

        public ResourceKey(GLImageDesc desc) {
            mDesc = desc;
        }

        @Override
        public IResourceKey copy() {
            return new ResourceKey(mDesc);
        }

        @Override
        public int hashCode() {
            return mDesc.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResourceKey that = (ResourceKey) o;

            return mDesc.equals(that.mDesc);
        }
    }
}
