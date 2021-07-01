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
import java.lang.ref.Cleaner;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * Represents an OpenGL renderbuffer object. Losing the reference
 * of the object will delete the renderbuffer automatically.
 */
public final class Renderbuffer implements AutoCloseable {

    private Ref mRef;

    public Renderbuffer() {
    }

    public int get() {
        if (mRef == null)
            mRef = new Ref(this);
        return mRef.renderbuffer;
    }

    public void init(int internalFormat, int width, int height, int samples) {
        if (samples < 0) {
            throw new IllegalArgumentException();
        } else if (samples > 0) {
            glNamedRenderbufferStorageMultisample(get(), samples, internalFormat, width, height);
        } else {
            glNamedRenderbufferStorage(get(), internalFormat, width, height);
        }
    }

    @Override
    public void close() {
        if (mRef != null) {
            mRef.cleanup.clean();
            mRef = null;
        }
    }

    private static final class Ref implements Runnable {

        private final int renderbuffer;
        private final Cleaner.Cleanable cleanup;

        private Ref(@Nonnull Renderbuffer owner) {
            renderbuffer = glCreateRenderbuffers();
            cleanup = ModernUI.registerCleanup(owner, this);
        }

        @Override
        public void run() {
            deleteRenderbufferAsync(renderbuffer, this);
        }
    }
}
