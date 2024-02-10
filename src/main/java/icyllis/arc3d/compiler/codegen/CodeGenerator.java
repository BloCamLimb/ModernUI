/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.compiler.codegen;

import icyllis.arc3d.compiler.Context;
import icyllis.arc3d.compiler.tree.TranslationUnit;
import org.lwjgl.BufferUtils;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

/**
 * Abstract superclass of all code generators, which take a {@link TranslationUnit} as input
 * and produce code as output.
 */
public abstract class CodeGenerator {

    public final Context mContext;
    public final TranslationUnit mTranslationUnit;

    protected ByteBuffer mBuffer;

    public CodeGenerator(Context context,
                         TranslationUnit translationUnit) {
        mContext = context;
        mTranslationUnit = translationUnit;
    }

    /**
     * Generates the code and returns the pointer value. The code size in bytes is
     * {@link ByteBuffer#remaining()}. Check errors via {@link Context#getErrorHandler()}.
     * <p>
     * The return value is a direct buffer, see {@link ByteBuffer#allocateDirect(int)}.
     * A direct buffer wraps an address that points to off-heap memory, i.e. a native
     * pointer. The byte order is {@link java.nio.ByteOrder#nativeOrder()} and it's
     * safe to pass the result to OpenGL and Vulkan API. There is no way to free this
     * buffer explicitly, as it is subject to GC.
     *
     * @return the generated code, regardless of errors
     */
    @Nonnull
    public abstract ByteBuffer generateCode();

    protected ByteBuffer grow(int minCapacity) {
        if (minCapacity > mBuffer.capacity()) {
            // double the buffer, overflow will throw exception
            int newCapacity = Math.max(minCapacity, mBuffer.capacity() << 1);
            mBuffer = BufferUtils.createByteBuffer(newCapacity)
                    .put(mBuffer.flip());
        }
        return mBuffer;
    }
}
