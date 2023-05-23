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

package icyllis.arc3d.opengl;

import icyllis.modernui.core.Core;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

import static icyllis.arc3d.opengl.GLCore.*;

/**
 * Represents a OpenGL buffer object.
 */
public class GLBufferCompat extends GLObjectCompat {

    public GLBufferCompat() {
    }

    /**
     * Returns the OpenGL buffer object name currently associated with this
     * object, or create and initialize it if not available. It may change in
     * the future if it is explicitly deleted.
     *
     * @return OpenGL buffer object
     */
    @Override
    public final int get() {
        if (ref == null) {
            ref = new Ref(this);
        }
        return ref.mId;
    }

    /**
     * Binds this buffer to the indexed buffer target, as well as entirely to the binding
     * point in the array given by index. Each target has its own indexed array of buffer object
     * binding points.
     *
     * @param target the target of the bind operation
     * @param index  the index of the binding point within the array specified by {@code target}
     */
    public void bindBase(int target, int index) {
        glBindBufferBase(target, index, get());
    }

    /**
     * Binds this buffer to the indexed buffer target, as well as a range within it to the
     * binding point in the array given by index. Each target has its own indexed array of buffer
     * object binding points.
     *
     * @param target the target of the bind operation
     * @param index  the index of the binding point within the array specified by {@code target}
     * @param offset the start offset in bytes into the buffer
     * @param size   the amount of data in bytes that can be read from the buffer object while used as an indexed target
     */
    public void bindRange(int target, int index, long offset, long size) {
        glBindBufferRange(target, index, get(), offset, size);
    }

    /**
     * Creates the immutable data store of this buffer object.
     *
     * @param size  the size of the data store in bytes
     * @param data  the data to initialize the buffer, or {@code NULL} leaving it undefined
     * @param flags the bitwise {@code OR} of flags describing the intended usage of the buffer object's data
     *              store by the application
     */
    public void allocate(long size, long data, int flags) {
        nglNamedBufferStorage(get(), size, data, flags);
    }

    /**
     * Creates the mutable data store of this buffer object.
     *
     * @param size  the size of the data store in bytes
     * @param data  the data to initialize the buffer, or {@code NULL} leaving it undefined
     * @param usage the expected usage pattern of the data store
     */
    public void allocateM(long size, long data, int usage) {
        nglNamedBufferData(get(), size, data, usage);
    }

    /**
     * Modifies a subset of this buffer object's data store.
     *
     * @param offset the offset into the buffer object's data store where data replacement will begin, measured in bytes
     * @param size   the size in bytes of the data store region being replaced
     * @param data   a pointer to the new data that will be copied into the data store, can't be {@code NULL}
     */
    public void upload(long offset, long size, long data) {
        nglNamedBufferSubData(get(), offset, size, data);
    }

    /**
     * Modifies a subset of this buffer object's data store.
     *
     * @param offset the offset into the buffer object's data store where data replacement will begin, measured in bytes
     * @param data   a pointer to the new data that will be copied into the data store, can't be {@code NULL}
     */
    public void upload(long offset, @Nonnull ByteBuffer data) {
        glNamedBufferSubData(get(), offset, data);
    }

    private static final class Ref extends GLObjectCompat.Ref {

        private Ref(@Nonnull GLBufferCompat owner) {
            super(owner, glCreateBuffers());
        }

        @Override
        public void run() {
            Core.executeOnRenderThread(() -> glDeleteBuffers(mId));
        }
    }
}
