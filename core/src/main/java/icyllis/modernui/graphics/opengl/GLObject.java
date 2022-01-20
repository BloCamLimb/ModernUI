/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.opengl;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.RenderThread;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.Cleaner;

/**
 * Represents OpenGL objects at low-level. Losing the reference to this object will delete
 * the associated OpenGL object automatically. An explicit deletion can recycle this object.
 * All methods need to be called from the render thread with OpenGL context, except for
 * the constructor and special notes.
 */
@RenderThread
public abstract class GLObject implements AutoCloseable {

    @Nullable
    protected Ref ref;

    protected GLObject() {
    }

    /**
     * Returns the OpenGL object name currently associated with this object,
     * or create a {@link Ref} if not available. It may change in the future
     * if it is explicitly deleted. When this method is called, the OpenGL
     * object must be initialized.
     *
     * @return OpenGL object
     */
    public abstract int get();

    public final boolean isCreated() {
        return ref != null;
    }

    /**
     * Deletes the associated OpenGL object explicitly.
     */
    @Override
    public void close() {
        if (ref != null) {
            ref.cleanup.clean();
            ref = null;
        }
    }

    /**
     * The reference to internal OpenGL object. Besides, it is used as the execution target
     * of the cleaner when the owner object becomes phantom-reachable, and exposes the reference
     * to the cleanup action for explicit deletion.
     */
    protected static abstract class Ref implements Runnable {

        public final int object;
        public final Cleaner.Cleanable cleanup;

        /**
         * Registers the cleaner and also creates the OpenGL object for the given owner.
         *
         * @param t   owner object
         * @param obj created OpenGL object
         */
        protected Ref(@Nonnull GLObject t, int obj) {
            cleanup = ModernUI.registerCleanup(t, this);
            object = obj;
        }

        /**
         * Performs the deletion operation on internal OpenGL object once. This method can be
         * called from any thread, implementations need to post this Runnable to the render thread
         * with OpenGL context. Do not directly call this method.
         */
        @Override
        public abstract void run();
    }
}
