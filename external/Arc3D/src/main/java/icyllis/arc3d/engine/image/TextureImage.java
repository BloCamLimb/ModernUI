/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.engine.image;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.RecordingContext;
import icyllis.arc3d.engine.TextureProxy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TextureImage extends Image {

    RecordingContext mContext;
    @SharedPtr
    TextureProxy mProxy;
    short mSwizzle;
    int mOrigin;

    public TextureImage(@Nonnull RecordingContext rContext,
                        @Nonnull TextureProxy proxy,
                        short swizzle,
                        int origin,
                        int colorType,
                        int alphaType,
                        @Nullable ColorSpace colorSpace) {
        super(ImageInfo.make(proxy.getBackingWidth(), proxy.getBackingHeight(),
                colorType, alphaType, colorSpace));
        mContext = rContext;
        mProxy = RefCnt.create(proxy);
        mSwizzle = swizzle;
        mOrigin = origin;
    }

    @Override
    protected void deallocate() {
        mProxy = RefCnt.move(mProxy);
    }

    @Override
    public RecordingContext getContext() {
        return mContext;
    }

    @Override
    public boolean isValid(@Nullable RecordingContext context) {
        if (mContext.isDiscarded()) {
            return false;
        }
        if (context != null) {
            return !context.isDiscarded() && mContext.matches(context);
        }
        return true;
    }

    @Override
    public boolean isTextureBacked() {
        return true;
    }

    @Override
    public long getTextureMemorySize() {
        return mProxy.getMemorySize();
    }
}
