/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.core.shaders;

import icyllis.arc3d.core.BlendMode;
import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import org.jspecify.annotations.Nullable;

public final class BlendShader implements Shader {

    private final BlendMode mMode;
    @SharedPtr
    private final Shader mSrc;
    @SharedPtr
    private final Shader mDst;

    BlendShader(BlendMode mode, @SharedPtr Shader src, @SharedPtr Shader dst) {
        mMode = mode;
        mSrc = src;
        mDst = dst;
    }

    @Nullable
    @SharedPtr
    public static Shader make(BlendMode mode, @SharedPtr Shader src, @SharedPtr Shader dst) {
        if (src == null || dst == null || mode == null) {
            RefCnt.move(src);
            RefCnt.move(dst);
            return null;
        }
        switch (mode) {
            case CLEAR: {
                RefCnt.move(src);
                RefCnt.move(dst);
                return new ColorShader(0x00000000);
            }
            case SRC: {
                RefCnt.move(dst);
                return src;
            }
            case DST: {
                RefCnt.move(src);
                return dst;
            }
        }
        return new BlendShader(mode, src, dst); // move
    }

    // We can leak the ref countability to the underlying object in this scenario

    @Override
    public void ref() {
        mSrc.ref();
        mDst.ref();
    }

    @Override
    public void unref() {
        mSrc.unref();
        mDst.unref();
    }

    @Override
    public boolean isTriviallyCounted() {
        return mSrc.isTriviallyCounted() && mDst.isTriviallyCounted();
    }

    public BlendMode getMode() {
        return mMode;
    }

    @RawPtr
    public Shader getSrc() {
        return mSrc;
    }

    @RawPtr
    public Shader getDst() {
        return mDst;
    }
}
