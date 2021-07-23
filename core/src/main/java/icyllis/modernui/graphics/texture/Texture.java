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

import icyllis.modernui.ModernUI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.Cleaner;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Represents OpenGL texture objects at low-level. Losing the
 * reference of this object will delete the texture automatically.
 */
public abstract class Texture implements AutoCloseable {

    @Nullable
    private Ref mRef;

    protected Texture() {
    }

    /**
     * Returns the OpenGL texture object name represented by this object.
     * It's always a valid value but may change if this was recycled.
     * This operation does not allocate GPU memory unless initialization.
     *
     * @return texture object name
     */
    public final int get() {
        if (mRef == null) {
            mRef = new Ref(this);
        }
        return mRef.texture;
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
     * An explicit way to delete this texture if present.
     */
    @Override
    public final void close() {
        if (mRef != null) {
            mRef.cleanup.clean();
            mRef = null;
        }
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
        mRef = new Ref(this);
        return r != null ? r.cleanup : null;
    }

    private static final class Ref implements Runnable {

        private final int texture;
        private final Cleaner.Cleanable cleanup;

        private Ref(@Nonnull Texture owner) {
            // the first binding to target defines the texture type,
            // but we use DSA, glCreateTextures does this
            texture = glCreateTextures(owner.getTarget());
            cleanup = ModernUI.registerCleanup(owner, this);
        }

        @Override
        public void run() {
            // if (id == INVALID_ID)
            // cleanup is synchronized, this method only called once by cleaner
            deleteTextureAsync(texture, this);
            // id = INVALID_ID;
        }
    }
}
