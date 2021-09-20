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
import javax.annotation.Nullable;
import java.lang.ref.Cleaner;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Represents OpenGL texture objects at low-level.
 */
public abstract class GLTexture extends GLObject {

    protected GLTexture() {
    }

    /**
     * Returns the OpenGL texture object name currently associated with this
     * object, or create and initialize it if not available. It may change in
     * the future if it is explicitly deleted.
     *
     * @return OpenGL texture object
     */
    @Override
    public final int get() {
        if (mRef == null) {
            mRef = new TextureRef(this);
        }
        return mRef.object;
    }

    public abstract int getTarget();

    /**
     * ERROR: if target is GL_TEXTURE_RECTANGLE and either of wrap mode GL_TEXTURE_WRAP_S or GL_TEXTURE_WRAP_T is set to
     * either GL_MIRROR_CLAMP_TO_EDGE, GL_MIRRORED_REPEAT or GL_REPEAT.
     */
    public void setWrapMode(int wrapS) {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapS);
    }

    public void setWrapMode(int wrapS, int wrapT, int wrapR) {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapS);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrapT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_R, wrapR);
    }

    /**
     * Re-create the OpenGL texture and returns the cleanup action for the previous one.
     * You should call the cleanup action if you will not touch the previous texture any more.
     * Otherwise it will be cleaned when this Texture object become phantom-reachable.
     *
     * @return cleanup action, null if this texture was recycled or never initialized
     */
    @Nullable
    public final Cleaner.Cleanable recreate() {
        final Ref r = mRef;
        mRef = new TextureRef(this);
        return r != null ? r.cleanup : null;
    }

    private static final class TextureRef extends Ref {

        private TextureRef(@Nonnull GLTexture owner) {
            super(owner, glCreateTextures(owner.getTarget()));
        }

        @Override
        public void run() {
            deleteTextureAsync(object, this);
        }
    }
}
